package com.example.data.model

data class DayCompletionStat(
    val date: String,
    val dayOfWeekLabel: String,
    val completedTasks: Int,
    val totalTasks: Int,
    val sleepHours: Float?,
    val isSleepLogged: Boolean,
    val exerciseMinutes: Int,
    val isExerciseLogged: Boolean
)

data class WeeklyReviewMetrics(
    val totalTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val completionRatePercent: Int = 0,
    val avgSleepHours: Float = 0f,
    val loggedHealthDays: Int = 0,
    val unloggedHealthDays: Int = 0,
    val totalExerciseMinutes: Int = 0,
    val journaledDaysCount: Int = 0,
    val dailyStats: List<DayCompletionStat> = emptyList(),
    val recurringDelayedTasks: List<TodoTask> = emptyList(),
    val executiveSummary: String = "",
    val adviceAction: String = ""
)
