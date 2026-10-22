package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fixed_schedules")
data class FixedScheduleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dayOfWeek: Int = 0, // 0: 平日 (週一至週五), 1: 週一, ..., 7: 週日
    val startTime: String,  // "07:30"
    val endTime: String,    // "17:00"
    val category: String = "SCHOOL", // SCHOOL, COMMUTE, CRAM_SCHOOL, ACTIVITY, SLEEP
    val notes: String = ""
)

enum class ScheduleCategory(val label: String, val colorHex: Long) {
    SCHOOL("學校課堂", 0xFF2563EB),
    COMMUTE("交通通勤", 0xFF64748B),
    CRAM_SCHOOL("課後補習/輔導", 0xFFD97706),
    ACTIVITY("社團與運動", 0xFF059669),
    SLEEP("固定睡眠時段", 0xFF8B5CF6)
}

object DefaultFixedSchedules {
    val DEFAULTS = listOf(
        FixedScheduleItem(
            title = "學校固定課堂與自習",
            dayOfWeek = 0,
            startTime = "07:30",
            endTime = "17:00",
            category = "SCHOOL",
            notes = "包含上午與下午固定課程及午休"
        ),
        FixedScheduleItem(
            title = "上下學通勤與身心轉換",
            dayOfWeek = 0,
            startTime = "17:00",
            endTime = "17:40",
            category = "COMMUTE",
            notes = "搭車或步行，適度放空釋放疲勞"
        ),
        FixedScheduleItem(
            title = "固定睡眠維護時段",
            dayOfWeek = 0,
            startTime = "23:00",
            endTime = "06:45",
            category = "SLEEP",
            notes = "大腦記憶固化與神經修復不可侵犯時段"
        )
    )
}
