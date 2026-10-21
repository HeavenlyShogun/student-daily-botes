package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val isCompleted: Boolean = false,
    val dueDate: String = "",
    val dueTime: String = "",
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Priority(val label: String, val badgeColorHex: Long) {
    HIGH_URGENT("重要且緊急", 0xFFEF4444),
    HIGH_NOT_URGENT("重要不緊急", 0xFF3B82F6),
    NORMAL("一般待辦", 0xFF10B981),
    LOW("瑣事備忘", 0xFF6B7280)
}

enum class ReminderStage(val label: String, val tip: String) {
    GENTLE("溫和提醒", "先準備好課本講義與水杯，平靜心情"),
    FOCUS("專注執行", "啟動5分鐘微步法則，排除手機干擾"),
    OVERDUE("進度微調", "遇到瓶頸別焦慮，深呼吸並重新規劃小步驟")
}
