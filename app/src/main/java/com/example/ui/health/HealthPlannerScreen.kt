package com.example.ui.health

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.model.DailyHealthLog
import com.example.data.model.DefaultPresets
import com.example.data.model.RoutinePreset
import com.example.data.model.ScheduleBlock
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ExerciseEmerald
import com.example.ui.theme.SleepPurple
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WaterCyan
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthPlannerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentHealth = viewModel.todayHealth.value ?: DailyHealthLog(date = "")
    val activePreset = viewModel.activePreset.value

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("睡眠與作息", "飲食與水分", "運動與伸展", "模組作息表")

    // Sleep logging dialog
    var showSleepDialog by remember { mutableStateOf(false) }

    // Exercise logging dialog
    var showExerciseDialog by remember { mutableStateOf(false) }

    // Interactive stretch guide modal
    var activeStretchGuide by remember { mutableStateOf<StretchRoutine?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("health_planner_screen")
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Sleep & Circadian rhythm
                    item {
                        SleepSectionCard(
                            healthLog = currentHealth,
                            onOpenLogDialog = { showSleepDialog = true },
                            onToggleWindDown = { viewModel.toggleScreenWindDown() }
                        )
                    }
                    item {
                        SleepDebtExplainerCard(sleepDebtHours = currentHealth.sleepDebtHours)
                    }
                    item {
                        CircadianNeuroscienceCard()
                    }
                }
                1 -> {
                    // Diet & Hydration
                    item {
                        WaterIntakeTrackerCard(
                            currentMl = currentHealth.waterMl,
                            targetMl = currentHealth.targetWaterMl,
                            onAddWater = { viewModel.addWater(it) },
                            onResetWater = { viewModel.resetWater() }
                        )
                    }
                    item {
                        ThreeMealsChecklistCard(
                            healthLog = currentHealth,
                            onToggleMeal = { viewModel.toggleMeal(it) }
                        )
                    }
                    item {
                        BrainNutritionalTipsCard()
                    }
                }
                2 -> {
                    // Exercise & Study break stretches
                    item {
                        ExerciseTrackerCard(
                            exerciseMinutes = currentHealth.exerciseMinutes,
                            targetMinutes = currentHealth.targetExerciseMinutes,
                            exerciseType = currentHealth.exerciseType,
                            onOpenExerciseDialog = { showExerciseDialog = true }
                        )
                    }
                    item {
                        Text(
                            text = "4大課間久坐放鬆伸展 (點擊啟動倒數引導)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        StretchRoutineCard(
                            title = "肩頸斜方肌放鬆 (低頭救星)",
                            durationMinutes = 3,
                            icon = Icons.Default.SelfImprovement,
                            description = "雙手置於後腦勺，緩慢低頭拉伸後頸部，隨後聳肩旋轉，釋放讀書肌肉張力。",
                            color = ExerciseEmerald,
                            onStart = {
                                activeStretchGuide = StretchRoutine(
                                    "肩頸斜方肌放鬆",
                                    180,
                                    "雙手交扣置於後腦勺，輕柔低頭向下維持15秒；接著將頭部分別向左肩、右肩側傾各15秒；最後雙肩向上聳起至最高點，再向後打圈旋轉5次。"
                                )
                            }
                        )
                    }
                    item {
                        StretchRoutineCard(
                            title = "20-20-20 眼球睫狀肌護眼法",
                            durationMinutes = 2,
                            icon = Icons.Default.Visibility,
                            description = "每看書20分鐘，向20英尺 (約6公尺) 外綠色植物遠眺20秒，緩解近距離視力疲勞。",
                            color = WaterCyan,
                            onStart = {
                                activeStretchGuide = StretchRoutine(
                                    "20-20-20 睫狀肌護眼法",
                                    120,
                                    "閉上雙眼深呼吸10秒；緩緩睜開眼睛，凝視6公尺以外最遠處的窗景或綠樹；順時針緩慢轉動眼球一圈，再逆時針轉動一圈，眨眼放鬆。"
                                )
                            }
                        )
                    }
                    item {
                        StretchRoutineCard(
                            title = "腰椎坐骨伸展 (久坐椅子延展)",
                            durationMinutes = 3,
                            icon = Icons.Default.FitnessCenter,
                            description = "坐在椅面前1/3處，雙手扶椅背做脊椎微扭轉，拉伸下背肌群，改善血液循環。",
                            color = SleepPurple,
                            onStart = {
                                activeStretchGuide = StretchRoutine(
                                    "腰椎坐骨伸展",
                                    180,
                                    "端坐於椅子前側，脊椎挺直；右手搭在左膝外側，身體緩慢向左後方旋轉，維持深呼吸3次；再換向右後方旋轉；最後雙腿微微伸直，雙手下觸腳尖拉伸大腿後側。"
                                )
                            }
                        )
                    }
                    item {
                        StretchRoutineCard(
                            title = "4-7-8 深度腹式呼吸 (考前焦慮解方)",
                            durationMinutes = 2,
                            icon = Icons.Default.HealthAndSafety,
                            description = "吸氣4秒、屏息7秒、呼氣8秒，迅速刺激副交感神經，平息心悸與焦慮。",
                            color = WarningOrange,
                            onStart = {
                                activeStretchGuide = StretchRoutine(
                                    "4-7-8 深度腹式呼吸",
                                    120,
                                    "閉上嘴巴，用鼻子安靜吸氣數4秒；屏住呼吸停頓7秒；從嘴巴用力呼氣發出呼聲數8秒。重複四個循環，讓心跳恢復從容穩定。"
                                )
                            }
                        )
                    }
                }
                3 -> {
                    // Modular routine presets
                    item {
                        PresetSelectorCard(
                            activePreset = activePreset,
                            onSelectPreset = { viewModel.selectPreset(it) }
                        )
                    }
                    item {
                        Text(
                            text = "當前模組日程時間表 (${activePreset.title})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(activePreset.scheduleBlocks) { block ->
                        ScheduleBlockItem(block = block)
                    }
                }
            }
        }
    }

    // Sleep logging dialog
    if (showSleepDialog) {
        SleepRecordDialog(
            currentSleepHours = currentHealth.sleepHours,
            currentBedtime = currentHealth.bedtime,
            currentWakeTime = currentHealth.wakeTime,
            onDismiss = { showSleepDialog = false },
            onSave = { hours, bedtime, wakeTime ->
                viewModel.logSleep(hours, bedtime, wakeTime)
                showSleepDialog = false
            }
        )
    }

    // Exercise logging dialog
    if (showExerciseDialog) {
        ExerciseRecordDialog(
            onDismiss = { showExerciseDialog = false },
            onSave = { mins, type ->
                viewModel.logExercise(mins, type)
                showExerciseDialog = false
            }
        )
    }

    // Interactive stretch countdown dialog
    activeStretchGuide?.let { routine ->
        StretchCountdownDialog(
            routine = routine,
            onDismiss = { activeStretchGuide = null },
            onFinish = {
                viewModel.logExercise(routine.durationSeconds / 60, routine.name)
                activeStretchGuide = null
            }
        )
    }
}

