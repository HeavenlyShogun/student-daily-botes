package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val db = AppDatabase.getDatabase(context)

        when (intent.action) {
            "com.example.ACTION_TRIGGER_REMINDER" -> {
                val stageName = intent.getStringExtra(NotificationHelper.EXTRA_STAGE) ?: ReminderStage.GENTLE.name
                val stage = try {
                    ReminderStage.valueOf(stageName)
                } catch (_: Exception) {
                    ReminderStage.GENTLE
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val task = db.todoDao().getTaskById(taskId)
                    if (task != null && !task.isCompleted) {
                        NotificationHelper.showTaskReminder(context, task, stage)
                    }
                }
            }

            NotificationHelper.ACTION_COMPLETE -> {
                NotificationManagerCompat.from(context).cancel(taskId.toInt())
                CoroutineScope(Dispatchers.IO).launch {
                    val task = db.todoDao().getTaskById(taskId)
                    if (task != null) {
                        val updatedSubTasks = task.subTasks.map { it.copy(isCompleted = true) }
                        db.todoDao().updateTask(
                            task.copy(
                                isCompleted = true,
                                subTasks = updatedSubTasks,
                                completedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            NotificationHelper.ACTION_POSTPONE -> {
                NotificationManagerCompat.from(context).cancel(taskId.toInt())
                CoroutineScope(Dispatchers.IO).launch {
                    val task = db.todoDao().getTaskById(taskId)
                    if (task != null) {
                        val updated = task.copy(
                            postponeCount = task.postponeCount + 1,
                            reminderStage = ReminderStage.OVERDUE
                        )
                        db.todoDao().updateTask(updated)
                        // Re-trigger in 15 minutes
                        val postponeTriggerTime = System.currentTimeMillis() + 15 * 60 * 1000L
                        AlarmScheduler.scheduleTaskReminder(context, updated, ReminderStage.FOCUS, postponeTriggerTime)
                    }
                }
            }
        }
    }
}
