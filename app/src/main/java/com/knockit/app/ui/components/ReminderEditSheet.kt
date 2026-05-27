package com.knockit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.knockit.app.data.model.Reminder
import com.knockit.app.data.model.RepeatRule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditSheet(
    reminder: Reminder,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (title: String, triggerAt: Long, repeatRule: String, intervalMinutes: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember(reminder.id) { mutableStateOf(reminder.title) }
    var triggerAt by remember(reminder.id) { mutableLongStateOf(reminder.triggerAt) }
    var repeatRule by remember(reminder.id) { mutableStateOf(reminder.repeatRule) }
    var intervalMinutesText by remember(reminder.id) {
        mutableStateOf(reminder.intervalMinutes?.toString() ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Sheet handle area title
            Text(
                text = "Edit Reminder",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp),
            )

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )

            // Date/Time display + picker button
            DateTimePickerField(
                epochMs = triggerAt,
                onDateTimeSelected = { triggerAt = it },
            )

            // Repeat rule dropdown
            RepeatRuleDropdown(
                selected = repeatRule,
                onSelected = { repeatRule = it },
            )

            // Interval minutes (only if repeat is none, for interval-based reminders)
            if (repeatRule == RepeatRule.NONE) {
                OutlinedTextField(
                    value = intervalMinutesText,
                    onValueChange = { intervalMinutesText = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Repeat interval (minutes, optional)") },
                    placeholder = { Text("e.g. 60") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val interval = intervalMinutesText.toIntOrNull()
                        onSave(title, triggerAt, repeatRule, interval)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = title.isNotBlank(),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun DateTimePickerField(
    epochMs: Long,
    onDateTimeSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()) }
    val displayText = remember(epochMs) { dateFmt.format(Date(epochMs)) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.weight(1f),
            label = { Text("Date & Time") },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
        TextButton(
            onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
                // Show date picker first; on date set, show time picker
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val dateCal = cal.clone() as Calendar
                        dateCal.set(Calendar.YEAR, year)
                        dateCal.set(Calendar.MONTH, month)
                        dateCal.set(Calendar.DAY_OF_MONTH, day)

                        android.app.TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                dateCal.set(Calendar.HOUR_OF_DAY, hour)
                                dateCal.set(Calendar.MINUTE, minute)
                                dateCal.set(Calendar.SECOND, 0)
                                dateCal.set(Calendar.MILLISECOND, 0)
                                onDateTimeSelected(dateCal.timeInMillis)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true,
                        ).show()
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
            modifier = Modifier.align(Alignment.CenterVertically),
        ) {
            Text("Edit", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatRuleDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(
        RepeatRule.NONE to "No repeat",
        RepeatRule.DAILY to "Daily",
        RepeatRule.WEEKLY to "Weekly",
    )
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: "No repeat"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text("Repeat") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}
