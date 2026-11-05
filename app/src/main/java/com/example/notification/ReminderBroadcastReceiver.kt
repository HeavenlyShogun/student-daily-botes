package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.db.AppDatabase
import com.example.data.engine.SecretaryDecisionEngine
import com.example.data.model.ReminderStage
import com.example.data.repository.StudentLifeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processIntent(context, intent)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        suspend fun processIntent(
            context: Context,
            intent: Intent,
            nowMillis: Long = System.currentTimeMillis()
        ) {
            val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
            if (taskId == -1L) return

            val db = AppDatabase.getDatabase(context)

            when (intent.action) {
                AlarmScheduler.ACTION_TRIGGER_REMINDER -> {
                    val stageName = intent.getStringExtra(NotificationHelper.EXTRA_STAGE) ?: ReminderStage.GENTLE.name
                    val stage = try {
                        ReminderStage.valueOf(stageName)
                    } catch (_: Exception) {
                        ReminderStage.GENTLE
                    }

                    val task = db.todoDao().getTaskById(taskId)
                    if (task != null && !task.isCompleted) {
                        NotificationHelper.showTaskReminder(context, task, stage)
                    }
                }

                NotificationHelper.ACTION_COMPLETE -> {
                    // 1. Cancel all scheduled stage alarms for this task immediately
                    AlarmScheduler.cancelTaskReminders(context, taskId)
                    NotificationManagerCompat.from(context).cancel(taskId.toInt())

                    val task = db.todoDao().getTaskById(taskId)
                    if (task != null) {
                        val updatedSubTasks = task.subTasks.map { it.copy(isCompleted = true) }
                        db.todoDao().updateTask(
                            task.copy(
                                isCompleted = true,
                                subTasks = updatedSubTasks,
                                completedAt = nowMillis
                            )
                        )
                        // Reconcile remaining tasks' schedule & alarms
                        StudentLifeRepository(db).reconcileDailyPlanAndAlarms(context, nowMillis)
                    }
                }

                NotificationHelper.ACTION_POSTPONE -> {
                    // 1. Cancel all old stage alarms (GENTLE, FOCUS, OVERDUE) so stale stages never fire
                    AlarmScheduler.cancelTaskReminders(context, taskId)
                    NotificationManagerCompat.from(context).cancel(taskId.toInt())

                    val task = db.todoDao().getTaskById(taskId)
                    if (task != null && !task.isCompleted) {
                        val shiftedStart = if (task.scheduledSlotStart.isNotBlank()) {
                            SecretaryDecisionEngine.minutesToTime(
                                SecretaryDecisionEngine.timeToMinutes(task.scheduledSlotStart) + 15
                            )
                        } else {
                            ""
                        }
                        val shiftedEnd = if (task.scheduledSlotEnd.isNotBlank()) {
                            SecretaryDecisionEngine.minutesToTime(
                                SecretaryDecisionEngine.timeToMinutes(task.scheduledSlotEnd) + 15
                            )
                        } else {
                            ""
                        }

                        val updated = task.copy(
                            postponeCount = task.postponeCount + 1,
                            reminderStage = ReminderStage.OVERDUE,
                            scheduledSlotStart = shiftedStart,
                            scheduledSlotEnd = shiftedEnd
                        )
                        db.todoDao().updateTask(updated)

                        // 2. Schedule new snoozed FOCUS reminder in +15 minutes
                        val postponeTriggerTime = nowMillis + 15 * 60_000L
                        AlarmScheduler.scheduleTaskReminder(
                            context,
                            updated,
                            ReminderStage.FOCUS,
                            postponeTriggerTime
                        )

                        // 3. Preserve OVERDUE deadline reminder if deadline is still after the snoozed FOCUS alarm
                        val plan = AlarmScheduler.computeTaskReminderPlan(updated, nowMillis)
                        val overdueTrigger = plan.dueAtMillis
                        if (overdueTrigger != null && overdueTrigger > postponeTriggerTime) {
                            AlarmScheduler.scheduleTaskReminder(
                                context,
                                updated,
                                ReminderStage.OVERDUE,
                                overdueTrigger
                            )
                        }
                    }
                }
            }
        }
    }
}

