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
    val category: String, // "FIXED", "TASK", "BUFFER", "REST", "WIND_DOWN", "SLEEP"
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
    val isOverloaded: Boolean,
    val scheduledSlotMap: Map<Long, Pair<String, String>> = emptyMap()
)

object SecretaryDecisionEngine {

    private data class TimeInterval(
        val startMins: Int,
        val endMins: Int,
        val title: String = "",
        val category: String = "FIXED"
    )

    /**
     * Converts "HH:mm" to minutes since midnight (0..1439)
     */
    fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        if (parts.size < 2) return 0
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return (h.coerceIn(0, 23) * 60) + m.coerceIn(0, 59)
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
     * Merges overlapping or adjacent intervals so overlapping fixed schedules
     * are never double-counted or mis-ordered.
     */
    private fun mergeIntervals(intervals: List<TimeInterval>): List<TimeInterval> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedWith(compareBy<TimeInterval> { it.startMins }.thenBy { it.endMins })
        val merged = mutableListOf<TimeInterval>()
        var current = sorted.first()

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startMins <= current.endMins) {
                val combinedTitle = if (current.title.isBlank()) next.title
                else if (next.title.isBlank() || current.title.contains(next.title)) current.title
                else "${current.title} / ${next.title}"
                current = current.copy(
                    endMins = maxOf(current.endMins, next.endMins),
                    title = combinedTitle
                )
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    /**
     * Computes free windows in the student's study period (default 17:40 - 23:00 for evening,
     * or custom based on fixed schedules, supporting overlapping cram school / evening activities
     * and cross-midnight bedtimes).
     */
    fun generateDailyPlan(
        fixedSchedules: List<FixedScheduleItem>,
        tasks: List<TodoTask>,
        healthLog: DailyHealthLog?,
        bufferPerTaskMinutes: Int = 10
    ): SecretaryPlanResult {
        val uncompletedTasks = tasks.filter { !it.isCompleted }

        // Determine bedtime (supports cross-midnight e.g. "00:30" -> 1470 mins)
        val bedtimeStr = healthLog?.bedtime?.takeIf { it.isNotBlank() } ?: "23:00"
        val bedtimeMinutes = timeToMinutes(bedtimeStr).let { if (it < 360) it + 1440 else it }
        val windDownStartMins = bedtimeMinutes - 30

        // Normalize fixed schedules into intervals
        val validFixedIntervals = fixedSchedules.mapNotNull { item ->
            val s = timeToMinutes(item.startTime)
            var e = timeToMinutes(item.endTime)
            if (e < s && e < 360) e += 1440
            if (e <= s) null else TimeInterval(s, e, item.title, item.category)
        }
        val mergedFixed = mergeIntervals(validFixedIntervals)

        // Base daytime school/commute end: default 17:40 (1060 mins)
        var eveningStartMins = 17 * 60 + 40
        for (interval in mergedFixed) {
            // If a daytime block (starting before 17:40) extends past 17:40, push evening start
            if (interval.startMins <= eveningStartMins && interval.endMins > eveningStartMins) {
                eveningStartMins = interval.endMins.coerceAtMost(windDownStartMins)
            }
        }

        // Evening fixed blocks that occur after eveningStartMins and before windDownStartMins
        // (e.g., evening cram school 18:30-20:00 or family dinner)
        val eveningFixedBlocks = mergedFixed.mapNotNull { interval ->
            val clampedStart = maxOf(interval.startMins, eveningStartMins)
            val clampedEnd = minOf(interval.endMins, windDownStartMins)
            if (clampedEnd > clampedStart && interval.startMins > 17 * 60 + 40) {
                interval.copy(startMins = clampedStart, endMins = clampedEnd)
            } else {
                null
            }
        }

        // Compute free study intervals in [eveningStartMins, windDownStartMins]
        val freeWindows = mutableListOf<Pair<Int, Int>>()
        if (windDownStartMins > eveningStartMins) {
            var cursor = eveningStartMins
            for (fixed in eveningFixedBlocks) {
                if (fixed.startMins > cursor) {
                    freeWindows.add(cursor to fixed.startMins)
                }
                cursor = maxOf(cursor, fixed.endMins)
            }
            if (cursor < windDownStartMins) {
                freeWindows.add(cursor to windDownStartMins)
            }
        }

        val availableWindowMins = freeWindows.sumOf { (s, e) -> (e - s).coerceAtLeast(0) }

        // Sort tasks by strategic cognitive priority:
        // 1. Priority.weight descending (HIGH_URGENT=4 > HIGH_NOT_URGENT=3 > NORMAL=2 > LOW=1)
        // 2. Recurring delayed tasks (postponeCount >= 2) boosted first within same priority
        // 3. Earlier dueDate + dueTime first
        // 4. Higher postponeCount tie-breaker
        val prioritisedTasks = uncompletedTasks.sortedWith(
            compareByDescending<TodoTask> { it.priority.weight }
                .thenByDescending { it.postponeCount >= 2 }
                .thenBy { it.dueDate.ifBlank { "9999-12-31" } }
                .thenBy { timeToMinutes(it.dueTime.ifBlank { "23:59" }) }
                .thenByDescending { it.postponeCount }
                .thenBy { it.id }
        )

        val plannedBlocks = mutableListOf<PlannedBlock>()

        // 1. Add Daytime Fixed Schedule Summary block
        val daytimeStart = mergedFixed.minOfOrNull { it.startMins }?.coerceAtMost(7 * 60 + 30) ?: (7 * 60 + 30)
        if (eveningStartMins > daytimeStart) {
            plannedBlocks.add(
                PlannedBlock(
                    title = "學校課堂與交通通勤",
                    subtitle = "已完成校內作息與常態課堂",
                    startTime = minutesToTime(daytimeStart),
                    endTime = minutesToTime(eveningStartMins),
                    durationMinutes = eveningStartMins - daytimeStart,
                    category = "FIXED",
                    colorHex = 0xFF2563EB
                )
            )
        }

        var totalTaskDemandMins = 0
        val scheduledList = mutableListOf<TodoTask>()
        val unallocatedList = mutableListOf<TodoTask>()
        val scheduledSlotMap = LinkedHashMap<Long, Pair<String, String>>()

        // Mutable pointers into freeWindows
        val mutableWindows = freeWindows.map { intArrayOf(it.first, it.second) }.toMutableList()
        val taskAndBufferBlocks = mutableListOf<Pair<Int, PlannedBlock>>()

        for (task in prioritisedTasks) {
            val taskDuration = task.targetMinutes.coerceIn(5, 120)
            val slotNeeded = taskDuration + bufferPerTaskMinutes
            totalTaskDemandMins += slotNeeded

            // Find the first free window that can fit taskDuration
            val winIndex = mutableWindows.indexOfFirst { (wStart, wEnd) -> (wEnd - wStart) >= taskDuration }
            if (winIndex != -1) {
                val win = mutableWindows[winIndex]
                val taskStart = win[0]
                val taskEnd = taskStart + taskDuration
                val startStr = minutesToTime(taskStart)
                val endStr = minutesToTime(taskEnd)

                val dueMins = task.dueTime.takeIf { it.isNotBlank() }?.let { timeToMinutes(it) }
                val isPastDueSlot = dueMins != null && taskEnd > dueMins

                val subtitleText = buildString {
                    when {
                        task.postponeCount >= 2 -> append("⚠️ 反覆延期 ${task.postponeCount} 次 · 啟動 5 分鐘微步破冰")
                        task.postponeCount == 1 -> append("⚠️ 已延期 1 次 · 建議 5 分鐘微步啟動")
                        else -> append("目標專注 $taskDuration 分鐘")
                    }
                    if (isPastDueSlot) {
                        append("（逾原訂 ${task.dueTime} 截止，優先補救）")
                    }
                }

                taskAndBufferBlocks.add(
                    taskStart to PlannedBlock(
                        title = "【${task.subjectTag}】${task.title}",
                        subtitle = subtitleText,
                        startTime = startStr,
                        endTime = endStr,
                        durationMinutes = taskDuration,
                        category = "TASK",
                        colorHex = task.priority.badgeColorHex,
                        taskId = task.id,
                        isCompleted = task.isCompleted
                    )
                )
                if (task.id > 0L) {
                    scheduledSlotMap[task.id] = startStr to endStr
                }
                scheduledList.add(task)
                win[0] = taskEnd

                // Add buffer block if it fits in the same window
                if (bufferPerTaskMinutes > 0 && win[0] + bufferPerTaskMinutes <= win[1]) {
                    val bufStart = win[0]
                    val bufEnd = bufStart + bufferPerTaskMinutes
                    taskAndBufferBlocks.add(
                        bufStart to PlannedBlock(
                            title = "大腦緩衝與伸展 (抗疲勞微歇)",
                            subtitle = "站立喝水、遠眺窗外、放鬆眼球，重置認知帶寬",
                            startTime = minutesToTime(bufStart),
                            endTime = minutesToTime(bufEnd),
                            durationMinutes = bufferPerTaskMinutes,
                            category = "BUFFER",
                            colorHex = 0xFF10B981
                        )
                    )
                    win[0] = bufEnd
                }
            } else {
                unallocatedList.add(task)
            }
        }

        // Also include evening fixed blocks (e.g. cram school) in chronological order
        for (fixed in eveningFixedBlocks) {
            taskAndBufferBlocks.add(
                fixed.startMins to PlannedBlock(
                    title = "📌 固定行程：${fixed.title.ifBlank { "固定活動" }}",
                    subtitle = "已保護固定課表與通勤時段，不排入重度自習",
                    startTime = minutesToTime(fixed.startMins),
                    endTime = minutesToTime(fixed.endMins),
                    durationMinutes = fixed.endMins - fixed.startMins,
                    category = "FIXED",
                    colorHex = 0xFF2563EB
                )
            )
        }

        plannedBlocks.addAll(taskAndBufferBlocks.sortedBy { it.first }.map { it.second })

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

        // Add Sleep block (supports cross-midnight bedtime)
        val wakeMins = timeToMinutes(healthLog?.wakeTime?.takeIf { it.isNotBlank() } ?: "06:45")
        val sleepDurationMins = ((wakeMins + 1440) - (bedtimeMinutes % 1440)) % 1440
        plannedBlocks.add(
            PlannedBlock(
                title = "😴 黃金深層睡眠 (免疫與記憶修復)",
                subtitle = "目標滿 ${healthLog?.targetSleepHours ?: 8.0f} 小時，嚴禁熬夜侵蝕明日專注力",
                startTime = minutesToTime(bedtimeMinutes),
                endTime = minutesToTime(wakeMins),
                durationMinutes = sleepDurationMins,
                category = "SLEEP",
                colorHex = 0xFF4F46E5
            )
        )

        val isOverloaded = unallocatedList.isNotEmpty()
        val deficitMinutes = if (totalTaskDemandMins > availableWindowMins) {
            totalTaskDemandMins - availableWindowMins
        } else if (isOverloaded) {
            unallocatedList.sumOf { it.targetMinutes.coerceIn(5, 120) }
        } else {
            0
        }

        val deficitExplanation = if (isOverloaded) {
            val deferredNames = unallocatedList.joinToString("、") { "「${it.title}」" }
            "⚠️ 今日夜間可支配專注時間為 $availableWindowMins 分鐘，待辦總需求 $totalTaskDemandMins 分鐘（含防疲勞緩衝），超出 $deficitMinutes 分鐘。\n\n" +
                    "💡 秘書科學決策建議：為了嚴守 $bedtimeStr 就寢防線並保護固定課表，系統已優先排定最緊急的 ${scheduledList.size} 項核心任務；其餘 $deferredNames 建議延後至明日或週末自習補上，切勿壓縮睡眠時間！"
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
            isOverloaded = isOverloaded,
            scheduledSlotMap = scheduledSlotMap
        )
    }
}

