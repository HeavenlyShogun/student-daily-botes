package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.ReminderStage
import com.example.data.model.TodoTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {

    fun scheduleTaskReminder(context: Context, task: TodoTask, stage: ReminderStage, triggerEpochMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_REMINDER"
            putExtra(NotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NotificationHelper.EXTRA_STAGE, stage.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 100 + stage.ordinal).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
        }
    }

    fun cancelTaskReminders(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ReminderStage.values().forEach { stage ->
            val intent = Intent(context, ReminderBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (taskId * 100 + stage.ordinal).toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
