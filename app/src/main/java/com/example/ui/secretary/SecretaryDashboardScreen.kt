package com.example.ui.secretary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyHealthLog
import com.example.data.model.DailyJournalEntry
import com.example.data.model.TodoTask
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ExerciseEmerald
import com.example.ui.theme.SleepPurple
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WaterCyan
import com.example.viewmodel.MainViewModel

@Composable
fun SecretaryDashboardScreen(
    viewModel: MainViewModel,
    onNavigateToHealth: () -> Unit,
    onNavigateToTodo: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onStartFocus: (TodoTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val briefing = viewModel.getSecretaryGreeting()
    val healthLog by remember { mutableStateOf<DailyHealthLog?>(null) }
    // Collect from viewModel flows in parent or directly
    val currentHealth = viewModel.todayHealth.value ?: DailyHealthLog(date = "")
    val activeTasks = viewModel.activeTasks.value
    val journalEntry = viewModel.todayJournal.value
    val timerState = viewModel.timerState.value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Secretary Greeting Hero Card
            SecretaryHeroCard(
                briefingTitle = briefing.title,
                briefingQuote = briefing.quote,
                briefingAdvice = briefing.advice,
                iconType = briefing.iconType
            )
        }

        // Timer Active Indicator if running
        if (timerState.isRunning) {
            item {
                ActiveTimerBanner(
                    timerState = timerState,
                    onStop = { viewModel.stopTimer() },
                    onTogglePause = { viewModel.togglePauseTimer() }
                )
            }
        }

        // Today's Life Balance & Health Radar Score Card
        item {
            LifeBalanceOverviewCard(
                healthLog = currentHealth,
                completedTasksCount = viewModel.allTasks.value.count { it.isCompleted },
                totalTasksCount = viewModel.allTasks.value.size,
                hasJournaled = journalEntry?.goodThing1?.isNotBlank() == true,
                onViewDetails = onNavigateToHealth
            )
        }

        // Sleep Debt Alert (if sleep debt > 0.5 hours or late night warning)
        if (currentHealth.sleepDebtHours > 0.5f || currentHealth.isLateNightWarningTriggered) {
            item {
                SleepAlertCard(
                    sleepDebtHours = currentHealth.sleepDebtHours,
                    isLateNight = currentHealth.isLateNightWarningTriggered,
                    onOpenSleepPlanner = onNavigateToHealth
                )
            }
        }

        // Quick Daily Health Actions (1-tap logging)
        item {
            QuickHealthCheckSection(
                healthLog = currentHealth,
                onAddWater = { viewModel.addWater(250) },
                onToggleMeal = { viewModel.toggleMeal(it) },
                onToggleWindDown = { viewModel.toggleScreenWindDown() },
                onGoToHealth = onNavigateToHealth
            )
        }

        // Priority Academic Tasks Section
        item {
            PriorityTasksSection(
                tasks = activeTasks.take(3),
                onStartFocus = onStartFocus,
                onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                onViewAll = onNavigateToTodo
            )
        }

        // Daily Reflection & Positive Journal Snapshot
        item {
            JournalSnapshotCard(
                entry = journalEntry,
                onOpenJournal = onNavigateToJournal
            )
        }

        // Neuroscience & High School Study Tips Card
        item {
            ScienceStudyTipCard()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SecretaryHeroCard(
    briefingTitle: String,
    briefingQuote: String,
    briefingAdvice: String,
    iconType: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("secretary_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (iconType) {
                        "MORNING" -> Icons.Default.WbSunny
                        "NOON" -> Icons.Default.Restaurant
                        "AFTERNOON" -> Icons.Default.SelfImprovement
                        "EVENING" -> Icons.Default.Schedule
                        else -> Icons.Default.Bedtime
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Secretary Icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "學生專屬秘書",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = briefingTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = briefingQuote,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = briefingAdvice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveTimerBanner(
    timerState: com.example.viewmodel.FocusTimerUiState,
    onStop: () -> Unit,
    onTogglePause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_timer_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🔥 專注進行中：${timerState.modeName}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                if (!timerState.activeTaskTitle.isNullOrBlank()) {
                    Text(
                        text = timerState.activeTaskTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1
                    )
                }
                val min = timerState.remainingSeconds / 60
                val sec = timerState.remainingSeconds % 60
                Text(
                    text = String.format("%02d:%02d", min, sec),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTogglePause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(if (timerState.isPaused) "繼續" else "暫停")
                }
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("結束")
                }
            }
        }
    }
}

