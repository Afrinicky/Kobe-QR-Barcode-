package com.kobe.qrbarcode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import java.util.Locale

private val SWATCHES = listOf(
    0xFF101010, 0xFF000000, 0xFFFFFFFF, 0xFF5B4BE0, 0xFF3A2DA8, 0xFF00B39A,
    0xFF0EA5E9, 0xFF2563EB, 0xFF7C3AED, 0xFFDB2777, 0xFFDC2626, 0xFFEA580C,
    0xFFCA8A04, 0xFF16A34A, 0xFF0F766E, 0xFF334155, 0xFF78350F, 0xFFF8FAFC
).map { it.toInt() }

@Composable
fun ColorRowPicker(
    label: String,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustom by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = selected.toHex(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { showCustom = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = "Custom colour",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(SWATCHES) { swatch ->
                val isSelected = swatch == selected
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(swatch))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                        .clickable { onSelected(swatch) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (Color(swatch).perceivedLuminance() > 0.55f) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCustom) {
        CustomColorDialog(
            initial = selected,
            onDismiss = { showCustom = false },
            onConfirm = {
                onSelected(it)
                showCustom = false
            }
        )
    }
}

@Composable
private fun CustomColorDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var red by remember { mutableFloatStateOf(((initial shr 16) and 0xFF).toFloat()) }
    var green by remember { mutableFloatStateOf(((initial shr 8) and 0xFF).toFloat()) }
    var blue by remember { mutableFloatStateOf((initial and 0xFF).toFloat()) }
    var hex by remember { mutableStateOf(initial.toHex().removePrefix("#")) }

    fun current(): Int = Color(red.toInt(), green.toInt(), blue.toInt()).toArgb()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(current()) }) { Text("Use colour") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Custom colour") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(current()))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                )
                Spacer(Modifier.height(14.dp))
                ChannelSlider("Red", red) { red = it; hex = current().toHex().removePrefix("#") }
                ChannelSlider("Green", green) { green = it; hex = current().toHex().removePrefix("#") }
                ChannelSlider("Blue", blue) { blue = it; hex = current().toHex().removePrefix("#") }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = hex,
                    onValueChange = { input ->
                        val cleaned = input.filter { it.isDigit() || it.uppercaseChar() in "ABCDEF" }
                            .take(6).uppercase()
                        hex = cleaned
                        if (cleaned.length == 6) {
                            val parsed = cleaned.toLongOrNull(16) ?: return@OutlinedTextField
                            red = ((parsed shr 16) and 0xFF).toFloat()
                            green = ((parsed shr 8) and 0xFF).toFloat()
                            blue = (parsed and 0xFF).toFloat()
                        }
                    },
                    label = { Text("Hex") },
                    singleLine = true,
                    prefix = { Text("#") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun ChannelSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(52.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toInt().toString(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

fun Int.toHex(): String = "#%06X".format(Locale.US, 0xFFFFFF and this)

private fun Color.perceivedLuminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
