package com.kobe.qrbarcode.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kobe.qrbarcode.R
import com.kobe.qrbarcode.core.generate.ErrorCorrection
import com.kobe.qrbarcode.data.prefs.ThemeMode
import com.kobe.qrbarcode.ui.LocalSnackbarHostState
import com.kobe.qrbarcode.ui.components.ColorRowPicker
import com.kobe.qrbarcode.ui.components.InfoPill
import com.kobe.qrbarcode.ui.components.SectionHeader

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 14.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.headlineSmall)
            }
        }

        item { SectionHeader(title = "Appearance") }
        item {
            SettingsCard {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                        ) { Text(mode.label) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                SwitchRow(
                    title = "Material You colours",
                    subtitle = "Follow the wallpaper palette (Android 12+)",
                    checked = settings.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor
                )
            }
        }

        item { SectionHeader(title = "Scanning") }
        item {
            SettingsCard {
                SwitchRow(
                    title = "Vibrate on scan",
                    subtitle = "A short buzz when a code is recognised",
                    checked = settings.vibrate,
                    onCheckedChange = viewModel::setVibrate
                )
                SwitchRow(
                    title = "Beep on scan",
                    subtitle = "Audible confirmation",
                    checked = settings.sound,
                    onCheckedChange = viewModel::setSound
                )
                SwitchRow(
                    title = "Open websites automatically",
                    subtitle = "Scanned links open in your browser without a tap",
                    checked = settings.autoOpenUrls,
                    onCheckedChange = viewModel::setAutoOpenUrls
                )
                SwitchRow(
                    title = "Keep scan history",
                    subtitle = "Store scans on this device",
                    checked = settings.saveScanHistory,
                    onCheckedChange = viewModel::setSaveScanHistory
                )
                SwitchRow(
                    title = "Batch: skip duplicates",
                    subtitle = "Ignore a code that is already in the batch list",
                    checked = settings.batchSkipDuplicates,
                    onCheckedChange = viewModel::setSkipDuplicates
                )
            }
        }

        item { SectionHeader(title = "Default QR style") }
        item {
            SettingsCard {
                ColorRowPicker(
                    label = "Foreground",
                    selected = settings.defaultStyle.foreground,
                    onSelected = viewModel::setDefaultForeground
                )
                Spacer(Modifier.height(14.dp))
                ColorRowPicker(
                    label = "Background",
                    selected = settings.defaultStyle.background,
                    onSelected = viewModel::setDefaultBackground
                )
                Spacer(Modifier.height(14.dp))
                Text("Error correction", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ErrorCorrection.entries.forEachIndexed { index, level ->
                        SegmentedButton(
                            selected = settings.defaultStyle.errorCorrection == level,
                            onClick = { viewModel.setDefaultErrorCorrection(level) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index,
                                ErrorCorrection.entries.size
                            )
                        ) { Text(level.level) }
                    }
                }
            }
        }

        item { SectionHeader(title = "Data") }
        item {
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearDialog = true }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("Clear history and favourites", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Removes every stored code from this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { SectionHeader(title = "About") }
        item {
            SettingsCard {
                Text(
                    text = stringResource(R.string.app_full_name),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                InfoPill(text = "No internet permission", icon = Icons.Rounded.CloudOff)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Scanning, decoding, generation and history all run on this phone. " +
                        "The app has no account, no server and no analytics. A network connection " +
                        "is only used by the app you hand a link to.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Text(
                text = "Made for the Kobe series",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear stored codes?") },
            text = { Text("This deletes all scanned and created codes, including favourites.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearDialog = false
                }) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
