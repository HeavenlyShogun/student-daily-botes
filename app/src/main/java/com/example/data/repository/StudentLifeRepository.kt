package com.example.data.repository

import com.example.data.ai.GeminiSecretaryService
import com.example.data.db.AppDatabase
import com.example.data.model.DailyHealthLog
import com.example.data.model.DailyJournalEntry
import com.example.data.model.DayCompletionStat
import com.example.data.model.DefaultFixedSchedules
import com.example.data.model.DefaultPresets
import com.example.data.model.FixedScheduleItem
import com.example.data.model.JournalMood
import com.example.data.model.Priority
import com.example.data.model.ReminderStage
import com.example.data.model.RoutinePreset
import com.example.data.model.SubTask
import com.example.data.model.TodoTask
import com.example.data.model.WeeklyReviewMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudentLifeRepository(private val database: AppDatabase) {

    private val todoDao = database.todoDao()
    private val healthDao = database.healthDao()
    private val journalDao = database.journalDao()
    private val fixedScheduleDao = database.fixedScheduleDao()
    val aiService = GeminiSecretaryService()

    private val _activePreset = MutableStateFlow<RoutinePreset>(DefaultPresets.REGULAR_SCHOOL_DAY)
    val activePreset: StateFlow<RoutinePreset> = _activePreset.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    val allTasks: Flow<List<TodoTask>> = todoDao.getAllTasks()
    val activeTasks: Flow<List<TodoTask>> = todoDao.getActiveTasks()
    val allFixedSchedules: Flow<List<FixedScheduleItem>> = fixedScheduleDao.getAllFixedSchedules()

    fun getTodayHealthLog(date: String = getTodayDateString()): Flow<DailyHealthLog?> {
        return healthDao.getHealthLogByDate(date)
    }

    val recentHealthLogs: Flow<List<DailyHealthLog>> = healthDao.getRecentHealthLogs()

    fun getTodayJournalEntry(date: String = getTodayDateString()): Flow<DailyJournalEntry?> {
        return journalDao.getJournalEntryByDate(date)
    }

    val allJournalEntries: Flow<List<DailyJournalEntry>> = journalDao.getAllJournalEntries()

    fun setRoutinePreset(preset: RoutinePreset) {
        _activePreset.value = preset
    }

    // Health Operations (Instant persistence to Room Database)
    suspend fun updateOrCreateHealthLog(updater: (DailyHealthLog) -> DailyHealthLog) = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        val current = healthDao.getHealthLogByDateSync(today) ?: DailyHealthLog(
            date = today,
            targetSleepHours = _activePreset.value.targetSleepHours,
            targetWaterMl = _activePreset.value.waterTargetMl
        )
        val updated = updater(current)
        healthDao.insertOrUpdate(updated)
    }

    suspend fun addWater(amountMl: Int) {
        updateOrCreateHealthLog { log ->
            log.copy(waterMl = (log.waterMl + amountMl).coerceAtLeast(0))
        }
    }

    suspend fun resetWater() {
        updateOrCreateHealthLog { log ->
            log.copy(waterMl = 0)
        }
    }

    suspend fun logSleep(hours: Float, bedtime: String, wakeTime: String) {
        updateOrCreateHealthLog { log ->
            val isLate = bedtime.startsWith("00:") || bedtime.startsWith("01:") || bedtime.startsWith("02:") ||
                    (bedtime.startsWith("23:") && (bedtime.substring(3).toIntOrNull() ?: 0) >= 30)
            log.copy(
                sleepHours = hours,
                bedtime = bedtime,
                wakeTime = wakeTime,
                isLateNightWarningTriggered = isLate
            )
        }
    }

    suspend fun logExercise(minutes: Int, type: String) {
        updateOrCreateHealthLog { log ->
            log.copy(
                exerciseMinutes = log.exerciseMinutes + minutes,
                exerciseType = type
            )
        }
    }

    suspend fun toggleMeal(meal: String) {
        updateOrCreateHealthLog { log ->
            when (meal) {
                "breakfast" -> log.copy(breakfastLogged = !log.breakfastLogged)
                "lunch" -> log.copy(lunchLogged = !log.lunchLogged)
                "dinner" -> log.copy(dinnerLogged = !log.dinnerLogged)
                else -> log
            }
        }
    }

    suspend fun toggleScreenWindDown() {
        updateOrCreateHealthLog { log ->
            log.copy(screenWindDownDone = !log.screenWindDownDone)
        }
    }

    // Task Operations
    suspend fun addTask(task: TodoTask): Long = withContext(Dispatchers.IO) {
        todoDao.insertTask(task)
    }

    suspend fun updateTask(task: TodoTask) = withContext(Dispatchers.IO) {
        todoDao.updateTask(task)
    }

    suspend fun toggleTaskCompletion(task: TodoTask) = withContext(Dispatchers.IO) {
        val newCompleted = !task.isCompleted
        // When parent task is toggled, sync all subtasks to match parent
        val updatedSubTasks = task.subTasks.map { it.copy(isCompleted = newCompleted) }
        val updated = task.copy(
            isCompleted = newCompleted,
            subTasks = updatedSubTasks,
            completedAt = if (newCompleted) System.currentTimeMillis() else null
        )
        todoDao.updateTask(updated)
    }

    suspend fun toggleSubTask(taskId: Long, subTaskId: String) = withContext(Dispatchers.IO) {
        val task = todoDao.getTaskById(taskId) ?: return@withContext
        val updatedSubTasks = task.subTasks.map { subTask ->
            if (subTask.id == subTaskId) subTask.copy(isCompleted = !subTask.isCompleted) else subTask
        }
        val allCompleted = updatedSubTasks.isNotEmpty() && updatedSubTasks.all { it.isCompleted }
        val updatedTask = task.copy(
            subTasks = updatedSubTasks,
            isCompleted = allCompleted,
            completedAt = if (allCompleted) System.currentTimeMillis() else if (task.isCompleted && !allCompleted) null else task.completedAt
        )
        todoDao.updateTask(updatedTask)
    }

    suspend fun addSubTask(taskId: Long, title: String) = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext
        val task = todoDao.getTaskById(taskId) ?: return@withContext
        val newSubTask = SubTask(title = title.trim(), isCompleted = false)
        val updatedSubTasks = task.subTasks + newSubTask
        val updatedTask = task.copy(
            subTasks = updatedSubTasks,
            isCompleted = false
        )
        todoDao.updateTask(updatedTask)
    }

    suspend fun deleteSubTask(taskId: Long, subTaskId: String) = withContext(Dispatchers.IO) {
        val task = todoDao.getTaskById(taskId) ?: return@withContext
        val updatedSubTasks = task.subTasks.filterNot { it.id == subTaskId }
        val allCompleted = updatedSubTasks.isNotEmpty() && updatedSubTasks.all { it.isCompleted }
        val updatedTask = task.copy(
            subTasks = updatedSubTasks,
            isCompleted = allCompleted
        )
        todoDao.updateTask(updatedTask)
    }

    suspend fun advanceMicroStep(task: TodoTask) = withContext(Dispatchers.IO) {
        if (task.microSteps.isNotEmpty()) {
            val nextIndex = task.currentMicroStepIndex + 1
            val isDone = nextIndex >= task.microSteps.size
            val updated = task.copy(
                currentMicroStepIndex = nextIndex.coerceAtMost(task.microSteps.size),
                isCompleted = isDone,
                completedAt = if (isDone) System.currentTimeMillis() else task.completedAt
            )
            todoDao.updateTask(updated)
        }
    }

    suspend fun deleteTask(task: TodoTask) = withContext(Dispatchers.IO) {
        todoDao.deleteTask(task)
    }

    suspend fun addFocusMinutesToTask(taskId: Long, minutes: Int) = withContext(Dispatchers.IO) {
        val task = todoDao.getTaskById(taskId)
        if (task != null) {
            val updated = task.copy(completedMinutes = task.completedMinutes + minutes)
            todoDao.updateTask(updated)
        }
    }

    // Journal Operations
    suspend fun saveJournalEntry(entry: DailyJournalEntry) = withContext(Dispatchers.IO) {
        journalDao.insertOrUpdate(entry)
    }

    // Fixed Schedule Operations
    suspend fun addFixedSchedule(schedule: FixedScheduleItem): Long = withContext(Dispatchers.IO) {
        fixedScheduleDao.insertSchedule(schedule)
    }

    suspend fun deleteFixedSchedule(schedule: FixedScheduleItem) = withContext(Dispatchers.IO) {
        fixedScheduleDao.deleteSchedule(schedule)
    }

    suspend fun getAllFixedSchedulesSync(): List<FixedScheduleItem> = withContext(Dispatchers.IO) {
        fixedScheduleDao.getAllFixedSchedulesSync()
    }

    // Weekly Review Analytics (Distinguishing Unlogged vs Incomplete)
    suspend fun getWeeklyReviewMetrics(): WeeklyReviewMetrics = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLabelFormat = SimpleDateFormat("E", Locale.TAIWAN)
        val cal = Calendar.getInstance()

        val past7Days = (6 downTo 0).map { daysAgo ->
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val dStr = sdf.format(c.time)
            val lStr = dayLabelFormat.format(c.time)
            dStr to lStr
        }

        val allTasksList = todoDao.getAllTasks().first()
        val dailyStatsList = mutableListOf<DayCompletionStat>()
        var loggedDays = 0
        var unloggedDays = 0
        var totalSleepSum = 0f
        var totalExercise = 0
        var journalDays = 0

        for ((dateStr, label) in past7Days) {
            val tasksForDay = allTasksList.filter { it.dueDate == dateStr }
            val completedCount = tasksForDay.count { it.isCompleted }
            val totalCount = tasksForDay.size

            val health = healthDao.getHealthLogByDateSync(dateStr)
            val isSleepLogged = health != null && health.sleepHours > 0f
            val isExerciseLogged = health != null

            if (health != null) {
                loggedDays++
                if (health.sleepHours > 0f) totalSleepSum += health.sleepHours
                totalExercise += health.exerciseMinutes
            } else {
                unloggedDays++
            }

            val journal = journalDao.getJournalEntryByDateSync(dateStr)
            if (journal != null && journal.goodThing1.isNotBlank()) {
                journalDays++
            }

            dailyStatsList.add(
                DayCompletionStat(
                    date = dateStr,
                    dayOfWeekLabel = label,
                    completedTasks = completedCount,
                    totalTasks = totalCount,
                    sleepHours = health?.sleepHours,
                    isSleepLogged = isSleepLogged,
                    exerciseMinutes = health?.exerciseMinutes ?: 0,
                    isExerciseLogged = isExerciseLogged
                )
            )
        }

        val recurringDelayed = allTasksList.filter { it.postponeCount >= 2 || it.isRecurringDelayed }
        val totalTasks = allTasksList.size
        val completedTasks = allTasksList.count { it.isCompleted }
        val completionRate = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 100
        val avgSleep = if (loggedDays > 0) (totalSleepSum / loggedDays) else 7.5f

        val executiveSummary = buildString {
            append("本週任務完成率達 $completionRate%，已完成 $completedTasks 項學習待辦。")
            if (recurringDelayed.isNotEmpty()) {
                append(" ⚠️ 偵測到 ${recurringDelayed.size} 項反覆延期（延期2次以上）的卡關任務。")
            }
            if (unloggedDays > 0) {
                append(" 本週有 $unloggedDays 天尚未記錄生理數據（未記錄 ≠ 未達成）。")
            }
            if (avgSleep < 7.0f) {
                append(" 平均睡眠時數僅 ${String.format("%.1f", avgSleep)} 小時，累積了輕微睡眠債。")
            } else {
                append(" 平均睡眠達 ${String.format("%.1f", avgSleep)} 小時，大腦神經修復良好！")
            }
        }

        val adviceAction = when {
            recurringDelayed.isNotEmpty() ->
                "💡 針對反覆延期的「${recurringDelayed.first().title}」，請嘗試開啟「5分鐘微步拆解」，降低啟動阻力！"
            avgSleep < 7.0f ->
                "💡 睡眠是記憶穩固的黃金基石，今晚請將就寢時間提前 30 分鐘，嚴守 23:00 就寢邊界。"
            else ->
                "💡 節律維持優異！保持每段專注間留有 10~15 分鐘放鬆緩衝，避免認知疲乏過載。"
        }

        WeeklyReviewMetrics(
            totalTasksCount = totalTasks,
            completedTasksCount = completedTasks,
            completionRatePercent = completionRate,
            avgSleepHours = avgSleep,
            loggedHealthDays = loggedDays,
            unloggedHealthDays = unloggedDays,
            totalExerciseMinutes = totalExercise,
            journaledDaysCount = journalDays,
            dailyStats = dailyStatsList,
            recurringDelayedTasks = recurringDelayed,
            executiveSummary = executiveSummary,
            adviceAction = adviceAction
        )
    }

    // AI Services
    suspend fun decomposeTaskWithAi(title: String, subject: String): Result<List<String>> {
        return aiService.decomposeTask(title, subject)
    }

    suspend fun getAiHealthAdvice(healthLog: DailyHealthLog, pendingTasksCount: Int): Result<String> {
        return aiService.generateHealthConsultation(
            sleepHours = healthLog.sleepHours,
            sleepDebtHours = healthLog.sleepDebtHours,
            waterMl = healthLog.waterMl,
            exerciseMinutes = healthLog.exerciseMinutes,
            pendingTasksCount = pendingTasksCount
        )
    }

    suspend fun getAiJournalFeedback(mood: JournalMood, goodThings: List<String>, reflection: String): Result<String> {
        return aiService.generateJournalFeedback(mood.label, goodThings, reflection)
    }

    // Seed Data
    private suspend fun seedInitialDataIfNeeded() {
        val existingTasks = todoDao.getAllTasks().first()
        val today = getTodayDateString()

        if (fixedScheduleDao.getAllFixedSchedulesSync().isEmpty()) {
            fixedScheduleDao.insertSchedules(DefaultFixedSchedules.DEFAULTS)
        }

        if (existingTasks.isEmpty()) {
            val sampleTasks = listOf(
                TodoTask(
                    title = "數學複習講義：第3單元雙重根式 (5分鐘啟動法)",
                    description = "遇到難題不要逃避！先花5分鐘翻開講義，讀懂第一個例題。",
                    subjectTag = "數學",
                    priority = Priority.HIGH_URGENT,
                    reminderStage = ReminderStage.FOCUS,
                    targetMinutes = 45,
                    completedMinutes = 15,
                    microSteps = listOf(
                        "微步1：拿出講義翻到第24頁，看懂範例1解法 (5分鐘)",
                        "微步2：遮住解答，自己動筆演算練習題1 (8分鐘)",
                        "微步3：連續算完第2~4題，紅筆圈出疑惑點 (15分鐘)",
                        "微步4：翻到課堂筆記對照公式並標註觀念 (5分鐘)"
                    ),
                    currentMicroStepIndex = 1,
                    subTasks = listOf(
                        SubTask(title = "閱讀講義範例一與公式解析", isCompleted = true),
                        SubTask(title = "親手演算範例二並核對解題步驟", isCompleted = false),
                        SubTask(title = "完成課後練習題 1~4 題", isCompleted = false),
                        SubTask(title = "用螢光筆標記錯題觀念筆記", isCompleted = false)
                    ),
                    dueDate = today,
                    dueTime = "20:30"
                ),
                TodoTask(
                    title = "英文核心單字 7000 Unit 12 朗讀與例句",
                    description = "睡前大腦突觸連結記憶法：大聲朗讀3遍，加深海馬迴印記。",
                    subjectTag = "英文",
                    priority = Priority.HIGH_NOT_URGENT,
                    reminderStage = ReminderStage.GENTLE,
                    targetMinutes = 25,
                    completedMinutes = 0,
                    microSteps = listOf(
                        "微步1：播放音檔快速跟讀10個單字",
                        "微步2：用螢光筆標記不熟詞彙",
                        "微步3：自造2個考試常用情境句"
                    ),
                    subTasks = listOf(
                        SubTask(title = "跟讀 Unit 12 核心單字音檔一遍", isCompleted = false),
                        SubTask(title = "背誦並默寫 5 個高頻必考片語", isCompleted = false),
                        SubTask(title = "閱讀課後閱讀測驗並劃記關鍵字", isCompleted = false)
                    ),
                    dueDate = today,
                    dueTime = "21:30"
                ),
                TodoTask(
                    title = "課間久坐放鬆：肩頸與眼部舒緩伸展",
                    description = "連續低頭讀書會造成頸椎過載。利用下課5分鐘做聳肩與眼球旋轉。",
                    subjectTag = "生活",
                    priority = Priority.NORMAL,
                    reminderStage = ReminderStage.GENTLE,
                    targetMinutes = 5,
                    completedMinutes = 5,
                    isCompleted = true,
                    subTasks = listOf(
                        SubTask(title = "起身遠眺 6 公尺窗外綠景 20 秒", isCompleted = true),
                        SubTask(title = "雙手交扣向後伸展斜方肌 1 分鐘", isCompleted = true)
                    ),
                    dueDate = today,
                    dueTime = "16:00"
                ),
                TodoTask(
                    title = "物理力學：牛頓第二運動定律觀念統整",
                    description = "結合歷屆大考試題，整理受力分析圖常見陷阱。",
                    subjectTag = "自然",
                    priority = Priority.HIGH_NOT_URGENT,
                    reminderStage = ReminderStage.GENTLE,
                    targetMinutes = 40,
                    subTasks = listOf(
                        SubTask(title = "繪製滑輪與斜面系統受力分析圖", isCompleted = false),
                        SubTask(title = "列出 F=ma 聯立方程並求加速度", isCompleted = false)
                    ),
                    dueDate = today,
                    dueTime = "22:00"
                )
            )
            todoDao.insertTasks(sampleTasks)
        }

        val currentHealth = healthDao.getHealthLogByDateSync(today)
        if (currentHealth == null) {
            val initialHealth = DailyHealthLog(
                date = today,
                sleepHours = 7.5f,
                targetSleepHours = 8.0f,
                bedtime = "23:15",
                wakeTime = "06:45",
                waterMl = 1250,
                targetWaterMl = 2000,
                exerciseMinutes = 20,
                targetExerciseMinutes = 30,
                exerciseType = "課間伸展與快走",
                breakfastLogged = true,
                lunchLogged = true,
                dinnerLogged = false,
                screenWindDownDone = false
            )
            healthDao.insertOrUpdate(initialHealth)
        }

        val currentJournal = journalDao.getJournalEntryByDateSync(today)
        if (currentJournal == null) {
            val initialJournal = DailyJournalEntry(
                date = today,
                mood = JournalMood.GREAT,
                energyLevel = 4,
                goodThing1 = "今天數學小考第二題完全按照步驟推導，沒有粗心算錯！",
                goodThing2 = "放學有去操場快走了15分鐘，吹著微風感覺腦袋非常清爽。",
                goodThing3 = "中午午休有睡了20分鐘，下午兩節物理課完全沒有打瞌睡。",
                selfReflection = "今天發現只要提早把手機放到抽屜，做功課的阻力就變小很多。5分鐘微步法則真的很有效，先算一題不知不覺就把一整頁寫完了。",
                tomorrowImprovement = "明天晚上10:45要提早開啟夜間模式，把書包整理好，不要拖到快12點才洗澡。"
            )
            journalDao.insertOrUpdate(initialJournal)
        }
    }
}
