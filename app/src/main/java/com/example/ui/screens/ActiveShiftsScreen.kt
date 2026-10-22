package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WorkShiftEntity
import com.example.data.util.DateTimeUtils
import com.example.ui.components.ShiftCard
import com.example.ui.viewmodel.WorkViewModel
import kotlinx.coroutines.launch

@Composable
fun ActiveShiftsScreen(
    viewModel: WorkViewModel,
    snackbarHostState: SnackbarHostState,
    onLogShift: () -> Unit,
    onEditShift: (WorkShiftEntity) -> Unit,
    onNavigateToSummary: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeShifts by viewModel.activeShifts.collectAsStateWithLifecycle()
    val weeklySummary by viewModel.weeklySummary.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showArchivePrompt by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weekly Glance Hero Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_glance_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "This Week's Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = onNavigateToSummary,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    "Full Summary",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Hours Logged",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = DateTimeUtils.formatHours(weeklySummary.totalHours),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Earnings",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = DateTimeUtils.formatCurrency(weeklySummary.totalEarnings, currencySymbol),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // Quick Archive Action to keep interface clean
            if (activeShifts.size > 3) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Keep interface clean",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            FilledTonalButton(
                                onClick = { showArchivePrompt = true },
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Archive Old", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Shifts (${activeShifts.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Shifts list or Empty state
            if (activeShifts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No Active Work Shifts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your working hours list is currently clean. Tap '+ Log Shift' to record hours and daily rates, or view your history.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                OutlinedButton(onClick = onNavigateToHistory) {
                                    Icon(Icons.Default.History, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("View History")
                                }
                                Button(onClick = onLogShift) {
                                    Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Log Shift")
                                }
                            }
                        }
                    }
                }
            } else {
                items(activeShifts, key = { it.id }) { shift ->
                    ShiftCard(
                        shift = shift,
                        currencySymbol = currencySymbol,
                        onEdit = { onEditShift(shift) },
                        onToggleArchive = {
                            viewModel.toggleArchiveShift(shift)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Shift archived to keep view clean")
                            }
                        },
                        onDelete = {
                            viewModel.deleteShift(shift)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Shift deleted")
                            }
                        }
                    )
                }
            }
        }

        // Extended Floating Action Button
        ExtendedFloatingActionButton(
            onClick = onLogShift,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Log Shift", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_shift_fab")
        )
    }

    // Clean / Archive dialog
    if (showArchivePrompt) {
        AlertDialog(
            onDismissRequest = { showArchivePrompt = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Clean Active Interface")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Archive older completed work records to keep your active interface clean and focused. All records are safely preserved in your History tab.")
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            showArchivePrompt = false
                            viewModel.archiveOlderThanDays(14) { count ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Archived $count record(s) older than 14 days")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Archive records older than 14 days")
                    }
                    Button(
                        onClick = {
                            showArchivePrompt = false
                            viewModel.archiveOlderThanDays(30) { count ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Archived $count record(s) older than 30 days")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Archive records older than 30 days")
                    }
                    OutlinedButton(
                        onClick = {
                            showArchivePrompt = false
                            viewModel.archiveAllActive { count ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Archived all $count active records")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Archive all current active records")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showArchivePrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
