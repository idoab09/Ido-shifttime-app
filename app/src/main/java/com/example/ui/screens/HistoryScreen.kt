package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WorkShiftEntity
import com.example.data.util.DateTimeUtils
import com.example.ui.components.ShiftCard
import com.example.ui.viewmodel.HistoryFilter
import com.example.ui.viewmodel.WorkViewModel
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    viewModel: WorkViewModel,
    snackbarHostState: SnackbarHostState,
    onEditShift: (WorkShiftEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val filteredShifts by viewModel.filteredHistoryShifts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val historyFilter by viewModel.historyFilter.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteAllArchivedDialog by remember { mutableStateOf(false) }

    val totalHours = filteredShifts.sumOf { it.hoursWorked }
    val totalEarnings = filteredShifts.sumOf { it.totalEarnings }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        // Search & Filter header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search by note, date...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("history_search_input")
                )

                // Share / Export report
                IconButton(
                    onClick = {
                        val report = viewModel.generateTimesheetReport()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Timesheet Summary", report))

                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, report)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Timesheet"))

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Timesheet report copied to clipboard")
                        }
                    },
                    modifier = Modifier.testTag("export_report_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export report")
                }

                // More archive options
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More archive actions")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Archive older than 30 days") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.archiveOlderThanDays(30) { count ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Archived $count record(s)")
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive older than 60 days") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.archiveOlderThanDays(60) { count ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Archived $count record(s)")
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restore all archived") },
                            leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.restoreAllArchived { count ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Restored $count record(s)")
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete all archived...", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteAllArchivedDialog = true
                            }
                        )
                    }
                }
            }

            // Filter Chips: All, Active, Archived
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = historyFilter == HistoryFilter.ALL,
                    onClick = { viewModel.onHistoryFilterChanged(HistoryFilter.ALL) },
                    label = { Text("All Records") },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterChip(
                    selected = historyFilter == HistoryFilter.ACTIVE,
                    onClick = { viewModel.onHistoryFilterChanged(HistoryFilter.ACTIVE) },
                    label = { Text("Active") },
                    modifier = Modifier.testTag("filter_active")
                )
                FilterChip(
                    selected = historyFilter == HistoryFilter.ARCHIVED,
                    onClick = { viewModel.onHistoryFilterChanged(HistoryFilter.ARCHIVED) },
                    label = { Text("Archived") },
                    modifier = Modifier.testTag("filter_archived")
                )
            }

            // Stats Sub-header
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredShifts.size} shift(s) shown",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${DateTimeUtils.formatHours(totalHours)} • ${DateTimeUtils.formatCurrency(totalEarnings, currencySymbol)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Shifts list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (filteredShifts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No records match '$searchQuery'" else "No records found in this category",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredShifts, key = { it.id }) { shift ->
                    ShiftCard(
                        shift = shift,
                        currencySymbol = currencySymbol,
                        onEdit = { onEditShift(shift) },
                        onToggleArchive = {
                            viewModel.toggleArchiveShift(shift)
                            coroutineScope.launch {
                                val action = if (shift.isArchived) "Shift restored to Active" else "Shift archived"
                                snackbarHostState.showSnackbar(action)
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
    }

    if (showDeleteAllArchivedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllArchivedDialog = false },
            title = { Text("Delete All Archived Shifts?") },
            text = { Text("This will permanently remove all archived records from your database. Active shifts will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllArchivedDialog = false
                        viewModel.deleteAllArchived { count ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Deleted $count archived shift(s)")
                            }
                        }
                    }
                ) {
                    Text("Delete Forever", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllArchivedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
