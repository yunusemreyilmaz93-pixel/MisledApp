package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainLayout(viewModel)
            }
        }
    }
}

@Composable
fun MainLayout(viewModel: MainViewModel) {
    val userEmail by viewModel.userEmail.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val activePassageId by viewModel.activePassageId.collectAsState()

    // Local state for active checkpoint screen overlay
    var activeCheckpointId by remember { mutableStateOf<String?>(null) }

    val isAuthenticated = userEmail != null || isGuest

    if (!isAuthenticated) {
        AuthScreen(viewModel = viewModel)
    } else {
        // Display full screen focus mode for study if an active passage is open
        if (activePassageId != null) {
            PassageScreen(viewModel = viewModel)
        } else if (activeCheckpointId != null) {
            CheckpointScreen(
                viewModel = viewModel,
                checkpointId = activeCheckpointId!!,
                onBack = { activeCheckpointId = null }
            )
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("app_scaffold"),
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        val navItems = listOf(
                            NavigationTab("home", "Home", Icons.Default.Home),
                            NavigationTab("units", "Curriculum", Icons.Default.AutoStories),
                            NavigationTab("vocabulary", "Arsenal", Icons.Default.MenuBook),
                            NavigationTab("progress", "Analytics", Icons.Default.Analytics),
                            NavigationTab("premium", "Premium", Icons.Default.WorkspacePremium)
                        )

                        navItems.forEach { item ->
                            val isSelected = currentTab == item.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.setTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(text = item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        "home" -> HomeScreen(viewModel = viewModel)
                        "units" -> UnitsScreen(
                            viewModel = viewModel,
                            onOpenCheckpoint = { checkpointId -> activeCheckpointId = checkpointId }
                        )
                        "vocabulary" -> VocabularyScreen(viewModel = viewModel)
                        "progress" -> ProgressScreen(viewModel = viewModel)
                        "premium" -> PremiumScreen(viewModel = viewModel)
                        else -> HomeScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

data class NavigationTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)
