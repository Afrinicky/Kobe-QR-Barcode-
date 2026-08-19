package com.kobe.qrbarcode.ui.batch

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kobe.qrbarcode.core.util.Feedback
import com.kobe.qrbarcode.core.util.TimeFormat
import com.kobe.qrbarcode.ui.LocalSnackbarHostState
import com.kobe.qrbarcode.ui.components.IconBadge
import com.kobe.qrbarcode.ui.components.visual
import com.kobe.qrbarcode.ui.scan.ScannerCamera
import com.kobe.qrbarcode.ui.scan.ScannerOverlay

/**
 * Continuous scanning for inventory work: every new symbol drops into a list
 * that can be exported as CSV or plain text.
 */
@Composable
fun BatchScanScreen(
    onClose: () -> Unit,
    viewModel: BatchScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            if (hasPermission) {
                ScannerCamera(
                    paused = state.paused,
                    torchEnabled = state.torchEnabled,
                    onDetected = { codes ->
                        if (viewModel.onDetected(codes)) {
                            Feedback.vibrate(context, 35)
                        }
                    },
                    onTorchAvailable = viewModel::setTorchAvailable,
                    modifier = Modifier.fillMaxSize()
                )
                ScannerOverlay(active = !state.paused, windowFraction = 0.82f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassIcon(Icons.Rounded.Close, "Close", onClose)
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${state.items.size} collected",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.torchAvailable) {
                        GlassIcon(
                            if (state.torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                            "Flashlight",
                            viewModel::toggleTorch,
                            highlighted = state.torchEnabled
                        )
                    }
                    GlassIcon(
                        if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        if (state.paused) "Resume" else "Pause",
                        viewModel::togglePause
                    )
                }
            }

            if (state.paused) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = "Paused",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }

        BatchPanel(
            state = state,
            onRemove = viewModel::remove,
            onClear = viewModel::clear,
            onCopy = viewModel::copyAll,
            onCsv = { viewModel.exportCsv(share = true) },
            onTxt = { viewModel.exportTxt(share = true) },
            onSave = viewModel::saveToHistory,
            onToggleDuplicates = viewModel::toggleSkipDuplicates
        )
    }
}

@Composable
private fun BatchPanel(
    state: BatchUiState,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onCsv: () -> Unit,
    onTxt: () -> Unit,
    onSave: () -> Unit,
    onToggleDuplicates: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Collected codes",
                    style = MaterialTheme.typography.titleSmall
                )
                FilterChip(
                    selected = state.skipDuplicates,
                    onClick = onToggleDuplicates,
                    label = { Text("Skip duplicates") }
                )
            }

            Spacer(Modifier.height(8.dp))

            if (state.items.isEmpty()) {
                Text(
                    text = "Keep the camera moving — each new code is added automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 210.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.items, key = { it.content }) { item ->
                        BatchRow(item = item, onRemove = { onRemove(item.content) })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportChip("CSV", Icons.Rounded.TableChart, state.items.isNotEmpty(), onCsv)
                ExportChip("TXT", Icons.Rounded.Description, state.items.isNotEmpty(), onTxt)
                ExportChip("Copy", Icons.Rounded.ContentCopy, state.items.isNotEmpty(), onCopy)
                ExportChip("Save", Icons.Rounded.Save, state.items.isNotEmpty(), onSave)
            }

            if (state.items.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Clear list")
                }
            }
        }
    }
}

@Composable
private fun BatchRow(item: BatchItem, onRemove: () -> Unit) {
    val visual = item.parsed.kind.visual()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconBadge(icon = visual.icon, tint = visual.color, size = 34)
        Column(Modifier.weight(1f)) {
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.format.displayName} · ${TimeFormat.time(item.scannedAt)}" +
                    if (item.count > 1) " · ×${item.count}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExportChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, Modifier.size(17.dp)) },
        colors = AssistChipDefaults.assistChipColors()
    )
}

@Composable
private fun GlassIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (highlighted) Color.White else Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (highlighted) Color.Black else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
