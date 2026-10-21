package com.example.ui.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.ReminderStage
import com.example.data.model.SubTask
import com.example.data.model.TodoTask
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.viewmodel.MainViewModel

@Composable
fun TodoListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    var selectedPriorityFilter by remember { mutableStateOf<Priority?>(null) }
    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }
    var showOnlyIncomplete by remember { mutableStateOf(false) }

    // Dialog state
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskForProcrastinationBuster by remember { mutableStateOf<TodoTask?>(null) }
    var showTimerModal by remember { mutableStateOf(false) }
    var taskForAddSubTask by remember { mutableStateOf<TodoTask?>(null) }

    val filteredTasks = tasks.filter { task ->
        (selectedPriorityFilter == null || task.priority == selectedPriorityFilter) &&
        (selectedSubjectFilter == null || task.subjectTag == selectedSubjectFilter) &&
        (!showOnlyIncomplete || !task.isCompleted)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("todo_list_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SteppedReminderBanner()
            }

            // Focus Timer Quick Entry Card
            item {
                FocusTimerQuickCard(
                    timerState = timerState,
                    onOpenTimerModal = { showTimerModal = true },
                    onTogglePause = { viewModel.togglePauseTimer() },
                    onStop = { viewModel.stopTimer() }
                )
            }

            // Filters & Dynamic Category selector
            item {
                TaskFilterSection(
                    selectedPriority = selectedPriorityFilter,
                    onSelectPriority = { selectedPriorityFilter = it },
                    selectedSubject = selectedSubjectFilter,
                    onSelectSubject = { selectedSubjectFilter = it },
                    showOnlyIncomplete = showOnlyIncomplete,
                    onToggleIncomplete = { showOnlyIncomplete = !showOnlyIncomplete }
                )
            }

            // Task list items
            if (filteredTasks.isEmpty()) {
                item {
                    EmptyTaskStateCard(onAddTask = { showAddTaskDialog = true })
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                        onToggleSubTask = { subTaskId -> viewModel.toggleSubTask(task.id, subTaskId) },
                        onAddSubTaskClick = { taskForAddSubTask = task },
                        onDeleteSubTask = { subTaskId -> viewModel.deleteSubTask(task.id, subTaskId) },
                        onStartTimer = {
                            viewModel.startFocusSession(task.targetMinutes, task, "${task.subjectTag} 專注")
                            showTimerModal = true
                        },
                        onOpenProcrastinationBuster = { taskForProcrastinationBuster = task },
                        onAdvanceMicroStep = { viewModel.advanceMicroStep(task) },
                        onUpdateReminderStage = { stage -> viewModel.updateTaskReminderStage(task, stage) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add Task Dialog (with AI decomposition)
    if (showAddTaskDialog) {
        AddTaskDialog(
            isAiLoading = isAiLoading,
            onDismiss = { showAddTaskDialog = false },
            onDecomposeWithAi = { title, subject, onResult ->
                viewModel.decomposeTaskWithAi(title, subject, onResult)
            },
            onSave = { title, desc, subject, priority, targetMin, microSteps, subTasks, dueDate, dueTime ->
                viewModel.addTask(title, desc, subject, priority, targetMin, microSteps, subTasks, dueDate, dueTime)
                showAddTaskDialog = false
            }
        )
    }

    // Add Subtask Dialog
    taskForAddSubTask?.let { task ->
        AddSubTaskDialog(
            taskTitle = task.title,
            onDismiss = { taskForAddSubTask = null },
            onAdd = { subTitle ->
                viewModel.addSubTask(task.id, subTitle)
                taskForAddSubTask = null
            }
        )
    }

    // 5-Minute Procrastination Buster Modal
    taskForProcrastinationBuster?.let { task ->
        ProcrastinationBusterDialog(
            task = task,
            isAiLoading = isAiLoading,
            onDismiss = { taskForProcrastinationBuster = null },
            onAiDecompose = {
                viewModel.decomposeTaskWithAi(task.title, task.subjectTag) { newSteps ->
                    viewModel.updateTaskReminderStage(task, ReminderStage.FOCUS)
                }
            },
            onAdvanceMicroStep = {
                viewModel.advanceMicroStep(task)
            },
            onStart5MinSession = {
                viewModel.startFocusSession(5, task, "5分鐘微步啟動")
                taskForProcrastinationBuster = null
                showTimerModal = true
            }
        )
    }

    // Dedicated Focus Timer Modal
    if (showTimerModal) {
        FocusTimerDialog(
            timerState = timerState,
            onDismiss = { showTimerModal = false },
            onStartNewSession = { minutes, modeName ->
                viewModel.startFocusSession(minutes, null, modeName)
            },
            onTogglePause = { viewModel.togglePauseTimer() },
            onStop = {
                viewModel.stopTimer()
                showTimerModal = false
            },
            onSelectAmbientSound = { viewModel.setAmbientSound(it) }
        )
    }
}

@Composable
fun SteppedReminderBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Stepped Reminder",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "三階動態提醒機制：降低認知負荷",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1. 溫和預告 (材料就緒) ➔ 2. 專注執行 (5分鐘微步突破) ➔ 3. 進度微調 (拒絕焦慮自責，重新分解步驟)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun FocusTimerQuickCard(
    timerState: com.example.viewmodel.FocusTimerUiState,
    onOpenTimerModal: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenTimerModal() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (timerState.isRunning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (timerState.isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = if (timerState.isRunning) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (timerState.isRunning) timerState.modeName else "專注計時器 (番茄鐘 & 大考擬真)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val min = timerState.remainingSeconds / 60
                    val sec = timerState.remainingSeconds % 60
                    Text(
                        text = if (timerState.isRunning)
                            "倒數中: ${String.format("%02d:%02d", min, sec)} | ${timerState.ambientSound}"
                        else
                            "25分番茄鐘 / 45分大考 / 5分啟動法",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (timerState.isRunning) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onTogglePause,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (timerState.isPaused) "繼續" else "暫停", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onStop,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("結束", fontSize = 11.sp)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onOpenTimerModal,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("開啟計時", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TaskFilterSection(
    selectedPriority: Priority?,
    onSelectPriority: (Priority?) -> Unit,
    selectedSubject: String?,
    onSelectSubject: (String?) -> Unit,
    showOnlyIncomplete: Boolean,
    onToggleIncomplete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val subjects = listOf("全部科目", "國文", "英文", "數學", "自然", "社會", "生活", "大考複習")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(subjects) { subj ->
                val isSelected = (subj == "全部科目" && selectedSubject == null) || (subj == selectedSubject)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSelectSubject(if (subj == "全部科目") null else subj)
                    },
                    label = { Text(subj, fontSize = 12.sp) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                item {
                    FilterChip(
                        selected = selectedPriority == null,
                        onClick = { onSelectPriority(null) },
                        label = { Text("所有等級", fontSize = 11.sp) }
                    )
                }
                items(Priority.values()) { p ->
                    FilterChip(
                        selected = selectedPriority == p,
                        onClick = { onSelectPriority(p) },
                        label = { Text(p.label, fontSize = 11.sp) }
                    )
                }
            }
            FilterChip(
                selected = showOnlyIncomplete,
                onClick = onToggleIncomplete,
                label = { Text(if (showOnlyIncomplete) "僅未完成" else "含已完成", fontSize = 11.sp) }
            )
        }
    }
}

@Composable
fun TaskCard(
    task: TodoTask,
    onToggleComplete: () -> Unit,
    onToggleSubTask: (String) -> Unit,
    onAddSubTaskClick: () -> Unit,
    onDeleteSubTask: (String) -> Unit,
    onStartTimer: () -> Unit,
    onOpenProcrastinationBuster: () -> Unit,
    onAdvanceMicroStep: () -> Unit,
    onUpdateReminderStage: (ReminderStage) -> Unit,
    onDelete: () -> Unit
) {
    var expandedStages by remember { mutableStateOf(false) }
    var expandedSubTasks by remember { mutableStateOf(true) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        contentDescription = "Toggle Complete",
                        tint = if (task.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(task.priority.badgeColorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = task.subjectTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(task.priority.badgeColorHex),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = task.priority.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        // Stepped reminder badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (task.reminderStage) {
                                ReminderStage.GENTLE -> MaterialTheme.colorScheme.secondaryContainer
                                ReminderStage.FOCUS -> MaterialTheme.colorScheme.tertiaryContainer
                                ReminderStage.OVERDUE -> DangerRed.copy(alpha = 0.15f)
                            },
                            modifier = Modifier.clickable { expandedStages = !expandedStages }
                        ) {
                            Text(
                                text = "🔔 ${task.reminderStage.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = when (task.reminderStage) {
                                    ReminderStage.GENTLE -> MaterialTheme.colorScheme.onSecondaryContainer
                                    ReminderStage.FOCUS -> MaterialTheme.colorScheme.onTertiaryContainer
                                    ReminderStage.OVERDUE -> DangerRed
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (task.dueDate.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "預計期限：${task.dueDate} ${task.dueTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Stepped Stage dropdown/toggle selector
            AnimatedVisibility(visible = expandedStages) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "切換階梯提醒狀態：",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReminderStage.values().forEach { stage ->
                                FilterChip(
                                    selected = task.reminderStage == stage,
                                    onClick = {
                                        onUpdateReminderStage(stage)
                                        expandedStages = false
                                    },
                                    label = { Text(stage.label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Independent SubTasks Section with Progress
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { expandedSubTasks = !expandedSubTasks }
                        ) {
                            Icon(
                                imageVector = if (expandedSubTasks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "子任務進度 (${task.completedSubTasksCount}/${task.totalSubTasksCount} · ${task.progressPercent}%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "+ 新增子步驟",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onAddSubTaskClick() }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { task.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (task.progressPercent == 100) SuccessGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    AnimatedVisibility(visible = expandedSubTasks && task.subTasks.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            task.subTasks.forEach { subTask ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onToggleSubTask(subTask.id) }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = subTask.isCompleted,
                                        onCheckedChange = { onToggleSubTask(subTask.id) },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = subTask.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null,
                                        color = if (subTask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDeleteSubTask(subTask.id) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete Subtask",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Micro-steps progress (5-minute procrastination buster)
            if (task.microSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarningOrange.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ 5分鐘抗拖延微步 (${task.currentMicroStepIndex}/${task.microSteps.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarningOrange
                            )
                            if (task.currentMicroStepIndex < task.microSteps.size) {
                                Text(
                                    text = "完成此微步 ➔",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarningOrange,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onAdvanceMicroStep() }
                                )
                            }
                        }
                        if (task.currentMicroStepIndex < task.microSteps.size) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "當前微步：${task.microSteps[task.currentMicroStepIndex]}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenProcrastinationBuster,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Buster",
                        tint = WarningOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("抗拖延微步", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Button(
                    onClick = onStartTimer,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Focus", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("開始專注 (${task.targetMinutes}分)", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyTaskStateCard(onAddTask: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Empty",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "目前沒有相符的待辦事項",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "太棒了！或者點擊下方按鈕新增一項學業與生活目標",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("新增待辦任務")
            }
        }
    }
}

// Dialogs: Add Task, Add Subtask, Procrastination Buster, and Focus Timer
@Composable
fun AddTaskDialog(
    isAiLoading: Boolean,
    onDismiss: () -> Unit,
    onDecomposeWithAi: (String, String, (List<String>) -> Unit) -> Unit,
    onSave: (String, String, String, Priority, Int, List<String>, List<SubTask>, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("數學") }
    var priority by remember { mutableStateOf(Priority.HIGH_URGENT) }
    var targetMin by remember { mutableIntStateOf(25) }
    var microStep1 by remember { mutableStateOf("") }
    var microStep2 by remember { mutableStateOf("") }
    var microStep3 by remember { mutableStateOf("") }

    val subjectOptions = listOf("數學", "英文", "國文", "自然", "社會", "生活", "大考複習")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("新增學習或生活待辦", fontWeight = FontWeight.Bold)
                if (isAiLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("任務標題 (例如：物理力學講義P.30)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // AI Decomposition Button
                item {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onDecomposeWithAi(title, subject) { steps ->
                                    if (steps.isNotEmpty()) microStep1 = steps.getOrNull(0) ?: ""
                                    if (steps.size > 1) microStep2 = steps.getOrNull(1) ?: ""
                                    if (steps.size > 2) microStep3 = steps.getOrNull(2) ?: ""
                                }
                            }
                        },
                        enabled = title.isNotBlank() && !isAiLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✨ AI 一鍵智慧微步拆解 (Gemini)", fontSize = 12.sp)
                    }
                }

                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("備註 / 具體範圍") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(text = "學科 / 分類：", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjectOptions) { s ->
                            FilterChip(
                                selected = subject == s,
                                onClick = { subject = s },
                                label = { Text(s, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    Text(text = "優先級 (四象限)：", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Priority.values().take(3).forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    Text(text = "預估專注時間：$targetMin 分鐘", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            FilterChip(
                                selected = targetMin == mins,
                                onClick = { targetMin = mins },
                                label = { Text("${mins}分", fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = "⚡ 5分鐘微步拆解 (克服拖延抗拒感)：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    OutlinedTextField(
                        value = microStep1,
                        onValueChange = { microStep1 = it },
                        label = { Text("微步 1 (例如：翻開課本找到範例一)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = microStep2,
                        onValueChange = { microStep2 = it },
                        label = { Text("微步 2 (例如：動筆算第1小題)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = microStep3,
                        onValueChange = { microStep3 = it },
                        label = { Text("微步 3 (例如：對答案並標註觀念)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val steps = listOf(microStep1, microStep2, microStep3).filter { it.isNotBlank() }
                        // Automatically initialize corresponding subtasks if microsteps are provided
                        val subTasks = steps.map { SubTask(title = it, isCompleted = false) }
                        onSave(title, desc, subject, priority, targetMin, steps, subTasks, "", "")
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("建立待辦")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AddSubTaskDialog(
    taskTitle: String,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var subTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增子任務步驟", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "父任務：$taskTitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = subTitle,
                    onValueChange = { subTitle = it },
                    label = { Text("子任務內容 (例如：計算第3題、整理錯題)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (subTitle.isNotBlank()) onAdd(subTitle) },
                enabled = subTitle.isNotBlank()
            ) {
                Text("新增")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ProcrastinationBusterDialog(
    task: TodoTask,
    isAiLoading: Boolean,
    onDismiss: () -> Unit,
    onAiDecompose: () -> Unit,
    onAdvanceMicroStep: () -> Unit,
    onStart5MinSession: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Buster",
                    tint = WarningOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("5分鐘微步突破拖延", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarningOrange.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "心理學「蔡加尼克效應」啟示：",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "大腦抗拒的不是學習本身，而是「開始」那一瞬間的巨大心理負擔。只要騙大腦『我只做5分鐘』，一旦開始，大腦就會渴望將其完成！",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }

                Text(
                    text = "當前任務：${task.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                if (task.microSteps.isNotEmpty()) {
                    Text(
                        text = "拆解後的微步清單：",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    task.microSteps.forEachIndexed { index, step ->
                        val isDone = index < task.currentMicroStepIndex
                        val isCurrent = index == task.currentMicroStepIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    if (isCurrent) onAdvanceMicroStep()
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = if (isDone) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Text(
                        text = "建議第一步：把手機翻面放遠，喝一口水，打開講義只看第一小題。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // AI Re-decompose option
                OutlinedButton(
                    onClick = onAiDecompose,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isAiLoading
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isAiLoading) "AI 正在重新微步拆解..." else "✨ AI 重新拆解為更小步驟", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStart5MinSession,
                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
            ) {
                Text("🔥 立即啟動 5 分鐘計時")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("我知道了")
            }
        }
    )
}

@Composable
fun FocusTimerDialog(
    timerState: com.example.viewmodel.FocusTimerUiState,
    onDismiss: () -> Unit,
    onStartNewSession: (Int, String) -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    onSelectAmbientSound: (String) -> Unit
) {
    val sounds = listOf("白噪音 (雨聲)", "寧靜自習室", "森林微風", "靜音")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(timerState.modeName, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!timerState.activeTaskTitle.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "當前任務：${timerState.activeTaskTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }

                // Giant Timer Display
                val min = timerState.remainingSeconds / 60
                val sec = timerState.remainingSeconds % 60
                Text(
                    text = String.format("%02d:%02d", min, sec),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )

                // Timer Controls
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onTogglePause,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (timerState.isPaused) "繼續專注" else "暫停")
                    }
                    Button(
                        onClick = onStop,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("提早結束")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Fast mode switches
                Text(text = "切換專注模式：", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = timerState.totalSeconds == 25 * 60,
                        onClick = { onStartNewSession(25, "標準番茄鐘 (25分)") },
                        label = { Text("25分番茄", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = timerState.totalSeconds == 45 * 60,
                        onClick = { onStartNewSession(45, "大考擬真模式 (45分)") },
                        label = { Text("45分大考", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = timerState.totalSeconds == 5 * 60,
                        onClick = { onStartNewSession(5, "5分鐘微步抗拖延") },
                        label = { Text("5分抗拖延", fontSize = 11.sp) }
                    )
                }

                // Ambient Sound selector
                Text(text = "環境專注音效意境：", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sounds) { s ->
                        FilterChip(
                            selected = timerState.ambientSound == s,
                            onClick = { onSelectAmbientSound(s) },
                            label = { Text(s, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
