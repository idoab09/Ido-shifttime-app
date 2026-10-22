package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WorkShiftEntity
import com.example.data.util.DateTimeUtils
import com.example.ui.components.ShiftCard
import com.example.ui.viewmodel.DaySummary
import com.example.ui.viewmodel.MonthSummaryData
import com.example.ui.viewmodel.MonthWeekData
import com.example.ui.viewmodel.WeekSummaryData
import com.example.ui.viewmodel.WorkViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: WorkViewModel,
    onEditShift: (WorkShiftEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Weekly, 1 = Monthly

    val weeklySummary by viewModel.weeklySummary.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("summary_screen")
    ) {
        // Tab row: Weekly vs Monthly
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Weekly Summary", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.DateRange, contentDescription = null, Modifier.size(20.dp)) },
                modifier = Modifier.testTag("tab_weekly_summary")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Monthly Summary", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, Modifier.size(20.dp)) },
                modifier = Modifier.testTag("tab_monthly_summary")
            )
        }

        AnimatedContent(targetState = selectedTab, label = "summary_tab_anim") { tab ->
            if (tab == 0) {
                WeeklySummaryView(
                    summary = weeklySummary,
                    currencySymbol = currencySymbol,
                    onPrevWeek = { viewModel.navigateWeek(-1) },
                    onNextWeek = { viewModel.navigateWeek(1) },
                    onResetWeek = { viewModel.resetWeekToCurrent() },
                    onEditShift = onEditShift,
                    onToggleArchive = { viewModel.toggleArchiveShift(it) },
                    onDeleteShift = { viewModel.deleteShift(it) }
                )
            } else {
                MonthlySummaryView(
                    summary = monthlySummary,
                    currencySymbol = currencySymbol,
                    onPrevMonth = { viewModel.navigateMonth(-1) },
                    onNextMonth = { viewModel.navigateMonth(1) },
                    onResetMonth = { viewModel.resetMonthToCurrent() },
                    onEditShift = onEditShift,
                    onToggleArchive = { viewModel.toggleArchiveShift(it) },
                    onDeleteShift = { viewModel.deleteShift(it) }
                )
            }
        }
    }
}

@Composable
fun WeeklySummaryView(
    summary: WeekSummaryData,
    currencySymbol: String,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onResetWeek: () -> Unit,
    onEditShift: (WorkShiftEntity) -> Unit,
    onToggleArchive: (WorkShiftEntity) -> Unit,
    onDeleteShift: (WorkShiftEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation bar
        item {
            PeriodNavigator(
                label = summary.weekRangeDisplay.ifEmpty { "This Week" },
                onPrev = onPrevWeek,
                onNext = onNextWeek,
                onReset = onResetWeek,
                resetLabel = "This Week"
            )
        }

        // Hero Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Hours",
                    value = DateTimeUtils.formatHours(summary.totalHours),
                    subtitle = "${summary.shiftsCount} shift(s)",
                    icon = Icons.Default.AccessTime,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Total Earnings",
                    value = DateTimeUtils.formatCurrency(summary.totalEarnings, currencySymbol),
                    subtitle = if (summary.totalHours > 0) {
                        "Avg $currencySymbol${String.format(Locale.US, "%.2f", summary.effectiveHourlyRate)}/h"
                    } else "No hours",
                    icon = Icons.Default.AttachMoney,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Day-by-Day Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Hours Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target: 8h/day",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    WeeklyBarChart(
                        days = summary.days,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }

        // Shifts list in this week
        item {
            Text(
                text = "Shifts This Week (${summary.shifts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (summary.shifts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No work shifts logged for this week",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(summary.shifts, key = { it.id }) { shift ->
                ShiftCard(
                    shift = shift,
                    currencySymbol = currencySymbol,
                    onEdit = { onEditShift(shift) },
                    onToggleArchive = { onToggleArchive(shift) },
                    onDelete = { onDeleteShift(shift) }
                )
            }
        }
    }
}

@Composable
fun MonthlySummaryView(
    summary: MonthSummaryData,
    currencySymbol: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetMonth: () -> Unit,
    onEditShift: (WorkShiftEntity) -> Unit,
    onToggleArchive: (WorkShiftEntity) -> Unit,
    onDeleteShift: (WorkShiftEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation bar
        item {
            PeriodNavigator(
                label = summary.monthYearDisplay.ifEmpty { "This Month" },
                onPrev = onPrevMonth,
                onNext = onNextMonth,
                onReset = onResetMonth,
                resetLabel = "Current Month"
            )
        }

        // Hero Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Monthly Hours",
                    value = DateTimeUtils.formatHours(summary.totalHours),
                    subtitle = "${summary.workDaysCount} active work days",
                    icon = Icons.Default.AccessTime,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Monthly Earnings",
                    value = DateTimeUtils.formatCurrency(summary.totalEarnings, currencySymbol),
                    subtitle = if (summary.totalHours > 0) {
                        "Avg $currencySymbol${String.format(Locale.US, "%.2f", summary.effectiveHourlyRate)}/h"
                    } else "No hours",
                    icon = Icons.Default.AttachMoney,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Secondary stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Avg Daily Hours", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f hrs/day", summary.avgHoursPerWorkDay),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Shifts", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${summary.shiftsCount} shifts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Weekly Breakdown in Month
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weekly Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    summary.weeklyBreakdown.forEachIndexed { index, week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = week.weekLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = week.dateRange,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = DateTimeUtils.formatHours(week.totalHours),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = DateTimeUtils.formatCurrency(week.totalEarnings, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (index < summary.weeklyBreakdown.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
            }
        }

        // Monthly shifts
        item {
            Text(
                text = "Shifts This Month (${summary.shifts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (summary.shifts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No work shifts logged for this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(summary.shifts, key = { it.id }) { shift ->
                ShiftCard(
                    shift = shift,
                    currencySymbol = currencySymbol,
                    onEdit = { onEditShift(shift) },
                    onToggleArchive = { onToggleArchive(shift) },
                    onDelete = { onDeleteShift(shift) }
                )
            }
        }
    }
}

@Composable
fun PeriodNavigator(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    resetLabel: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = resetLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.labelMedium)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun WeeklyBarChart(
    days: List<DaySummary>,
    currencySymbol: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxHours = maxOf(8.0, (days.maxOfOrNull { it.totalHours } ?: 0.0) * 1.1)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Custom canvas for bars
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 8.dp)
        ) {
            val barWidth = 22.dp.toPx()
            val totalBars = days.size
            val spacing = (size.width - (barWidth * totalBars)) / (totalBars + 1)
            val chartHeight = size.height - 24.dp.toPx()

            // Draw reference dotted line at 8h
            val targetY = chartHeight - ((8.0 / maxHours) * chartHeight).toFloat()
            drawLine(
                color = labelColor.copy(alpha = 0.3f),
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 2.dp.toPx()
            )

            days.forEachIndexed { i, day ->
                val x = spacing + i * (barWidth + spacing)
                val barHeight = ((day.totalHours / maxHours) * chartHeight).toFloat()
                val y = chartHeight - barHeight

                // Background track
                drawRoundRect(
                    color = surfaceVariantColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                // Filled Bar
                if (barHeight > 0) {
                    val barColor = if (day.isToday) secondaryColor else primaryColor
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
        }

        // Labels row below canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp)
                ) {
                    Text(
                        text = day.dayOfWeek,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (day.totalHours > 0) "${String.format(Locale.US, "%.1f", day.totalHours)}h" else "-",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (day.totalHours > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (day.totalHours > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
