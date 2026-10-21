package com.example.ui.journal

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import com.example.data.model.DailyJournalEntry
import com.example.data.model.JournalMood
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TertiaryAmber
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentJournal by viewModel.todayJournal.collectAsState()
    val allJournals by viewModel.allJournals.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiJournalFeedback by viewModel.aiJournalFeedback.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("今日手帳與反思", "成長足跡歷史")

    // Form states
    var mood by remember { mutableStateOf(currentJournal?.mood ?: JournalMood.GREAT) }
    var energyLevel by remember { mutableIntStateOf(currentJournal?.energyLevel ?: 4) }
    var goodThing1 by remember { mutableStateOf(currentJournal?.goodThing1 ?: "") }
    var goodThing2 by remember { mutableStateOf(currentJournal?.goodThing2 ?: "") }
    var goodThing3 by remember { mutableStateOf(currentJournal?.goodThing3 ?: "") }
    var selfReflection by remember { mutableStateOf(currentJournal?.selfReflection ?: "") }
    var tomorrowImprovement by remember { mutableStateOf(currentJournal?.tomorrowImprovement ?: "") }

    var saveSuccessMessage by remember { mutableStateOf(false) }

    // Sync when currentJournal changes from database
    LaunchedEffect(currentJournal) {
        if (currentJournal != null) {
            mood = currentJournal!!.mood
            energyLevel = currentJournal!!.energyLevel
            goodThing1 = currentJournal!!.goodThing1
            goodThing2 = currentJournal!!.goodThing2
            goodThing3 = currentJournal!!.goodThing3
            selfReflection = currentJournal!!.selfReflection
            tomorrowImprovement = currentJournal!!.tomorrowImprovement
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("journal_screen")
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header philosophy card
                item {
                    PositiveJournalIntroCard()
                }

                // Mood selector & Encouragement
                item {
                    MoodAndEnergySection(
                        selectedMood = mood,
                        onSelectMood = { mood = it },
                        energyLevel = energyLevel,
                        onSelectEnergy = { energyLevel = it }
                    )
                }

                // Three Good Things
                item {
                    ThreeGoodThingsSection(
                        thing1 = goodThing1,
                        onThing1Change = { goodThing1 = it },
                        thing2 = goodThing2,
                        onThing2Change = { goodThing2 = it },
                        thing3 = goodThing3,
                        onThing3Change = { goodThing3 = it }
                    )
                }

                // Daily Self-Dialogue & Tomorrow's micro-tweak
                item {
                    SelfReflectionSection(
                        reflection = selfReflection,
                        onReflectionChange = { selfReflection = it },
                        improvement = tomorrowImprovement,
                        onImprovementChange = { tomorrowImprovement = it }
                    )
                }

                // AI Growth Coach Feedback Section
                if (!aiJournalFeedback.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
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
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "AI Feedback",
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "✨ AI 學伴成長賦能點評 (Gemini)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.clearAiJournalFeedback() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = aiJournalFeedback!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // Action buttons: Save & AI Coach
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.requestAiJournalFeedback(
                                    mood = mood,
                                    goodThings = listOf(goodThing1, goodThing2, goodThing3),
                                    reflection = selfReflection
                                )
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isAiLoading
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI 導師點評", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.saveJournal(
                                    mood = mood,
                                    energyLevel = energyLevel,
                                    goodThing1 = goodThing1,
                                    goodThing2 = goodThing2,
                                    goodThing3 = goodThing3,
                                    selfReflection = selfReflection,
                                    tomorrowImprovement = tomorrowImprovement
                                )
                                saveSuccessMessage = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("save_journal_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保存手帳", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                if (saveSuccessMessage) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SuccessGreen.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✨ 手帳已即時寫入本機資料庫！微小進步累積成不凡，今天辛苦了！",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            // Historical timeline tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "過往成長足跡 (${allJournals.size} 篇記錄)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (allJournals.isEmpty()) {
                    item {
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("尚無過往記錄，寫下今日的第一篇手帳吧！")
                            }
                        }
                    }
                } else {
                    items(allJournals) { entry ->
                        HistoricalJournalCard(entry = entry)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun PositiveJournalIntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Positivity",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "正向心理學「每日三件好事」",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "醫學實證研究：每天睡前記錄 3 件微小好事與自我反思，能重塑大腦神經迴路，降低高中升學焦慮，將自律轉化為內在成就感。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun MoodAndEnergySection(
    selectedMood: JournalMood,
    onSelectMood: (JournalMood) -> Unit,
    energyLevel: Int,
    onSelectEnergy: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "今天的心情晴雨表：",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(JournalMood.values()) { m ->
                    val isSelected = selectedMood == m
                    Card(
                        modifier = Modifier
                            .clickable { onSelectMood(m) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = m.emoji, fontSize = 22.sp)
                            Text(
                                text = m.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "💡 秘書對話：", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedMood.encouragingMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Energy Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日體力與專注能量：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= energyLevel) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star $star",
                            tint = TertiaryAmber,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onSelectEnergy(star) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThreeGoodThingsSection(
    thing1: String,
    onThing1Change: (String) -> Unit,
    thing2: String,
    onThing2Change: (String) -> Unit,
    thing3: String,
    onThing3Change: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Good Things",
                    tint = Color(0xFFE11D48)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "今日 3 件好事 (微小進步紀錄)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = thing1,
                onValueChange = onThing1Change,
                label = { Text("好事 1：學業小突破 (例如：搞懂一個錯題公式)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = thing2,
                onValueChange = onThing2Change,
                label = { Text("好事 2：身心好習慣 (例如：有快走15分、喝水達標)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = thing3,
                onValueChange = onThing3Change,
                label = { Text("好事 3：生活微確幸 (例如：看到美麗晚霞、同學互助)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
        }
    }
}

@Composable
fun SelfReflectionSection(
    reflection: String,
    onReflectionChange: (String) -> Unit,
    improvement: String,
    onImprovementChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Reflection",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "每日自我對話與成長微調",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = reflection,
                onValueChange = onReflectionChange,
                label = { Text("今日覆盤：今天做得很棒的地方？遇到困難時的心態？") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = improvement,
                onValueChange = onImprovementChange,
                label = { Text("明日微調目標：明天可以更早做好的 1 件小事？") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }
    }
}

@Composable
fun HistoricalJournalCard(entry: DailyJournalEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = entry.mood.emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    (1..entry.energyLevel).forEach {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = TertiaryAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (entry.goodThing1.isNotBlank()) {
                Text(
                    text = "1. ${entry.goodThing1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (entry.goodThing2.isNotBlank()) {
                Text(
                    text = "2. ${entry.goodThing2}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (entry.goodThing3.isNotBlank()) {
                Text(
                    text = "3. ${entry.goodThing3}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (entry.selfReflection.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "反思：${entry.selfReflection}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
