package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )

        if (intent.action !in validActions) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeTasks = db.todoDao().getActiveTasks().first()
                val now = System.currentTimeMillis()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                for (task in activeTasks) {
                    if (task.isCompleted) continue
                    if (task.dueDate.isNotBlank() && task.dueTime.isNotBlank()) {
                        val dueString = "${task.dueDate} ${task.dueTime}"
                        val dueDateObj = try {
                            sdf.parse(dueString)
                        } catch (_: Exception) {
                            null
                        } ?: continue

                        val dueEpoch = dueDateObj.time
                        // Stepped reminders:
                        // 1. Gentle reminder: 30 minutes before
                        val gentleEpoch = dueEpoch - 30 * 60 * 1000L
                        if (gentleEpoch > now) {
                            AlarmScheduler.scheduleTaskReminder(context, task, ReminderStage.GENTLE, gentleEpoch)
                        }
                        // 2. Focus reminder: 5 minutes before
                        val focusEpoch = dueEpoch - 5 * 60 * 1000L
                        if (focusEpoch > now) {
                            AlarmScheduler.scheduleTaskReminder(context, task, ReminderStage.FOCUS, focusEpoch)
                        }
                        // 3. Overdue reminder: at due time
                        if (dueEpoch > now) {
                            AlarmScheduler.scheduleTaskReminder(context, task, ReminderStage.OVERDUE, dueEpoch)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