data class StretchRoutine(
    val name: String,
    val durationSeconds: Int,
    val instructions: String
)

@Composable
fun SleepSectionCard(
    healthLog: DailyHealthLog,
    onOpenLogDialog: () -> Unit,
    onToggleWindDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sleep_section_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SleepPurple.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SleepPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "科學睡眠時鐘",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "充足睡眠是大腦鞏固記憶的唯一途徑",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = onOpenLogDialog,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleepPurple)
                ) {
                    Text("記錄作息", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SleepMetricBox("實際睡眠", "${healthLog.sleepHours} 小時", SleepPurple)
                SleepMetricBox("目標時數", "${healthLog.targetSleepHours} 小時", MaterialTheme.colorScheme.primary)
                SleepMetricBox("就寢時間", healthLog.bedtime, if (healthLog.isLateNightWarningTriggered) DangerRed else MaterialTheme.colorScheme.onSurface)
                SleepMetricBox("起床時間", healthLog.wakeTime, MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Screen Wind down digital detox switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onToggleWindDown() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (healthLog.screenWindDownDone) Icons.Default.Check else Icons.Default.Nightlife,
                        contentDescription = "Detox",
                        tint = if (healthLog.screenWindDownDone) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "睡前半小時數位排毒 (遠離手機藍光)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (healthLog.screenWindDownDone) "已完成：褪黑激素正常分泌中" else "點擊打卡：提早放下手機，平靜神經",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (healthLog.screenWindDownDone) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (healthLog.screenWindDownDone) "✓ 已達成" else "+10分",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (healthLog.screenWindDownDone) SuccessGreen else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SleepMetricBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun SleepDebtExplainerCard(sleepDebtHours: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sleepDebtHours > 0.5f) WarningOrange.copy(alpha = 0.1f) else SuccessGreen.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LockClock,
                    contentDescription = "Debt",
                    tint = if (sleepDebtHours > 0.5f) WarningOrange else SuccessGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (sleepDebtHours > 0.5f) "睡眠債累積警示 (${String.format("%.1f", sleepDebtHours)} 小時赤字)" else "睡眠時數充足，節律維持良好！",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (sleepDebtHours > 0.5f) WarningOrange else SuccessGreen
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (sleepDebtHours > 0.5f)
                    "醫學實證：睡眠赤字會顯著損害前額葉皮質的高階邏輯運算與考場應變力。補眠法則：今晚提早30~45分鐘就寢，切忌週末一覺睡到中午（會誘發「星期一社交時差」）。"
                else
                    "昨夜獲得了足量深層慢波睡眠與快速動眼期(REM)，海馬迴記憶已成功轉存，今天的大腦學習吸收效率處於顛峰！",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun CircadianNeuroscienceCard() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📚 專家建議：高中生作息黃金週期",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• 23:00 - 01:00：生長激素分泌顛峰，細胞修復與骨骼生長最旺盛期。\n• 01:00 - 04:00：深層慢波睡眠，大腦清除類澱粉蛋白等代謝廢物。\n• 04:00 - 06:30：REM 快速動眼期密集發生，負責將白天的單字、數理公式轉化為長期記憶神經突觸連接。\n• 晨起日照15分鐘：促進血清素分泌，校正視交叉上核(SCN)生物時鐘。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun WaterIntakeTrackerCard(
    currentMl: Int,
    targetMl: Int,
    onAddWater: (Int) -> Unit,
    onResetWater: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("water_tracker_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = WaterCyan.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WaterCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalDrink,
                            contentDescription = "Water",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "大腦補水進度",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "輕度缺水1%即會使注意力下降12%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onResetWater) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Water",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val ratio = (currentMl.toFloat() / targetMl).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$currentMl / $targetMl ml",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = WaterCyan
                )
                Text(
                    text = "${(ratio * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = WaterCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onAddWater(250) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = WaterCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+250ml 水杯", fontSize = 12.sp)
                }
                Button(
                    onClick = { onAddWater(500) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = WaterCyan.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+500ml 水壺", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ThreeMealsChecklistCard(
    healthLog: DailyHealthLog,
    onToggleMeal: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = "Diet",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "三餐規律打卡 (穩定血糖與神經能量)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            MealToggleRow(
                mealName = "大腦啟動早餐",
                description = "全穀燕麥、雞蛋牛奶優質蛋白質，防課堂低血糖",
                isLogged = healthLog.breakfastLogged,
                onToggle = { onToggleMeal("breakfast") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            MealToggleRow(
                mealName = "均衡提神午餐",
                description = "多蔬菜、適量白肉澱粉，避免高糖精緻澱粉導致午後大腦昏睡",
                isLogged = healthLog.lunchLogged,
                onToggle = { onToggleMeal("lunch") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            MealToggleRow(
                mealName = "清淡好眠晚餐",
                description = "7~8分飽為宜，睡前3小時結束進食，避免胃酸逆流干擾深度睡眠",
                isLogged = healthLog.dinnerLogged,
                onToggle = { onToggleMeal("dinner") }
            )
        }
    }
}

@Composable
fun MealToggleRow(
    mealName: String,
    description: String,
    isLogged: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isLogged) SuccessGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mealName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isLogged) SuccessGreen else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (isLogged) Icons.Default.Check else Icons.Default.Add,
            contentDescription = "Toggle",
            tint = if (isLogged) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun BrainNutritionalTipsCard() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🥗 高中考生健腦護眼補給指南",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "1. DHA 與 Omega-3：深海魚、核桃堅果，能修復大腦神經細胞膜，提升概念理解力。\n2. 葉黃素與玉米黃素：深綠色菠菜、地瓜葉、藍莓，過濾螢幕藍光，減少視網膜氧化壓力。\n3. 維生素B群：糙米、香蕉、豆類，參與神經傳導物質合成，緩解背書疲勞。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun ExerciseTrackerCard(
    exerciseMinutes: Int,
    targetMinutes: Int,
    exerciseType: String,
    onOpenExerciseDialog: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_tracker_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ExerciseEmerald.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ExerciseEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = "Exercise",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "規律運動與活動",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "有氧運動刺激大腦分泌BDNF神經營養因子",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = onOpenExerciseDialog,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExerciseEmerald)
                ) {
                    Text("+ 記錄運動", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val ratio = (exerciseMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "$exerciseMinutes / $targetMinutes 分鐘",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = ExerciseEmerald
                    )
                    Text(
                        text = "主要項目：$exerciseType",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(ratio * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = ExerciseEmerald,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun StretchRoutineCard(
    title: String,
    durationMinutes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    color: Color,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStart() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${durationMinutes}分鐘",
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("引導", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PresetSelectorCard(
    activePreset: RoutinePreset,
    onSelectPreset: (RoutinePreset) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "科學模組化作息規劃切換",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "根據學期階段一鍵套用生理節律模板，避免無序作息引發學習疲勞",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            DefaultPresets.ALL.forEach { preset ->
                val isSelected = preset.id == activePreset.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectPreset(preset) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = preset.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleBlockItem(block: ScheduleBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val badgeColor = when (block.category) {
                "SLEEP" -> SleepPurple
                "STUDY" -> MaterialTheme.colorScheme.primary
                "MEAL" -> MaterialTheme.colorScheme.secondary
                "EXERCISE" -> ExerciseEmerald
                else -> WarningOrange
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = block.timeRange,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = block.tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// Dialogs
@Composable
fun SleepRecordDialog(
    currentSleepHours: Float,
    currentBedtime: String,
    currentWakeTime: String,
    onDismiss: () -> Unit,
    onSave: (Float, String, String) -> Unit
) {
    var hours by remember { mutableFloatStateOf(if (currentSleepHours > 0f) currentSleepHours else 7.5f) }
    var bedtime by remember { mutableStateOf(currentBedtime) }
    var wakeTime by remember { mutableStateOf(currentWakeTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("記錄睡眠與作息時鐘", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "實際睡眠時數：${String.format("%.1f", hours)} 小時",
                    fontWeight = FontWeight.Bold,
                    color = SleepPurple
                )
                Slider(
                    value = hours,
                    onValueChange = { hours = it },
                    valueRange = 4f..10f,
                    steps = 11
                )
                OutlinedTextField(
                    value = bedtime,
                    onValueChange = { bedtime = it },
                    label = { Text("昨夜入睡時間 (例如 23:15)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = wakeTime,
                    onValueChange = { wakeTime = it },
                    label = { Text("今晨起床時間 (例如 06:45)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "提示：超過 23:30 入睡會觸發熬夜警告，影響隔天皮質醇與注意力穩定度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(hours, bedtime, wakeTime) }) {
                Text("保存記錄")
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
fun ExerciseRecordDialog(
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit
) {
    var minutes by remember { mutableIntStateOf(20) }
    var type by remember { mutableStateOf("課間伸展與操場快走") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("記錄課間運動與活動", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "運動時長：$minutes 分鐘",
                    fontWeight = FontWeight.Bold,
                    color = ExerciseEmerald
                )
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { minutes = it.toInt() },
                    valueRange = 5f..60f,
                    steps = 10
                )
                val presetTypes = listOf("操場快走/慢跑", "課間伸展放鬆", "跳繩微出汗", "球類活動", "深蹲伸展")
                Text(text = "快捷選擇項目：", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetTypes) { item ->
                        FilterChip(
                            selected = type == item,
                            onClick = { type = item },
                            label = { Text(item, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(minutes, type) }) {
                Text("確定保存")
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
fun StretchCountdownDialog(
    routine: StretchRoutine,
    onDismiss: () -> Unit,
    onFinish: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(routine.durationSeconds) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(isPaused) {
        while (remainingSeconds > 0 && !isPaused) {
            delay(1000)
            remainingSeconds--
        }
        if (remainingSeconds == 0) {
            onFinish()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "🧘 ${routine.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val min = remainingSeconds / 60
                val sec = remainingSeconds % 60
                Text(
                    text = String.format("%02d:%02d", min, sec),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = ExerciseEmerald
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = routine.instructions,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                        lineHeight = 20.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { isPaused = !isPaused }
            ) {
                Text(if (isPaused) "繼續" else "暫停")
            }
        },
        dismissButton = {
            TextButton(onClick = onFinish) {
                Text("提早完成打卡")
            }
        }
    )
}
