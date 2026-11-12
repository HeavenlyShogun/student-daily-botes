package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.EisenhowerQuadrantData
import com.example.data.engine.MaslowLevel
import com.example.data.engine.MaslowPyramidAssessment
import com.example.data.engine.MaslowTierStatus
import com.example.data.model.Priority
import com.example.data.model.TodoTask
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange

/**
 * 馬斯洛生存與自我實現需求金字塔互動卡片
 * 結合學生每日生理作息（睡眠、飲水、自我菜單三餐、運動）、排程安全感、正向感恩日記與待辦達成度
 */
@Composable
fun MaslowNeedPyramidCard(
    assessment: MaslowPyramidAssessment,
    onNavigateToHealth: (() -> Unit)? = null,
    onAddTaskForPriority: ((Priority) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTier by remember { mutableStateOf<MaslowLevel?>(assessment.bottleneckTier.level) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("maslow_pyramid_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Maslow Pyramid",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "馬斯洛生存與自我實現需求金字塔",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "先穩固底層生存能量，再衝刺頂層學業自我實現",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "金字塔指數 ${assessment.overallFoundationScore}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual Stepped Pyramid (Top Tier 5 narrowest -> Bottom Tier 1 widest)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                assessment.tiersTopToBottom.forEachIndexed { index, tierStatus ->
                    val widthFraction = when (tierStatus.level.tierNumber) {
                        5 -> 0.62f
                        4 -> 0.72f
                        3 -> 0.82f
                        2 -> 0.91f
                        else -> 1.0f
                    }
                    val tierColor = Color(tierStatus.level.colorHex)
                    val isSelected = selectedTier == tierStatus.level
                    val isBottleneck = assessment.bottleneckTier.level == tierStatus.level

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(widthFraction)
                            .clip(
                                RoundedCornerShape(
                                    topStart = if (index == 0) 16.dp else 8.dp,
                                    topEnd = if (index == 0) 16.dp else 8.dp,
                                    bottomStart = if (index == 4) 14.dp else 8.dp,
                                    bottomEnd = if (index == 4) 14.dp else 8.dp
                                )
                            )
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        tierColor.copy(alpha = if (isSelected) 0.24f else 0.14f),
                                        tierColor.copy(alpha = if (isSelected) 0.34f else 0.20f)
                                    )
                                )
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) tierColor else tierColor.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(
                                    topStart = if (index == 0) 16.dp else 8.dp,
                                    topEnd = if (index == 0) 16.dp else 8.dp,
                                    bottomStart = if (index == 4) 14.dp else 8.dp,
                                    bottomEnd = if (index == 4) 14.dp else 8.dp
                                )
                            )
                            .clickable {
                                selectedTier = if (selectedTier == tierStatus.level) null else tierStatus.level
                            }
                            .testTag("maslow_tier_${tierStatus.level.tierNumber}")
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = tierColor,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "L${tierStatus.level.tierNumber}",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tierStatus.level.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isBottleneck) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "⚡優先",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tierColor
                                    )
                                }
                            }
                            Text(
                                text = "${tierStatus.scorePercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = tierColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expanded Details for Selected Tier
            val activeTierStatus: MaslowTierStatus = assessment.tiersTopToBottom.find { it.level == selectedTier }
                ?: assessment.bottleneckTier
            val activeColor = Color(activeTierStatus.level.colorHex)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = activeColor.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "【第 ${activeTierStatus.level.tierNumber} 層】${activeTierStatus.level.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = activeColor
                            )
                            Text(
                                text = activeTierStatus.level.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = activeColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "${activeTierStatus.statusLabel} (${activeTierStatus.scorePercent}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = activeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { activeTierStatus.scorePercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = activeColor,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "📊 當前狀態：${activeTierStatus.summaryText}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = activeColor,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeTierStatus.actionableAdvice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }

                    if (activeTierStatus.relatedTasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "對應此需求階層的待辦 (${activeTierStatus.relatedTasks.size} 項)：",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        activeTierStatus.relatedTasks.take(3).forEach { task ->
                            Text(
                                text = "• [${task.subjectTag}] ${task.title} ${if (task.isCompleted) "(✓已完成)" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quick Action Buttons
                    if (onNavigateToHealth != null || onAddTaskForPriority != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (activeTierStatus.level == MaslowLevel.PHYSIOLOGICAL_SURVIVAL && onNavigateToHealth != null) {
                                Button(
                                    onClick = onNavigateToHealth,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("前往飲食自我菜單 / 補水作息", fontSize = 11.sp)
                                }
                            }
                            if (onAddTaskForPriority != null) {
                                val suggestedPriority = when (activeTierStatus.level) {
                                    MaslowLevel.SELF_ACTUALIZATION -> Priority.HIGH_NOT_URGENT
                                    MaslowLevel.ESTEEM_MASTERY -> Priority.HIGH_URGENT
                                    else -> Priority.NORMAL
                                }
                                OutlinedButton(
                                    onClick = { onAddTaskForPriority(suggestedPriority) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("新增此階層目標", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 艾森豪「緊急與重要四象限規劃表」互動面板
 * 提供 2x2 視覺矩陣總覽 + 各象限任務執行、快速轉移象限與直接新增象限任務
 */
@Composable
fun EisenhowerMatrixBoard(
    quadrants: List<EisenhowerQuadrantData>,
    onToggleComplete: (TodoTask) -> Unit,
    onStartTimer: (TodoTask) -> Unit,
    onMovePriority: (TodoTask, Priority) -> Unit,
    onAddTaskToQuadrant: (Priority) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalActiveMins = quadrants.sumOf { it.totalTargetMinutes }.coerceAtLeast(1)
    val q2Mins = quadrants.find { it.priority == Priority.HIGH_NOT_URGENT }?.totalTargetMinutes ?: 0
    val q2RatioPercent = (q2Mins * 100) / totalActiveMins

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("eisenhower_matrix_board"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Matrix Overview Header & 2x2 Mini Grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Four Quadrants",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "艾森豪「緊急與重要」四象限規劃表",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "區分「急迫感」與「真正價值」，擺脫每天被死線追著跑的焦慮",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 Visual Matrix Grid
                val q1 = quadrants.getOrNull(0)
                val q2 = quadrants.getOrNull(1)
                val q3 = quadrants.getOrNull(2)
                val q4 = quadrants.getOrNull(3)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (q1 != null) {
                        MiniQuadrantSummaryCell(
                            quadrant = q1,
                            modifier = Modifier.weight(1f),
                            onAddClick = { onAddTaskToQuadrant(q1.priority) }
                        )
                    }
                    if (q2 != null) {
                        MiniQuadrantSummaryCell(
                            quadrant = q2,
                            modifier = Modifier.weight(1f),
                            onAddClick = { onAddTaskToQuadrant(q2.priority) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (q3 != null) {
                        MiniQuadrantSummaryCell(
                            quadrant = q3,
                            modifier = Modifier.weight(1f),
                            onAddClick = { onAddTaskToQuadrant(q3.priority) }
                        )
                    }
                    if (q4 != null) {
                        MiniQuadrantSummaryCell(
                            quadrant = q4,
                            modifier = Modifier.weight(1f),
                            onAddClick = { onAddTaskToQuadrant(q4.priority) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secretary Strategy Diagnosis
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (q2Mins == 0)
                            "💡 秘書四象限診斷：目前「第二象限（重要不緊急）」尚無排入長期深耕任務！建議每天保留 25 分鐘給大考觀念或錯題複習，才不會考前全部擠進第一象限。"
                        else
                            "💡 秘書四象限診斷：您的「第二象限（重要不緊急）」佔今日待辦時間 $q2RatioPercent%（共 ${q2Mins} 分鐘），兼顧了眼前進度與長遠實力累積！",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Detailed Quadrant Action Cards
        quadrants.forEach { quadrant ->
            QuadrantDetailCard(
                quadrant = quadrant,
                onToggleComplete = onToggleComplete,
                onStartTimer = onStartTimer,
                onMovePriority = onMovePriority,
                onAddTask = { onAddTaskToQuadrant(quadrant.priority) }
            )
        }
    }
}

@Composable
private fun MiniQuadrantSummaryCell(
    quadrant: EisenhowerQuadrantData,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = Color(quadrant.colorHex)
    val activeCount = quadrant.tasks.count { !it.isCompleted }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = badgeColor.copy(alpha = 0.12f),
        modifier = modifier
            .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quadrant.quadrantCode,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = badgeColor
                )
                Text(
                    text = "+ 新增",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier
                        .clickable { onAddClick() }
                        .testTag("quadrant_add_${quadrant.priority.name}")
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = quadrant.priority.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "待完成 $activeCount 項 · ${quadrant.totalTargetMinutes} 分鐘",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun QuadrantDetailCard(
    quadrant: EisenhowerQuadrantData,
    onToggleComplete: (TodoTask) -> Unit,
    onStartTimer: (TodoTask) -> Unit,
    onMovePriority: (TodoTask, Priority) -> Unit,
    onAddTask: () -> Unit
) {
    val color = Color(quadrant.colorHex)
    var expandedMoveTaskId by remember { mutableStateOf<Long?>(null) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quadrant_card_${quadrant.priority.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = quadrant.quadrantCode,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = quadrant.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = quadrant.strategySubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = onAddTask,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("排入此象限", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "📌 策略：${quadrant.actionGuide}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (quadrant.tasks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "目前此象限沒有任務，點擊右上角「+ 排入此象限」即可新增",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quadrant.tasks.forEach { task ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (task.isCompleted)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else
                                color.copy(alpha = 0.06f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onToggleComplete(task) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                            contentDescription = "Complete",
                                            tint = if (task.isCompleted) SuccessGreen else color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "[${task.subjectTag}] ${task.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                                        )
                                        val slotStr = if (task.scheduledSlotStart.isNotBlank()) {
                                            "建議時段 ${task.scheduledSlotStart}-${task.scheduledSlotEnd} · "
                                        } else ""
                                        Text(
                                            text = "${slotStr}預估 ${task.targetMinutes} 分 · 期限 ${task.dueTime}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            expandedMoveTaskId = if (expandedMoveTaskId == task.id) null else task.id
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Move Quadrant",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (!task.isCompleted) {
                                        IconButton(
                                            onClick = { onStartTimer(task) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Start Focus",
                                                tint = color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = expandedMoveTaskId == task.id) {
                                    Column(modifier = Modifier.padding(top = 6.dp)) {
                                        Text(
                                            text = "快速移動至其他象限：",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(Priority.values()) { targetPriority ->
                                                FilterChip(
                                                    selected = task.priority == targetPriority,
                                                    onClick = {
                                                        if (task.priority != targetPriority) {
                                                            onMovePriority(task, targetPriority)
                                                        }
                                                        expandedMoveTaskId = null
                                                    },
                                                    label = { Text(targetPriority.label, fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
