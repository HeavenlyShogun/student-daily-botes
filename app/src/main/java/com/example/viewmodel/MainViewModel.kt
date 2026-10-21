package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.DailyHealthLog
import com.example.data.model.DailyJournalEntry
import com.example.data.model.DefaultPresets
import com.example.data.model.JournalMood
import com.example.data.model.Priority
import com.example.data.model.ReminderStage
import com.example.data.model.RoutinePreset
import com.example.data.model.SubTask
import com.example.data.model.TodoTask
import com.example.data.repository.StudentLifeRepository
import com.example.ui.util.FeedbackManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class FocusTimerUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val activeTaskId: Long? = null,
    val activeTaskTitle: String? = null,
    val modeName: String = "標準番茄鐘 (25分)",
    val ambientSound: String = "白噪音 (雨聲)"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentLifeRepository(AppDatabase.getDatabase(application))
    val feedbackManager = FeedbackManager(application)

    val allTasks: StateFlow<List<TodoTask>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks: StateFlow<List<TodoTask>> = repository.activeTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayHealth: StateFlow<DailyHealthLog?> = repository.getTodayHealthLog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentHealthLogs: StateFlow<List<DailyHealthLog>> = repository.recentHealthLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayJournal: StateFlow<DailyJournalEntry?> = repository.getTodayJournalEntry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allJournals: StateFlow<List<DailyJournalEntry>> = repository.allJournalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePreset: StateFlow<RoutinePreset> = repository.activePreset

    // Timer State
    private val _timerState = MutableStateFlow(FocusTimerUiState())
    val timerState: StateFlow<FocusTimerUiState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    // AI Status & Results
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiConsultationText = MutableStateFlow<String?>(null)
    val aiConsultationText: StateFlow<String?> = _aiConsultationText.asStateFlow()

    private val _aiJournalFeedback = MutableStateFlow<String?>(null)
    val aiJournalFeedback: StateFlow<String?> = _aiJournalFeedback.asStateFlow()

    // Health actions (Immediate local database persistence)
    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.addWater(amountMl)
            feedbackManager.playStartSound()
        }
    }

    fun resetWater() {
        viewModelScope.launch { repository.resetWater() }
    }

    fun logSleep(hours: Float, bedtime: String, wakeTime: String) {
        viewModelScope.launch { repository.logSleep(hours, bedtime, wakeTime) }
    }

    fun logExercise(minutes: Int, type: String) {
        viewModelScope.launch {
            repository.logExercise(minutes, type)
            feedbackManager.playCompleteSound()
        }
    }

    fun toggleMeal(meal: String) {
        viewModelScope.launch {
            repository.toggleMeal(meal)
            feedbackManager.playStartSound()
        }
    }

    fun toggleScreenWindDown() {
        viewModelScope.launch {
            repository.toggleScreenWindDown()
            feedbackManager.playStartSound()
        }
    }

    fun selectPreset(preset: RoutinePreset) {
        repository.setRoutinePreset(preset)
    }

    // Task actions
    fun addTask(
        title: String,
        description: String,
        subject: String,
        priority: Priority,
        targetMinutes: Int,
        microSteps: List<String>,
        subTasks: List<SubTask> = emptyList(),
        dueDate: String = repository.getTodayDateString(),
        dueTime: String = "20:00"
    ) {
        viewModelScope.launch {
            val task = TodoTask(
                title = title,
                description = description,
                subjectTag = subject,
                priority = priority,
                reminderStage = ReminderStage.GENTLE,
                targetMinutes = targetMinutes,
                microSteps = microSteps,
                subTasks = subTasks,
                dueDate = dueDate,
                dueTime = dueTime
            )
            repository.addTask(task)
            feedbackManager.playStartSound()
        }
    }

    fun toggleTaskCompletion(task: TodoTask) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
            if (!task.isCompleted) {
                feedbackManager.playCompleteSound()
            }
        }
    }

    fun toggleSubTask(taskId: Long, subTaskId: String) {
        viewModelScope.launch {
            repository.toggleSubTask(taskId, subTaskId)
            feedbackManager.playStartSound()
        }
    }

    fun addSubTask(taskId: Long, title: String) {
        viewModelScope.launch {
            repository.addSubTask(taskId, title)
        }
    }

    fun deleteSubTask(taskId: Long, subTaskId: String) {
        viewModelScope.launch {
            repository.deleteSubTask(taskId, subTaskId)
        }
    }

    fun advanceMicroStep(task: TodoTask) {
        viewModelScope.launch {
            repository.advanceMicroStep(task)
            feedbackManager.playCompleteSound()
        }
    }

    fun deleteTask(task: TodoTask) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun updateTaskReminderStage(task: TodoTask, stage: ReminderStage) {
        viewModelScope.launch {
            repository.updateTask(task.copy(reminderStage = stage))
        }
    }

    // Journal actions
    fun saveJournal(
        mood: JournalMood,
        energyLevel: Int,
        goodThing1: String,
        goodThing2: String,
        goodThing3: String,
        selfReflection: String,
        tomorrowImprovement: String
    ) {
        viewModelScope.launch {
            val entry = DailyJournalEntry(
                date = repository.getTodayDateString(),
                mood = mood,
                energyLevel = energyLevel,
                goodThing1 = goodThing1,
                goodThing2 = goodThing2,
                goodThing3 = goodThing3,
                selfReflection = selfReflection,
                tomorrowImprovement = tomorrowImprovement
            )
            repository.saveJournalEntry(entry)
            feedbackManager.playCompleteSound()
        }
    }

    // Timer Controls
    fun startFocusSession(minutes: Int, task: TodoTask? = null, modeName: String = "專注學習") {
        timerJob?.cancel()
        val totalSec = minutes * 60
        _timerState.value = FocusTimerUiState(
            isRunning = true,
            isPaused = false,
            remainingSeconds = totalSec,
            totalSeconds = totalSec,
            activeTaskId = task?.id,
            activeTaskTitle = task?.title,
            modeName = modeName,
            ambientSound = _timerState.value.ambientSound
        )
        feedbackManager.playStartSound()

        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0) {
                delay(1000)
                if (!_timerState.value.isPaused) {
                    val remaining = _timerState.value.remainingSeconds - 1
                    _timerState.value = _timerState.value.copy(remainingSeconds = remaining)
                    if (remaining == 0) {
                        feedbackManager.playCompleteSound()
                        val completedMins = _timerState.value.totalSeconds / 60
                        _timerState.value.activeTaskId?.let { taskId ->
                            repository.addFocusMinutesToTask(taskId, completedMins)
                        }
                        _timerState.value = _timerState.value.copy(isRunning = false, isPaused = false)
                    }
                }
            }
        }
    }

    fun togglePauseTimer() {
        val current = _timerState.value
        _timerState.value = current.copy(isPaused = !current.isPaused)
        feedbackManager.playPauseSound()
    }

    fun stopTimer() {
        timerJob?.cancel()
        val spentSeconds = _timerState.value.totalSeconds - _timerState.value.remainingSeconds
        if (spentSeconds >= 60 && _timerState.value.activeTaskId != null) {
            val mins = spentSeconds / 60
            viewModelScope.launch {
                repository.addFocusMinutesToTask(_timerState.value.activeTaskId!!, mins)
            }
        }
        _timerState.value = _timerState.value.copy(isRunning = false, isPaused = false)
        feedbackManager.playPauseSound()
    }

    fun setAmbientSound(sound: String) {
        _timerState.value = _timerState.value.copy(ambientSound = sound)
    }

    // AI Services (Gemini Integration)
    fun decomposeTaskWithAi(title: String, subject: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val result = repository.decomposeTaskWithAi(title, subject)
            _isAiLoading.value = false
            onResult(result.getOrDefault(emptyList()))
        }
    }

    fun requestAiHealthAdvice() {
        viewModelScope.launch {
            val health = todayHealth.value ?: DailyHealthLog(date = repository.getTodayDateString())
            val pendingCount = activeTasks.value.size
            _isAiLoading.value = true
            val result = repository.getAiHealthAdvice(health, pendingCount)
            _isAiLoading.value = false
            _aiConsultationText.value = result.getOrNull()
        }
    }

    fun requestAiJournalFeedback(mood: JournalMood, goodThings: List<String>, reflection: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val result = repository.getAiJournalFeedback(mood, goodThings, reflection)
            _isAiLoading.value = false
            _aiJournalFeedback.value = result.getOrNull()
        }
    }

    fun clearAiConsultation() {
        _aiConsultationText.value = null
    }

    fun clearAiJournalFeedback() {
        _aiJournalFeedback.value = null
    }

    fun getSecretaryGreeting(): SecretaryBriefing {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 5..10 -> SecretaryBriefing(
                title = "晨間早安！今日學習啟動",
                quote = "「晨起一杯溫水與自然日照，是大腦褪黑激素的最佳調節劑。」",
                advice = "先把今天最重要的大科目排在上午黃金思維時段，開啟高能自律一天！",
                iconType = "MORNING"
            )
            hour in 11..13 -> SecretaryBriefing(
                title = "午間充能！避免午後昏睡",
                quote = "「20分鐘的微充電午休，能重置海馬迴，使下午記憶吸收率提高35%。」",
                advice = "午餐多攝取優質蛋白質與青菜，吃飽後稍微散步再小睡20分鐘。",
                iconType = "NOON"
            )
            hour in 14..18 -> SecretaryBriefing(
                title = "午後提神！課間動一動",
                quote = "「連續久坐45分鐘以上，血液循環減緩會讓注意力急速衰退。」",
                advice = "起身伸展雙臂、旋轉肩膀，大口喝一杯水，驅散疲倦感！",
                iconType = "AFTERNOON"
            )
            hour in 19..22 -> SecretaryBriefing(
                title = "夜間專注！清空今日待辦",
                quote = "「面對拖延的秘訣：先做5分鐘。大腦一旦啟動，慣性就會帶你完成。」",
                advice = "使用階梯式提醒梳理繁雜功課，一項一項勾選微小勝利！",
                iconType = "EVENING"
            )
            else -> SecretaryBriefing(
                title = "護眠警示！早睡是大腦超級武器",
                quote = "「熬夜複習等於向明天的智商借高利貸。睡眠才能真正固化今日學到的知識！」",
                advice = "深夜藍光會抑制褪黑激素分泌。請收起手機，深呼吸並提早就寢。",
                iconType = "NIGHT"
            )
        }
    }
}

data class SecretaryBriefing(
    val title: String,
    val quote: String,
    val advice: String,
    val iconType: String
)
