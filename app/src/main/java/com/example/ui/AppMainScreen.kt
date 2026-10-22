package com.example.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.health.HealthPlannerScreen
import com.example.ui.journal.JournalScreen
import com.example.ui.navigation.NavigationScreen
import com.example.ui.review.WeeklyReviewScreen
import com.example.ui.secretary.SecretaryDashboardScreen
import com.example.ui.theme.WaterCyan
import com.example.ui.todo.TodoListScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(NavigationScreen.DASHBOARD) }

    // Runtime Permission for Android 13+ (POST_NOTIFICATIONS)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Graceful handling: notification alarms already handle exact/inexact fallback
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // BackHandler: If user is on a secondary screen and presses back, go to DASHBOARD
    if (currentScreen != NavigationScreen.DASHBOARD) {
        BackHandler {
            currentScreen = NavigationScreen.DASHBOARD
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_main_screen"),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "學生生活秘書",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "科學作息 × 自律學習 × 正向成長",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Quick water button
                    IconButton(
                        onClick = { viewModel.addWater(250) },
                        modifier = Modifier.testTag("top_bar_water_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalDrink,
                            contentDescription = "Quick Water",
                            tint = WaterCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.testTag(screen.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentScreen,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                NavigationScreen.DASHBOARD -> {
                    SecretaryDashboardScreen(
                        viewModel = viewModel,
                        onNavigateToHealth = { currentScreen = NavigationScreen.HEALTH },
                        onNavigateToTodo = { currentScreen = NavigationScreen.TODO },
                        onNavigateToJournal = { currentScreen = NavigationScreen.JOURNAL },
                        onStartFocus = { task ->
                            viewModel.startFocusSession(task.targetMinutes, task, "${task.subjectTag} 專注")
                            currentScreen = NavigationScreen.TODO
                        }
                    )
                }
                NavigationScreen.HEALTH -> {
                    HealthPlannerScreen(
                        viewModel = viewModel
                    )
                }
                NavigationScreen.TODO -> {
                    TodoListScreen(
                        viewModel = viewModel
                    )
                }
                NavigationScreen.JOURNAL -> {
                    JournalScreen(
                        viewModel = viewModel
                    )
                }
                NavigationScreen.REVIEW -> {
                    WeeklyReviewScreen(
                        viewModel = viewModel,
                        onStartFocus = { task ->
                            viewModel.startFocusSession(task.targetMinutes, task, "${task.subjectTag} 專注")
                            currentScreen = NavigationScreen.TODO
                        }
                    )
                }
            }
        }
    }
}