@Composable
fun LifeBalanceOverviewCard(
    healthLog: DailyHealthLog,
    completedTasksCount: Int,
    totalTasksCount: Int,
    hasJournaled: Boolean,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("life_balance_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "今日生活自律指數",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "健康三大主軸 × 學習執行力",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "詳細規劃 →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onViewDetails() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Animated Circular Progress Gauge
                val overallScore = healthLog.healthScore
                val animatedProgress by animateFloatAsState(
                    targetValue = overallScore / 100f,
                    animationSpec = tween(durationMillis = 800),
                    label = "ScoreProgress"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    Canvas(modifier = Modifier.size(100.dp)) {
                        drawCircle(
                            color = trackColor,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$overallScore",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "健康分",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Balance Pillars Breakdown
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    PillarIndicator(
                        icon = Icons.Default.Bedtime,
                        iconColor = SleepPurple,
                        label = "黃金睡眠",
                        value = "${healthLog.sleepHours}h / ${healthLog.targetSleepHours}h"
                    )
                    PillarIndicator(
                        icon = Icons.Default.LocalDrink,
                        iconColor = WaterCyan,
                        label = "補水紀錄",
                        value = "${healthLog.waterMl} / ${healthLog.targetWaterMl} ml"
                    )
                    PillarIndicator(
                        icon = Icons.Default.FitnessCenter,
                        iconColor = ExerciseEmerald,
                        label = "課間運動",
                        value = "${healthLog.exerciseMinutes} / ${healthLog.targetExerciseMinutes} 分"
                    )
                    PillarIndicator(
                        icon = Icons.Default.CheckCircle,
                        iconColor = MaterialTheme.colorScheme.primary,
                        label = "待辦完成",
                        value = "$completedTasksCount / $totalTasksCount 項"
                    )
                }
            }
        }
    }
}

@Composable
fun PillarIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SleepAlertCard(
    sleepDebtHours: Float,
    isLateNight: Boolean,
    onOpenSleepPlanner: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sleep_alert_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = WarningOrange.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alert",
                tint = WarningOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLateNight) "⚠️ 熬夜警示：睡眠節律偏移" else "⏳ 累積睡眠赤字：${String.format("%.1f", sleepDebtHours)} 小時",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = WarningOrange
                )
                Text(
                    text = "醫學建議：切勿報復性熬夜！請於今晚提早半小時入睡，維護專注力與免疫力。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onOpenSleepPlanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarningOrange
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("調作息", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun QuickHealthCheckSection(
    healthLog: DailyHealthLog,
    onAddWater: () -> Unit,
    onToggleMeal: (String) -> Unit,
    onToggleWindDown: () -> Unit,
    onGoToHealth: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日健康快捷打卡",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "健康細節 ➔",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onGoToHealth() }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Water Quick Add Chip
            QuickActionChip(
                modifier = Modifier.weight(1f),
                title = "+250ml 水",
                subtitle = "${healthLog.waterMl}ml",
                icon = Icons.Default.LocalDrink,
                color = WaterCyan,
                onClick = onAddWater
            )
            // Breakfast
            QuickToggleChip(
                modifier = Modifier.weight(1f),
                title = "早餐",
                isDone = healthLog.breakfastLogged,
                icon = Icons.Default.Restaurant,
                onClick = { onToggleMeal("breakfast") }
            )
            // Lunch
            QuickToggleChip(
                modifier = Modifier.weight(1f),
                title = "午餐",
                isDone = healthLog.lunchLogged,
                icon = Icons.Default.Restaurant,
                onClick = { onToggleMeal("lunch") }
            )
            // Wind down
            QuickToggleChip(
                modifier = Modifier.weight(1f),
                title = "數位排毒",
                isDone = healthLog.screenWindDownDone,
                icon = Icons.Default.Bedtime,
                onClick = onToggleWindDown
            )
        }
    }
}

@Composable
fun QuickActionChip(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickToggleChip(
    title: String,
    isDone: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else icon,
                contentDescription = title,
                tint = if (isDone) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
                color = if (isDone) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isDone) "已打卡" else "待打卡",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (isDone) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PriorityTasksSection(
    tasks: List<TodoTask>,
    onStartFocus: (TodoTask) -> Unit,
    onToggleComplete: (TodoTask) -> Unit,
    onViewAll: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日核心待辦 (動態階梯提醒)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "全部清單 ➔",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onViewAll() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎉 今日待辦皆已完成！身心放鬆，享受夜晚時光",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessGreen
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
                    DashboardTaskItem(
                        task = task,
                        onStartFocus = { onStartFocus(task) },
                        onToggleComplete = { onToggleComplete(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardTaskItem(
    task: TodoTask,
    onStartFocus: () -> Unit,
    onToggleComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_task_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Schedule,
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
                    Text(
                        text = task.reminderStage.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Button(
                onClick = onStartFocus,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Focus",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "專注", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun JournalSnapshotCard(
    entry: DailyJournalEntry?,
    onOpenJournal: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("journal_snapshot_card")
            .clickable { onOpenJournal() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Journal",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "每日正向記事與反思手帳",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = if (entry?.goodThing1?.isNotBlank() == true) "已撰寫 ✏️" else "去記錄 ➔",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (entry != null && entry.goodThing1.isNotBlank()) {
                Text(
                    text = "今日小確幸：${entry.goodThing1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2
                )
            } else {
                Text(
                    text = "記錄今日微小成就與3件好事，透過正向自我對話內化自律意識。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ScienceStudyTipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Tips",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "科學家的高中生學習記憶密碼",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "大腦的神經可塑性在深層睡眠中最強烈！白天輸入的數學公式與歷史時間軸，唯有在睡眠循環中才會由暫存區移往永久皮質。不要用縮短睡眠換取複習時間，那是在做負功！",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
