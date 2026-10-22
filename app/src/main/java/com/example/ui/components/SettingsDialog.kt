package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    currentRate: Double,
    currentCurrency: String,
    onDismiss: () -> Unit,
    onSave: (rate: Double, currency: String) -> Unit
) {
    var rateInput by remember {
        mutableStateOf(if (currentRate % 1.0 == 0.0) currentRate.toInt().toString() else currentRate.toString())
    }
    var selectedCurrency by remember { mutableStateOf(currentCurrency) }

    val currencies = listOf("$", "€", "£", "¥", "A$", "C$")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Preferences", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Currency selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Currency Symbol",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencies.forEach { sym ->
                            FilterChip(
                                selected = selectedCurrency == sym,
                                onClick = { selectedCurrency = sym },
                                label = { Text(sym) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Default hourly rate
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Default Hourly Rate",
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
                        label = { Text("Standard rate") },
                        leadingIcon = { Text(selectedCurrency, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rate = rateInput.toDoubleOrNull() ?: currentRate
                    onSave(rate, selectedCurrency)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_settings_btn")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
