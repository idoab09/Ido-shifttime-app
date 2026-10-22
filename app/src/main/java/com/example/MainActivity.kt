package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WorkShiftEntity
import com.example.ui.components.SettingsDialog
import com.example.ui.components.ShiftDialog
import com.example.ui.screens.ActiveShiftsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.theme.ShiftFlowTheme
import com.example.ui.viewmodel.WorkViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WorkViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiftFlowTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: WorkViewModel) {
    var selectedNavTab by remember { mutableIntStateOf(0) } // 0: Active, 1: Summaries, 2: History
    val snackbarHostState = remember { SnackbarHostState() }

    var showShiftDialog by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<WorkShiftEntity?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val defaultRate by viewModel.defaultRate.collectAsStateWithLifecycle()
    val defaultBreak by viewModel.defaultBreakMinutes.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    // Handle system back navigation
    BackHandler(enabled = selectedNavTab != 0) {
        selectedNavTab = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (selectedNavTab) {
                            0 -> "ShiftFlow"
                            1 -> "Work Summaries"
                            else -> "History & Archives"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (selectedNavTab != 0) {
                        IconButton(
                            onClick = {
                                editingShift = null
                                showShiftDialog = true
                            },
                            modifier = Modifier.testTag("top_add_shift_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Log Shift")
                        }
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Preferences")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedNavTab == 0,
                    onClick = { selectedNavTab = 0 },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text("Active") },
                    modifier = Modifier.testTag("nav_tab_active")
                )
                NavigationBarItem(
                    selected = selectedNavTab == 1,
                    onClick = { selectedNavTab = 1 },
                    icon = { Icon(Icons.Default.Insights, contentDescription = null) },
                    label = { Text("Summaries") },
                    modifier = Modifier.testTag("nav_tab_summaries")
                )
                NavigationBarItem(
                    selected = selectedNavTab == 2,
                    onClick = { selectedNavTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_tab_history")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedNavTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "nav_anim"
            ) { tab ->
                when (tab) {
                    0 -> ActiveShiftsScreen(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        onLogShift = {
                            editingShift = null
                            showShiftDialog = true
                        },
                        onEditShift = { shift ->
                            editingShift = shift
                            showShiftDialog = true
                        },
                        onNavigateToSummary = { selectedNavTab = 1 },
                        onNavigateToHistory = { selectedNavTab = 2 }
                    )
                    1 -> SummaryScreen(
                        viewModel = viewModel,
                        onEditShift = { shift ->
                            editingShift = shift
                            showShiftDialog = true
                        }
                    )
                    2 -> HistoryScreen(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        onEditShift = { shift ->
                            editingShift = shift
                            showShiftDialog = true
                        }
                    )
                }
            }
        }
    }

    // Shift Log / Edit Dialog
    if (showShiftDialog) {
        ShiftDialog(
            initialShift = editingShift,
            defaultRate = defaultRate,
            defaultBreak = defaultBreak,
            currencySymbol = currencySymbol,
            onDismiss = {
                showShiftDialog = false
                editingShift = null
            },
            onSave = { id, dateStr, startH, startM, endH, endM, breakM, rate, note ->
                viewModel.saveShift(
                    id = id,
                    dateString = dateStr,
                    startHour = startH,
                    startMinute = startM,
                    endHour = endH,
                    endMinute = endM,
                    breakMinutes = breakM,
                    hourlyRate = rate,
                    note = note
                )
            }
        )
    }

    // Preferences Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentRate = defaultRate,
            currentCurrency = currencySymbol,
            onDismiss = { showSettingsDialog = false },
            onSave = { rate, curr ->
                viewModel.setDefaultHourlyRate(rate)
                viewModel.setCurrency(curr)
            }
        )
    }
}
