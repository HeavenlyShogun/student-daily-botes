package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("秘書首頁", Icons.Filled.School, Icons.Outlined.Dashboard, "nav_dashboard"),
    HEALTH("健康作息", Icons.Filled.HealthAndSafety, Icons.Outlined.Bedtime, "nav_health"),
    TODO("動態待辦", Icons.Filled.Checklist, Icons.Outlined.Checklist, "nav_todo"),
    JOURNAL("正向手帳", Icons.Filled.EditNote, Icons.Outlined.EditNote, "nav_journal")
}
