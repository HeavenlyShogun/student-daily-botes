package com.example.ui.health

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.DefaultSelfMenu
import com.example.data.model.SelfMenuItem
import com.example.ui.theme.ExerciseEmerald
import com.example.ui.theme.SleepPurple
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WaterCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelfMenuDecisionSection(
    menuItems: List<SelfMenuItem>,
    recommendedMeal: SelfMenuItem?,
    onPickRandomMeal: (String?, String?) -> Unit,
    onChooseMeal: (SelfMenuItem) -> Unit,
    onToggleFavorite: (SelfMenuItem) -> Unit,
    onDeleteMeal: (SelfMenuItem) -> Unit,
    onAddMeal: (String, String, List<String>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMealSlot by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Collect all unique tags across preset tags and user's custom menu items
    val allAvailableTags = remember(menuItems) {
        (DefaultSelfMenu.PRESET_TAGS + menuItems.flatMap { it.tags }).distinct()
    }

    val filteredItems = remember(menuItems, selectedMealSlot, selectedTag) {
        menuItems.filter { item ->
            (selectedMealSlot == null || item.mealSlot == selectedMealSlot) &&
            (selectedTag == null || item.tags.contains(selectedTag))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("self_menu_section"),
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(WarningOrange.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = "Self Menu",
                            tint = WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "個人化飲食自我菜單 (不猶豫決策器)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "依餐別與情境標籤秒選餐點，省下選擇疲勞的大腦能量",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("open_add_self_menu_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新增菜單", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Meal Slot Filter Row
            Text(
                text = "1. 選擇時段餐別：",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedMealSlot == null,
                        onClick = { selectedMealSlot = null },
                        label = { Text("全部餐別", fontSize = 11.sp) }
                    )
                }
                items(DefaultSelfMenu.PRESET_MEAL_SLOTS) { slot ->
                    FilterChip(
                        selected = selectedMealSlot == slot,
                        onClick = {
                            selectedMealSlot = if (selectedMealSlot == slot) null else slot
                        },
                        label = { Text(slot, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Multi-Tag Filter Row
            Text(
                text = "2. 選擇今日需求標籤（點擊篩選）：",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { selectedTag = null },
                        label = { Text("全部標籤", fontSize = 11.sp) }
                    )
                }
                items(allAvailableTags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = {
                            selectedTag = if (selectedTag == tag) null else tag
                        },
                        label = { Text("#$tag", fontSize = 11.sp) },
                        modifier = Modifier.testTag("menu_tag_filter_$tag")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. One-Tap "Help Me Decide What to Eat Today" Button
            Button(
                onClick = { onPickRandomMeal(selectedMealSlot, selectedTag) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_random_meal_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarningOrange
                )
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val filterDesc = listOfNotNull(selectedMealSlot, selectedTag?.let { "#$it" }).joinToString(" · ")
                Text(
                    text = if (filterDesc.isBlank()) "🎲 今天吃什麼？幫我一鍵抽選！" else "🎲 依「$filterDesc」幫我決定吃什麼！",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // 4. Highlighted Recommendation Card (when picked)
            AnimatedVisibility(visible = recommendedMeal != null) {
                recommendedMeal?.let { picked ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WarningOrange.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .border(1.5.dp, WarningOrange, RoundedCornerShape(14.dp))
                            .testTag("recommended_meal_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎯 秘書今日推薦決定（不再猶豫！）",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningOrange
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = WarningOrange
                                ) {
                                    Text(
                                        text = picked.mealSlot,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = picked.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (picked.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    picked.tags.forEach { tag ->
                                        TagBadge(tag = tag)
                                    }
                                }
                            }

                            if (picked.brainBenefitNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🧠 營養師叮嚀：${picked.brainBenefitNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onChooseMeal(picked) },
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("就吃這個！(同步打卡)", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onPickRandomMeal(selectedMealSlot, selectedTag) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("換一個", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Filtered Self-Menu List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的自我菜單庫（共 ${filteredItems.size} 道符合條件）",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "此標籤組合下尚無餐點",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "點擊右上角「+ 新增菜單」建立專屬餐點與標籤！",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredItems.forEach { item ->
                        SelfMenuItemCard(
                            item = item,
                            onChoose = { onChooseMeal(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onDelete = { onDeleteMeal(item) },
                            onTagClick = { clickedTag ->
                                selectedTag = if (selectedTag == clickedTag) null else clickedTag
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSelfMenuDialog(
            availableTags = allAvailableTags,
            onDismiss = { showAddDialog = false },
            onSave = { name, slot, tags, note ->
                onAddMeal(name, slot, tags, note)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelfMenuItemCard(
    item: SelfMenuItem,
    onChoose: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onTagClick: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("self_menu_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = mealSlotColor(item.mealSlot).copy(alpha = 0.16f)
                ) {
                    Text(
                        text = item.mealSlot,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = mealSlotColor(item.mealSlot),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Meal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.tags.forEach { tag ->
                        TagBadge(tag = tag, onClick = { onTagClick(tag) })
                    }
                }
            }

            if (item.brainBenefitNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.brainBenefitNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.timesChosen > 0) "已選用 ${item.timesChosen} 次" else "尚未選用",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onChoose,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("今天吃這個", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun TagBadge(
    tag: String,
    onClick: (() -> Unit)? = null
) {
    val badgeColor = when {
        tag.contains("高蛋白") || tag.contains("健腦") -> SleepPurple
        tag.contains("低GI") || tag.contains("防昏睡") -> ExerciseEmerald
        tag.contains("便利商店") || tag.contains("快速") -> WaterCyan
        tag.contains("省錢") || tag.contains("銅板") -> WarningOrange
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = badgeColor.copy(alpha = 0.14f),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(
            text = "#$tag",
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun mealSlotColor(slot: String): Color = when (slot) {
    "早餐" -> WarningOrange
    "午餐" -> ExerciseEmerald
    "晚餐" -> SleepPurple
    else -> WaterCyan
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSelfMenuDialog(
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mealSlot by remember { mutableStateOf("午餐") }
    var customTagInput by remember { mutableStateOf("") }
    var brainBenefitNote by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf("高蛋白健腦", "低GI防昏睡") }
    val extraCustomTags = remember { mutableStateListOf<String>() }

    val allDialogTags = (availableTags + extraCustomTags).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("新增專屬自我菜單餐點", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("餐點名稱 (例如：巷口火雞肉飯＋燙青菜＋滷蛋)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("self_menu_name_input")
                    )
                }

                item {
                    Text("適用餐別：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DefaultSelfMenu.PRESET_MEAL_SLOTS.forEach { slot ->
                            FilterChip(
                                selected = mealSlot == slot,
                                onClick = { mealSlot = slot },
                                label = { Text(slot, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "點選餐點標籤（可複選，幫助日後快速篩選）：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allDialogTags.forEach { tag ->
                            val isSelected = selectedTags.contains(tag)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                                },
                                label = { Text("#$tag", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customTagInput,
                            onValueChange = { customTagInput = it },
                            label = { Text("新增自訂標籤 (如：校門口、媽媽煮)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_tag_input"),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val cleaned = customTagInput.trim().removePrefix("#")
                                if (cleaned.isNotBlank()) {
                                    if (!extraCustomTags.contains(cleaned)) extraCustomTags.add(cleaned)
                                    if (!selectedTags.contains(cleaned)) selectedTags.add(cleaned)
                                    customTagInput = ""
                                }
                            },
                            enabled = customTagInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ 加入標籤", fontSize = 11.sp)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = brainBenefitNote,
                        onValueChange = { brainBenefitNote = it },
                        label = { Text("營養/備註 (選填，如：少飯多菜下午不昏睡)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalTags = selectedTags.toList()
                        onSave(name.trim(), mealSlot, finalTags, brainBenefitNote.trim())
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_save_self_menu_button")
            ) {
                Text("儲存至自我菜單")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
