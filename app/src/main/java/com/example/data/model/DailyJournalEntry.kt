package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_journal_entries")
data class DailyJournalEntry(
    @PrimaryKey
    val date: String, // "YYYY-MM-DD"
    val mood: JournalMood = JournalMood.GREAT,
    val energyLevel: Int = 4, // 1 to 5
    val goodThing1: String = "",
    val goodThing2: String = "",
    val goodThing3: String = "",
    val selfReflection: String = "", // 自我對話與心得
    val tomorrowImprovement: String = "", // 明日微調目標
    val updatedAt: Long = System.currentTimeMillis()
)

enum class JournalMood(val emoji: String, val label: String, val encouragingMessage: String) {
    GREAT("🌟", "充實有收穫", "太棒了！今天的大腦神經元進行了深度連結，每一次專注都是進步的基石！"),
    GOOD("😊", "穩定前行", "步調很扎實！穩定的自律習慣就像複利，時間會證明你的成長。"),
    CALM("☕", "心境平靜", "平靜是最高效的專注力源泉。維持這種清明，生活盡在掌握。"),
    TIRED("🥱", "有點疲累", "今天辛苦了！學習是馬拉松，請務必給自己充足的睡眠，大腦會在睡眠中整理記憶。"),
    ANXIOUS("🌧️", "感到焦慮", "深吸一口氣——焦慮代表你在乎。試著把大煩惱拆解成 5 分鐘小任務，一步一步來！")
}
