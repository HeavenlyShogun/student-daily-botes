package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.DailyHealthLog
import com.example.data.model.DailyJournalEntry
import com.example.data.model.DefaultPresets
import com.example.data.model.JournalMood
import com.example.data.model.Priority
import com.example.data.model.ReminderStage
import com.example.data.model.RoutinePreset
import com.example.data.model.TodoTask
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
import java.util.Date
import java.util.Locale

class StudentLifeRepository(private val database: AppDatabase) {

    private val todoDao = database.todoDao()
    private val healthDao = database.healthDao()
    private val journalDao = database.journalDao()

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

    // Health Operations
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
            log.copy(waterMl = log.waterMl + amountMl)
        }
    }

    suspend fun resetWater() {
        updateOrCreateHealthLog { log ->
            log.copy(waterMl = 0)
        }
    }

    suspend fun logSleep(hours: Float, bedtime: String, wakeTime: String) {
        updateOrCreateHealthLog { log ->
            // Check if bedtime is past 23:30 (e.g. "23:45", "00:30")
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
        val updated = task.copy(
            isCompleted = !task.isCompleted,
            completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
        )
        todoDao.updateTask(updated)
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

    // Seed Data
    private suspend fun seedInitialDataIfNeeded() {
        val existingTasks = todoDao.getAllTasks().first()
        val today = getTodayDateString()

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
                    dueDate = today,
                    dueTime = "22:00"
                )
            )
            todoDao.insertTasks(sampleTasks)
        }

        // Initialize today's health log if empty
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

        // Initialize today's journal entry if empty
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
