package com.example.ui.review

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayCompletionStat
import com.example.data.model.TodoTask
import com.example.ui.theme.ExerciseEmerald
import com.example.ui.theme.SleepPurple
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.viewmodel.MainViewModel

@Composable
fun WeeklyReviewScreen(
    viewModel: MainViewModel,
    onStartFocus: (TodoTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val weeklyMetrics by viewModel.weeklyMetrics.collectAsState()
    var selectedBufferMinutes by remember { mutableIntStateOf(10) }
    var selectedBedtime by remember { mutableStateOf("23:00") }

    LaunchedEffect(Unit) {
        viewModel.refreshWeeklyMetrics()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("weekly_review_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "本週學習與作息回顧",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "看見累積的微小進步 · 從數據中調整節律",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.refreshWeeklyMetrics() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Metrics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Executive Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = "Insights",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "秘書週診斷總評",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "近 7 日綜合執行率與作息健康",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = weeklyMetrics.executiveSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Advice",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = weeklyMetrics.adviceAction,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Weekly Core KPI Highlights
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "待辦完成率",
                    value = "${weeklyMetrics.completionRatePercent}%",
                    sub = "${weeklyMetrics.completedTasksCount}/${weeklyMetrics.totalTasksCount} 項完成",
                    color = MaterialTheme.colorScheme.primary
                )
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "平均睡眠",
                    value = "${String.format("%.1f", weeklyMetrics.avgSleepHours)}h",
                    sub = "記錄 ${weeklyMetrics.loggedHealthDays} 天",
                    color = SleepPurple
                )
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "累計運動",
                    value = "${weeklyMetrics.totalExerciseMinutes}m",
                    sub = "課間舒緩伸展",
                    color = ExerciseEmerald
                )
            }
        }

        // 7-Day Day-by-Day Matrix (Distinguishing Unlogged vs Incomplete)
        item {
            Text(
                text = "近 7 日每日達成狀態 (精準分辨「未記錄」與「零進度」)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(weeklyMetrics.dailyStats) { stat ->
            DayStatRow(stat = stat)
        }

        // Recurring Delayed Tasks Warning (postponeCount >= 2)
        if (weeklyMetrics.recurringDelayedTasks.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WarningOrange.copy(alpha = 0.12f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = WarningOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠️ 反覆延期警示 (${weeklyMetrics.recurringDelayedTasks.size} 項卡關)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarningOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "心理學指出：任務反覆被推遲，通常是因為目標過大引發畏難情緒。建議直接使用「5分鐘微步拆解」，只做前5分鐘就能打破拖延僵局！",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        weeklyMetrics.recurringDelayedTasks.forEach { task ->
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "【${task.subjectTag}】${task.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "已延期 ${task.postponeCount} 次 · 預估 ${task.targetMinutes} 分鐘",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WarningOrange
                                        )
                                    }
                                    Button(
                                        onClick = { onStartFocus(task) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("5分微步", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Personalization & Tuning Preferences
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tune",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "個人化作息與抗拖延參數調整",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "任務間防疲勞緩衝時間：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5 to "5 分鐘 (緊湊)", 10 to "10 分鐘 (推薦)", 15 to "15 分鐘 (充裕)").forEach { (mins, label) ->
                            val isSelected = selectedBufferMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedBufferMinutes = mins }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "每日黃金睡眠防線就寢時間：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("22:30", "23:00", "23:30").forEach { time ->
                            val isSelected = selectedBedtime == time
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) SleepPurple else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedBedtime = time
                                        viewModel.logSleep(7.5f, time, "06:45")
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (time == "23:00") "$time (黃金)" else time,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DayStatRow(stat: DayCompletionStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Day label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stat.dayOfWeekLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stat.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (stat.totalTasks > 0) "${stat.completedTasks}/${stat.totalTasks} 項完成" else "無待辦安排",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (stat.totalTasks > 0 && stat.completedTasks == stat.totalTasks) SuccessGreen else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Health status chips (Carefully distinguishing "未記錄" from "0分")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Sleep chip
                if (stat.isSleepLogged && stat.sleepHours != null) {
                    StatusChip(
                        icon = Icons.Default.Bedtime,
                        text = "${String.format("%.1f", stat.sleepHours)}h",
                        color = SleepPurple
                    )
                } else {
                    UnloggedChip(label = "睡眠未記")
                }

                // Exercise chip
                if (stat.isExerciseLogged) {
                    StatusChip(
                        icon = Icons.Default.FitnessCenter,
                        text = "${stat.exerciseMinutes}m",
                        color = ExerciseEmerald
                    )
                } else {
                    UnloggedChip(label = "運動未記")
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun UnloggedChip(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            fontSize = 10.sp
        )
    }
}
