package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ReminderStage
import com.example.data.model.TodoTask

object NotificationHelper {

    const val CHANNEL_ID = "student_reminders_channel"
    const val CHANNEL_NAME = "學伴秘書待辦與生活提醒"
    const val CHANNEL_DESC = "提供階梯式溫和預告、專注執行提醒與健康作息推播"

    const val ACTION_COMPLETE = "com.example.ACTION_COMPLETE_TASK"
    const val ACTION_POSTPONE = "com.example.ACTION_POSTPONE_TASK"
    const val ACTION_START_FOCUS = "com.example.ACTION_START_FOCUS"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_STAGE = "extra_stage"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskReminder(context: Context, task: TodoTask, stage: ReminderStage) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Complete
        val completeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Postpone 15 mins
        val postponeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_POSTPONE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val postponePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 2).toInt(),
            postponeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = when (stage) {
            ReminderStage.GENTLE -> "🔔 【溫和提醒】即將開始：${task.subjectTag}"
            ReminderStage.FOCUS -> "🔥 【專注執行】5分鐘啟動：${task.title}"
            ReminderStage.OVERDUE -> "💡 【進度微調】遇到卡關嗎？別焦慮"
        }

        val messageText = when (stage) {
            ReminderStage.GENTLE -> "建議先備妥課本講義與溫水，深呼吸放鬆，15 分鐘後進入自習。"
            ReminderStage.FOCUS -> "大腦抗拒的是開始那一刻。先做 5 分鐘微步，慣性就會帶你完成！"
            ReminderStage.OVERDUE -> "當前進度稍微落後，建議點擊「延後 15 分」或重新拆解為更小步驟。"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titleText)
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$messageText\n任務內容：${task.title}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "✓ 完成", completePendingIntent)
            .addAction(0, "⏱ 延後15分", postponePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(task.id.toInt(), builder.build())
        } catch (_: SecurityException) {
            // Notification permission might not be granted
        }
    }
}
