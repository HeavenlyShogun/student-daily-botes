package com.example.data.engine

import com.example.data.model.DailyHealthLog
import com.example.data.model.FixedScheduleItem
import com.example.data.model.Priority
import com.example.data.model.TodoTask

data class PlannedBlock(
    val title: String,
    val subtitle: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val category: String, // "FIXED", "TASK", "BUFFER", "REST", "WIND_DOWN"
    val colorHex: Long,
    val taskId: Long? = null,
    val isCompleted: Boolean = false
)

data class SecretaryPlanResult(
    val plannedBlocks: List<PlannedBlock>,
    val scheduledTasksCount: Int,
    val unallocatedTasks: List<TodoTask>,
    val totalFreeMinutes: Int,
    val totalRequiredMinutes: Int,
    val deficitMinutes: Int,
    val deficitExplanation: String?,
    val bedtimeRecommendation: String,
    val isOverloaded: Boolean
)

object SecretaryDecisionEngine {

    /**
     * Converts "HH:mm" to minutes since midnight (0..1439)
     */
    fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        if (parts.size < 2) return 0
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return h * 60 + m
    }

    /**
     * Converts minutes since midnight back to "HH:mm"
     */
    fun minutesToTime(mins: Int): String {
        val m = ((mins % 1440) + 1440) % 1440
        val hours = m / 60
        val minutes = m % 60
        return String.format("%02d:%02d", hours, minutes)
    }

    /**
     * Computes free windows in the student's study period (default 17:40 - 23:00 for evening,
     * or custom based on fixed schedules)
     */
    fun generateDailyPlan(
        fixedSchedules: List<FixedScheduleItem>,
        tasks: List<TodoTask>,
        healthLog: DailyHealthLog?,
        bufferPerTaskMinutes: Int = 10
    ): SecretaryPlanResult {
        val uncompletedTasks = tasks.filter { !it.isCompleted }

        // Determine bedtime
        val bedtimeStr = healthLog?.bedtime?.takeIf { it.isNotBlank() } ?: "23:00"
        val bedtimeMinutes = timeToMinutes(bedtimeStr).let { if (it < 360) it + 1440 else it } // e.g. 23:00 = 1380

        // Evening study window begins after school and commute: default around 17:40 (1060 mins)
        var eveningStartMins = 17 * 60 + 40 // 17:40
        val sortedFixed = fixedSchedules.sortedBy { timeToMinutes(it.startTime) }

        // Find the latest end time of SCHOOL or COMMUTE or CRAM_SCHOOL
        for (item in sortedFixed) {
            val endMins = timeToMinutes(item.endTime)
            if (item.category in listOf("SCHOOL", "COMMUTE", "CRAM_SCHOOL") && endMins > eveningStartMins && endMins < bedtimeMinutes) {
                eveningStartMins = endMins
            }
        }

        // Available study window (e.g. 17:40 to 22:30, leaving 30 mins wind-down before bedtime)
        val windDownStartMins = bedtimeMinutes - 30
        val availableWindowMins = (windDownStartMins - eveningStartMins).coerceAtLeast(0)

        // Sort tasks by strategic cognitive priority:
        // 1. HIGH_URGENT first
        // 2. High postponeCount (repeatedly postponed tasks need immediate focus intervention)
        // 3. HIGH_NOT_URGENT
        // 4. NORMAL / LOW
        val prioritisedTasks = uncompletedTasks.sortedWith(
            compareByDescending<TodoTask> { it.priority == Priority.HIGH_URGENT }
                .thenByDescending { it.postponeCount >= 2 }
                .thenByDescending { it.priority == Priority.HIGH_NOT_URGENT }
                .thenBy { timeToMinutes(it.dueTime.ifBlank { "23:59" }) }
        )

        val plannedBlocks = mutableListOf<PlannedBlock>()

        // 1. Add Fixed Schedule Summary block
        plannedBlocks.add(
            PlannedBlock(
                title = "學校課堂與交通通勤",
                subtitle = "已完成校內作息與常態課堂",
                startTime = "07:30",
                endTime = minutesToTime(eveningStartMins),
                durationMinutes = eveningStartMins - (7 * 60 + 30),
                category = "FIXED",
                colorHex = 0xFF2563EB
            )
        )

        var currentMins = eveningStartMins
        var totalTaskDemandMins = 0
        val scheduledList = mutableListOf<TodoTask>()
        val unallocatedList = mutableListOf<TodoTask>()

        for (task in prioritisedTasks) {
            val taskDuration = task.targetMinutes.coerceIn(5, 120)
            val slotNeeded = taskDuration + bufferPerTaskMinutes
            totalTaskDemandMins += slotNeeded

            if (currentMins + taskDuration <= windDownStartMins) {
                // Schedule task
                val taskStart = currentMins
                val taskEnd = currentMins + taskDuration
                plannedBlocks.add(
                    PlannedBlock(
                        title = "【${task.subjectTag}】${task.title}",
                        subtitle = if (task.postponeCount > 0) "⚠️ 已延期 ${task.postponeCount} 次 · 建議 5 分鐘微步啟動" else "目標專注 $taskDuration 分鐘",
                        startTime = minutesToTime(taskStart),
                        endTime = minutesToTime(taskEnd),
                        durationMinutes = taskDuration,
                        category = "TASK",
                        colorHex = task.priority.badgeColorHex,
                        taskId = task.id,
                        isCompleted = task.isCompleted
                    )
                )
                currentMins = taskEnd
                scheduledList.add(task)

                // Add buffer block if not reaching wind-down
                if (currentMins + bufferPerTaskMinutes <= windDownStartMins) {
                    val bufStart = currentMins
                    val bufEnd = currentMins + bufferPerTaskMinutes
                    plannedBlocks.add(
                        PlannedBlock(
                            title = "大腦緩衝與伸展 (抗疲勞微歇)",
                            subtitle = "站立喝水、遠眺窗外、放鬆眼球，重置認知帶寬",
                            startTime = minutesToTime(bufStart),
                            endTime = minutesToTime(bufEnd),
                            durationMinutes = bufferPerTaskMinutes,
                            category = "BUFFER",
                            colorHex = 0xFF10B981
                        )
                    )
                    currentMins = bufEnd
                }
            } else {
                unallocatedList.add(task)
            }
        }

        // Add 30-min Wind-down & Screen-off block before sleep
        plannedBlocks.add(
            PlannedBlock(
                title = "🌙 數位排毒與就寢準備",
                subtitle = "放下手機、整理書包、深呼吸，守護海馬迴記憶固化",
                startTime = minutesToTime(windDownStartMins),
                endTime = minutesToTime(bedtimeMinutes),
                durationMinutes = 30,
                category = "WIND_DOWN",
                colorHex = 0xFF8B5CF6
            )
        )

        // Add Sleep block
        val wakeMins = timeToMinutes(healthLog?.wakeTime?.takeIf { it.isNotBlank() } ?: "06:45")
        plannedBlocks.add(
            PlannedBlock(
                title = "😴 黃金深層睡眠 (免疫與記憶修復)",
                subtitle = "目標滿 ${healthLog?.targetSleepHours ?: 8.0f} 小時，嚴禁熬夜侵蝕明日專注力",
                startTime = minutesToTime(bedtimeMinutes),
                endTime = minutesToTime(wakeMins),
                durationMinutes = (wakeMins + 1440 - bedtimeMinutes) % 1440,
                category = "SLEEP",
                colorHex = 0xFF4F46E5
            )
        )

        val isOverloaded = unallocatedList.isNotEmpty()
        val deficitMinutes = if (totalTaskDemandMins > availableWindowMins) totalTaskDemandMins - availableWindowMins else 0

        val deficitExplanation = if (isOverloaded) {
            val deferredNames = unallocatedList.joinToString("、") { "「${it.title}」" }
            "⚠️ 今日夜間可支配專注時間為 $availableWindowMins 分鐘，待辦總需求 $totalTaskDemandMins 分鐘（含防疲勞緩衝），超出 $deficitMinutes 分鐘。\n\n" +
                    "💡 秘書科學決策建議：為了嚴守 $bedtimeStr 就寢防線，系統已優先排定最緊急的 ${scheduledList.size} 項核心任務；其餘 $deferredNames 建議延後至明日或週末自習補上，切勿壓縮睡眠時間！"
        } else {
            "✅ 今日時間配置極佳！所有待辦均已在就寢前妥善排程，並包含 ${scheduledList.size * bufferPerTaskMinutes} 分鐘微歇緩衝空間。"
        }

        return SecretaryPlanResult(
            plannedBlocks = plannedBlocks,
            scheduledTasksCount = scheduledList.size,
            unallocatedTasks = unallocatedList,
            totalFreeMinutes = availableWindowMins,
            totalRequiredMinutes = totalTaskDemandMins,
            deficitMinutes = deficitMinutes,
            deficitExplanation = deficitExplanation,
            bedtimeRecommendation = bedtimeStr,
            isOverloaded = isOverloaded
        )
    }
}
