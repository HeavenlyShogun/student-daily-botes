package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.JournalMood
import com.example.data.model.Priority
import com.example.data.model.ReminderStage

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString("|||") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("|||").filter { it.isNotBlank() }
    }

    @TypeConverter
    fun fromPriority(priority: Priority?): String {
        return priority?.name ?: Priority.NORMAL.name
    }

    @TypeConverter
    fun toPriority(value: String?): Priority {
        return try {
            value?.let { Priority.valueOf(it) } ?: Priority.NORMAL
        } catch (_: Exception) {
            Priority.NORMAL
        }
    }

    @TypeConverter
    fun fromReminderStage(stage: ReminderStage?): String {
        return stage?.name ?: ReminderStage.GENTLE.name
    }

    @TypeConverter
    fun toReminderStage(value: String?): ReminderStage {
        return try {
            value?.let { ReminderStage.valueOf(it) } ?: ReminderStage.GENTLE
        } catch (_: Exception) {
            ReminderStage.GENTLE
        }
    }

    @TypeConverter
    fun fromJournalMood(mood: JournalMood?): String {
        return mood?.name ?: JournalMood.GREAT.name
    }

    @TypeConverter
    fun toJournalMood(value: String?): JournalMood {
        return try {
            value?.let { JournalMood.valueOf(it) } ?: JournalMood.GREAT
        } catch (_: Exception) {
            JournalMood.GREAT
        }
    }
}
