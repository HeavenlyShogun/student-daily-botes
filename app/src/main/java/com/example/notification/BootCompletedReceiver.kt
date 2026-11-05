package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.repository.StudentLifeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )

        if (intent.action !in validActions) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reconcileOnSystemEvent(context)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        /**
         * Single source of truth on device boot, time change, or timezone change:
         * Re-runs the SecretaryDecisionEngine over fixed schedules, health logs, and active tasks,
         * persists updated `scheduledSlotStart`/`scheduledSlotEnd`, and syncs AlarmManager PendingIntents.
         */
        suspend fun reconcileOnSystemEvent(
            context: Context,
            nowMillis: Long = System.currentTimeMillis()
        ) {
            val db = AppDatabase.getDatabase(context)
            val repository = StudentLifeRepository(db)
            repository.reconcileDailyPlanAndAlarms(context, nowMillis)
        }
    }
}

