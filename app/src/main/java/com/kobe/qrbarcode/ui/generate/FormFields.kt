package com.kobe.qrbarcode.ui.generate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Renders one form field from its [FieldSpec]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicField(
    spec: FieldSpec,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (val type = spec.type) {
        is FieldType.Options -> {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = value.ifBlank { type.values.first() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(spec.label) },
                    trailingIcon = {
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    type.values.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onChange(option)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }

        FieldType.Toggle -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = spec.label, style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = value == "true",
                    onCheckedChange = { onChange(it.toString()) }
                )
            }
        }

        FieldType.DateTime -> DateTimeField(
            label = spec.label,
            millis = value.toLongOrNull(),
            onChange = { onChange(it.toString()) },
            modifier = modifier
        )

        FieldType.Password -> {
            var visible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(spec.label) },
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            imageVector = if (visible) Icons.Rounded.VisibilityOff
                            else Icons.Rounded.Visibility,
                            contentDescription = if (visible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = modifier.fillMaxWidth()
            )
        }

        else -> {
            val multiline = type == FieldType.Multiline
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(spec.label + if (spec.required) " *" else "") },
                placeholder = if (spec.placeholder.isNotBlank()) {
                    { Text(spec.placeholder) }
                } else null,
                singleLine = !multiline,
                minLines = if (multiline) 3 else 1,
                keyboardOptions = KeyboardOptions(keyboardType = type.keyboardType()),
                modifier = modifier
                    .fillMaxWidth()
                    .then(if (multiline) Modifier.heightIn(min = 110.dp) else Modifier)
            )
        }
    }
}

private fun FieldType.keyboardType(): KeyboardType = when (this) {
    FieldType.Number -> KeyboardType.Number
    FieldType.Phone -> KeyboardType.Phone
    FieldType.Email -> KeyboardType.Email
    FieldType.Url -> KeyboardType.Uri
    else -> KeyboardType.Text
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeField(
    label: String,
    millis: Long?,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<Long?>(null) }
    val formatter = remember { SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDate = true }
    ) {
        OutlinedTextField(
            value = millis?.let { formatter.format(Date(it)) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text("Pick a date and time") },
            trailingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = millis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDate = pickerState.selectedDateMillis
                    showDate = false
                    showTime = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTime) {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis ?: System.currentTimeMillis() }
        val timeState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTime = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Choose a time", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timeState)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTime = false }) { Text("Cancel") }
                        Spacer(Modifier.size(8.dp))
                        TextButton(onClick = {
                            val base = Calendar.getInstance().apply {
                                timeInMillis = pendingDate ?: millis ?: System.currentTimeMillis()
                                set(Calendar.HOUR_OF_DAY, timeState.hour)
                                set(Calendar.MINUTE, timeState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onChange(base.timeInMillis)
                            showTime = false
                        }) { Text("Done") }
                    }
                }
            }
        }
    }
}
