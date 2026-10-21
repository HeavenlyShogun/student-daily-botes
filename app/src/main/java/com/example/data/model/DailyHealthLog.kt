package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_health_logs")
data class DailyHealthLog(
    @PrimaryKey
    val date: String, // "YYYY-MM-DD"
    val sleepHours: Float = 0f,
    val targetSleepHours: Float = 8.0f,
    val bedtime: String = "23:00",
    val wakeTime: String = "07:00",
    val isLateNightWarningTriggered: Boolean = false,
    val waterMl: Int = 0,
    val targetWaterMl: Int = 2000,
    val exerciseMinutes: Int = 0,
    val targetExerciseMinutes: Int = 30,
    val exerciseType: String = "課間伸展與快走",
    val breakfastLogged: Boolean = false,
    val lunchLogged: Boolean = false,
    val dinnerLogged: Boolean = false,
    val screenWindDownDone: Boolean = false // 睡前30分鐘數位排毒
) {
    // Calculate sleep debt: target - actual (capped reasonably)
    val sleepDebtHours: Float
        get() = (targetSleepHours - sleepHours).coerceAtLeast(0f)

    // Scientific Health Compliance Score (0 - 100%)
    val healthScore: Int
        get() {
            var score = 0
            // Sleep: up to 35 points
            if (sleepHours >= 7.5f) score += 35
            else if (sleepHours >= 6.0f) score += 20
            else if (sleepHours > 0f) score += 10

            // Water: up to 20 points
            val waterRatio = (waterMl.toFloat() / targetWaterMl.coerceAtLeast(1)).coerceIn(0f, 1f)
            score += (waterRatio * 20).toInt()

            // Exercise: up to 20 points
            val exerciseRatio = (exerciseMinutes.toFloat() / targetExerciseMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
            score += (exerciseRatio * 20).toInt()

            // Nutrition: up to 15 points (5 points each meal)
            if (breakfastLogged) score += 5
            if (lunchLogged) score += 5
            if (dinnerLogged) score += 5

            // Wind down: 10 points
            if (screenWindDownDone) score += 10

            return score.coerceIn(0, 100)
        }
}
