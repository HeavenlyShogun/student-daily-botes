package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

data class SubTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "todo_tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val subjectTag: String = "課業", // 國文, 英文, 數學, 自然, 社會, 生活, 複習
    val priority: Priority = Priority.HIGH_URGENT,
    val reminderStage: ReminderStage = ReminderStage.GENTLE,
    val targetMinutes: Int = 25,
    val completedMinutes: Int = 0,
    val microSteps: List<String> = emptyList(), // 5-minute break-down steps
    val currentMicroStepIndex: Int = 0,
    val subTasks: List<SubTask> = emptyList(), // 獨立子任務列表
    val isCompleted: Boolean = false,
    val dueDate: String = "",
    val dueTime: String = "",
    val postponeCount: Int = 0, // 延期次數追蹤 (反覆延期分析)
    val scheduledSlotStart: String = "", // 秘書建議時段開始 (例如 "19:00")
    val scheduledSlotEnd: String = "",   // 秘書建議時段結束 (例如 "19:45")
    val reminderMinutesBefore: Int = 15, // 溫和預先提醒時間 (分鐘)
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    // 父任務進度百分比 (0 ~ 100%)
    val progressPercent: Int
        get() {
            if (subTasks.isEmpty()) {
                return if (isCompleted) 100 else 0
            }
            val completedCount = subTasks.count { it.isCompleted }
            return ((completedCount.toFloat() / subTasks.size) * 100).toInt()
        }

    val completedSubTasksCount: Int
        get() = subTasks.count { it.isCompleted }

    val totalSubTasksCount: Int
        get() = subTasks.size

    val isRecurringDelayed: Boolean
        get() = postponeCount >= 2
}

enum class Priority(val label: String, val badgeColorHex: Long, val weight: Int) {
    HIGH_URGENT("重要且緊急", 0xFFEF4444, 4),
    HIGH_NOT_URGENT("重要不緊急", 0xFF3B82F6, 3),
    NORMAL("一般待辦", 0xFF10B981, 2),
    LOW("瑣事備忘", 0xFF6B7280, 1)
}

enum class ReminderStage(val label: String, val tip: String) {
    GENTLE("溫和提醒", "先準備好課本講義與水杯，平靜心情"),
    FOCUS("專注執行", "啟動5分鐘微步法則，排除手機干擾"),
    OVERDUE("進度微調", "遇到瓶頸別焦慮，深呼吸並重新規劃小步驟")
}
