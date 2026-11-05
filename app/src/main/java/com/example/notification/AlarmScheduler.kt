package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.example.data.model.ReminderStage
import com.example.data.model.TodoTask
import java.text.SimpleDateFormat
import java.util.Locale

data class TaskReminderTimePlan(
    val dueAtMillis: Long?,
    val scheduledStartAtMillis: Long?,
    val scheduledEndAtMillis: Long?,
    val stageTriggers: Map<ReminderStage, Long>
)

object AlarmScheduler {

    const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"

    fun computeRequestCode(taskId: Long, stage: ReminderStage): Int {
        return (taskId * 100 + stage.ordinal).toInt()
    }

    /**
     * Unified Intent factory used for both scheduling and cancelling PendingIntents.
     * Ensures Intent.filterEquals() (component + action) is 100% identical.
     */
    fun buildReminderIntent(context: Context, taskId: Long, stage: ReminderStage): Intent {
        return Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
            putExtra(NotificationHelper.EXTRA_STAGE, stage.name)
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun scheduleTaskReminder(context: Context, task: TodoTask, stage: ReminderStage, triggerEpochMillis: Long) {
        if (task.id <= 0L || task.isCompleted) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = buildReminderIntent(context, task.id, stage)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            computeRequestCode(task.id, stage),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
        }
    }

    fun cancelTaskReminders(context: Context, taskId: Long) {
        if (taskId <= 0L) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ReminderStage.values().forEach { stage ->
            val intent = buildReminderIntent(context, taskId, stage)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                computeRequestCode(taskId, stage),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun hasScheduledReminder(context: Context, taskId: Long, stage: ReminderStage): Boolean {
        if (taskId <= 0L) return false
        val intent = buildReminderIntent(context, taskId, stage)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            computeRequestCode(taskId, stage),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }

    private fun parseDateTimeMillis(dateStr: String, timeStr: String): Long? {
        if (dateStr.isBlank() || timeStr.isBlank()) return null
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return try {
            sdf.parse("${dateStr.trim()} ${timeStr.trim()}")?.time
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Single source of truth for calculating task reminder times:
     * Distinguishes:
     * - dueAtMillis: The task's deadline (dueDate + dueTime)
     * - scheduledStartAtMillis: The SecretaryDecisionEngine's planned start time (dueDate + scheduledSlotStart)
     * - scheduledEndAtMillis: The SecretaryDecisionEngine's planned end time (dueDate + scheduledSlotEnd)
     * - stageTriggers:
     *   - GENTLE: `reminderMinutesBefore` (default 15m) before `scheduledStartAtMillis`, or 30m before `dueAtMillis` if unscheduled
     *   - FOCUS: at `scheduledStartAtMillis` (5-min micro-step launch), or 5m before `dueAtMillis` if unscheduled
     *   - OVERDUE: at `dueAtMillis` (or `scheduledEndAtMillis` if later/present)
     */
    fun computeTaskReminderPlan(
        task: TodoTask,
        nowMillis: Long = System.currentTimeMillis()
    ): TaskReminderTimePlan {
        if (task.isCompleted || task.dueDate.isBlank()) {
            return TaskReminderTimePlan(null, null, null, emptyMap())
        }

        val dueAtMillis = parseDateTimeMillis(task.dueDate, task.dueTime)
        val scheduledStartAtMillis = parseDateTimeMillis(task.dueDate, task.scheduledSlotStart)
        val scheduledEndAtMillis = parseDateTimeMillis(task.dueDate, task.scheduledSlotEnd)

        val leadMinutes = task.reminderMinutesBefore.coerceIn(5, 120)
        val triggers = LinkedHashMap<ReminderStage, Long>()

        if (scheduledStartAtMillis != null) {
            val gentleAt = scheduledStartAtMillis - leadMinutes * 60_000L
            val focusAt = scheduledStartAtMillis
            val overdueCandidate = when {
                dueAtMillis != null && dueAtMillis > focusAt -> dueAtMillis
                scheduledEndAtMillis != null && scheduledEndAtMillis > focusAt -> scheduledEndAtMillis
                else -> focusAt + task.targetMinutes.coerceIn(5, 120) * 60_000L
            }

            if (gentleAt > nowMillis) triggers[ReminderStage.GENTLE] = gentleAt
            if (focusAt > nowMillis) triggers[ReminderStage.FOCUS] = focusAt
            if (overdueCandidate > nowMillis) triggers[ReminderStage.OVERDUE] = overdueCandidate
        } else if (dueAtMillis != null) {
            val gentleAt = dueAtMillis - 30 * 60_000L
            val focusAt = dueAtMillis - 5 * 60_000L
            val overdueAt = dueAtMillis

            if (gentleAt > nowMillis) triggers[ReminderStage.GENTLE] = gentleAt
            if (focusAt > nowMillis) triggers[ReminderStage.FOCUS] = focusAt
            if (overdueAt > nowMillis) triggers[ReminderStage.OVERDUE] = overdueAt
        }

        return TaskReminderTimePlan(
            dueAtMillis = dueAtMillis,
            scheduledStartAtMillis = scheduledStartAtMillis,
            scheduledEndAtMillis = scheduledEndAtMillis,
            stageTriggers = triggers
        )
    }

    /**
     * Atomically cancels any prior alarms for this task and schedules the up-to-date reminder set.
     */
    fun syncTaskReminders(
        context: Context,
        task: TodoTask,
        nowMillis: Long = System.currentTimeMillis()
    ): TaskReminderTimePlan {
        cancelTaskReminders(context, task.id)
        if (task.isCompleted || task.id <= 0L) {
            return TaskReminderTimePlan(null, null, null, emptyMap())
        }
        val plan = computeTaskReminderPlan(task, nowMillis)
        for ((stage, triggerAt) in plan.stageTriggers) {
            scheduleTaskReminder(context, task, stage, triggerAt)
        }
        return plan
    }
}

