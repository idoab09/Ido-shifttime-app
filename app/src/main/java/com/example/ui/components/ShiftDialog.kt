package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.WorkShiftEntity
import com.example.data.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShiftDialog(
    initialShift: WorkShiftEntity? = null,
    defaultRate: Double = 25.0,
    defaultBreak: Int = 0,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        dateString: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        breakMinutes: Int,
        hourlyRate: Double,
        note: String
    ) -> Unit
) {
    val context = LocalContext.current

    // Initialize states
    var selectedDateString by remember {
        mutableStateOf(initialShift?.dateString ?: DateTimeUtils.todayDateString())
    }
    var startHour by remember { mutableIntStateOf(initialShift?.startHour ?: 9) }
    var startMinute by remember { mutableIntStateOf(initialShift?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initialShift?.endHour ?: 17) }
    var endMinute by remember { mutableIntStateOf(initialShift?.endMinute ?: 0) }
    var breakMinutes by remember {
        mutableIntStateOf(initialShift?.breakMinutes ?: defaultBreak)
    }
    var rateInput by remember {
        mutableStateOf(
            if (initialShift != null) {
                if (initialShift.hourlyRate % 1.0 == 0.0) {
                    initialShift.hourlyRate.toInt().toString()
                } else {
                    initialShift.hourlyRate.toString()
                }
            } else {
                if (defaultRate % 1.0 == 0.0) defaultRate.toInt().toString() else defaultRate.toString()
            }
        )
    }
    var note by remember { mutableStateOf(initialShift?.note ?: "") }

    // Live calculations
    val calculatedNetMinutes by remember(startHour, startMinute, endHour, endMinute, breakMinutes) {
        derivedStateOf {
            val startTotal = startHour * 60 + startMinute
            val endTotal = endHour * 60 + endMinute
            val gross = if (endTotal < startTotal) {
                (24 * 60 - startTotal) + endTotal
            } else {
                endTotal - startTotal
            }
            maxOf(0, gross - breakMinutes)
        }
    }

    val calculatedHours by remember(calculatedNetMinutes) {
        derivedStateOf { calculatedNetMinutes / 60.0 }
    }

    val parsedRate by remember(rateInput) {
        derivedStateOf { rateInput.toDoubleOrNull() ?: 0.0 }
    }

    val calculatedEarnings by remember(calculatedHours, parsedRate) {
        derivedStateOf { calculatedHours * parsedRate }
    }

    val isFormValid = calculatedNetMinutes > 0 && parsedRate >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        title = {
            Text(
                text = if (initialShift == null) "Log Work Shift" else "Edit Work Shift",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live preview card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Working Duration",
                                style = MaterialTheme.typography.labelMedium
                            )
                            val h = calculatedNetMinutes / 60
                            val m = calculatedNetMinutes % 60
                            Text(
                                text = if (calculatedNetMinutes > 0) "${h}h ${m}m (${String.format(Locale.US, "%.1f", calculatedHours)} hrs)" else "0 hrs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Estimated Earnings",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = DateTimeUtils.formatCurrency(calculatedEarnings, currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 1. Date Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Work Date",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = DateTimeUtils.parseDate(selectedDateString)
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val picked = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth)
                                        }
                                        selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.time)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = DateTimeUtils.formatDisplayDate(selectedDateString),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(Icons.Default.EditCalendar, contentDescription = "Change Date")
                        }
                    }

                    // Quick date chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val todayStr = DateTimeUtils.todayDateString()
                        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calYesterday.time)

                        FilterChip(
                            selected = selectedDateString == todayStr,
                            onClick = { selectedDateString = todayStr },
                            label = { Text("Today") },
                            leadingIcon = if (selectedDateString == todayStr) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedDateString == yesterdayStr,
                            onClick = { selectedDateString = yesterdayStr },
                            label = { Text("Yesterday") },
                            leadingIcon = if (selectedDateString == yesterdayStr) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                // 2. Start & End Hours
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Working Hours (Start – End)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Time Button
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            startHour = h
                                            startMinute = m
                                        },
                                        startHour,
                                        startMinute,
                                        false
                                    ).show()
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Start Time", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = WorkShiftEntity.formatTime(startHour, startMinute),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // End Time Button
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            endHour = h
                                            endMinute = m
                                        },
                                        endHour,
                                        endMinute,
                                        false
                                    ).show()
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("End Time", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = WorkShiftEntity.formatTime(endHour, endMinute),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Quick Shift presets
                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val presets = listOf(
                            Triple("9 AM - 5 PM", 9 to 0, 17 to 0),
                            Triple("8 AM - 4:30 PM", 8 to 0, 16 to 30),
                            Triple("7 AM - 3:30 PM", 7 to 0, 15 to 30),
                            Triple("10 PM - 6 AM (Night)", 22 to 0, 6 to 0)
                        )
                        presets.forEach { (label, start, end) ->
                            val isMatch = startHour == start.first && startMinute == start.second &&
                                    endHour == end.first && endMinute == end.second
                            FilterChip(
                                selected = isMatch,
                                onClick = {
                                    startHour = start.first
                                    startMinute = start.second
                                    endHour = end.first
                                    endMinute = end.second
                                },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }

                // 3. Unpaid Break
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Coffee,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Unpaid Break Duration",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(0, 15, 30, 45, 60).forEach { mins ->
                            FilterChip(
                                selected = breakMinutes == mins,
                                onClick = { breakMinutes = mins },
                                label = { Text(if (mins == 0) "None" else "${mins}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. Hourly Rate
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Hourly Rate for This Day",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                rateInput = input
                            }
                        },
                        label = { Text("Rate per hour") },
                        leadingIcon = {
                            Text(
                                currencySymbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Text(
                                "/ hour",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rate_input")
                    )

                    // Quick rate step suggestions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(20.0, 25.0, 30.0, 35.0, 40.0).forEach { r ->
                            val rStr = if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
                            FilterChip(
                                selected = rateInput == rStr,
                                onClick = { rateInput = rStr },
                                label = { Text("$currencySymbol$rStr") }
                            )
                        }
                    }
                }

                // 5. Notes / Tag
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Project / Client / Note (Optional)") },
                    placeholder = { Text("e.g. Overtime, Client Project, Site B") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isFormValid) {
                        onSave(
                            initialShift?.id ?: 0L,
                            selectedDateString,
                            startHour,
                            startMinute,
                            endHour,
                            endMinute,
                            breakMinutes,
                            parsedRate,
                            note
                        )
                        onDismiss()
                    }
                },
                enabled = isFormValid,
                modifier = Modifier.testTag("save_shift_button")
            ) {
                Text(
                    text = if (initialShift == null) "Log Shift" else "Save Changes",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_shift_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
