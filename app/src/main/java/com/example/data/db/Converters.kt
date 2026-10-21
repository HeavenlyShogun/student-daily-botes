package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.JournalMood
import com.example.data.model.Priority
import com.example.data.model.ReminderStage
import com.example.data.model.SubTask
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
    fun fromSubTaskList(list: List<SubTask>?): String {
        if (list.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("isCompleted", item.isCompleted)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toSubTaskList(value: String?): List<SubTask> {
        if (value.isNullOrBlank() || value == "[]") return emptyList()
        val list = mutableListOf<SubTask>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SubTask(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
        } catch (_: Exception) {
            // Safe fallback
        }
        return list
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
