package com.kobe.qrbarcode.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kobe.qrbarcode.core.model.ContentKind
import com.kobe.qrbarcode.core.model.SmartAction
import com.kobe.qrbarcode.core.util.ActionRunner
import com.kobe.qrbarcode.core.util.Feedback
import com.kobe.qrbarcode.ui.LocalSnackbarHostState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onClose: () -> Unit,
    onOpenBatch: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionRequested = true
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::scanFromImage) }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Fresh scans buzz, and may open a website straight away when the user asked for that.
    LaunchedEffect(state.result?.parsed?.raw, state.result?.isNew) {
        val result = state.result ?: return@LaunchedEffect
        if (!result.isNew) return@LaunchedEffect
        Feedback.onScan(context, settings.vibrate, settings.sound)
        if (settings.autoOpenUrls && result.parsed.kind == ContentKind.URL) {
            result.parsed.actions.filterIsInstance<SmartAction.OpenUrl>().firstOrNull()?.let {
                ActionRunner.run(context, it)
            }
        }
        viewModel.markResultSeen()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            ScannerCamera(
                paused = state.isPaused,
                torchEnabled = state.torchEnabled,
                onDetected = viewModel::onDetected,
                onTorchAvailable = viewModel::setTorchAvailable,
                onError = viewModel::showMessage,
                modifier = Modifier.fillMaxSize()
            )
            ScannerOverlay(active = !state.isPaused)
        } else {
            PermissionPanel(
                requested = permissionRequested,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { ActionRunner.openAppSettings(context) },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        ScanTopBar(
            torchEnabled = state.torchEnabled,
            torchAvailable = state.torchAvailable && hasPermission,
            onClose = onClose,
            onToggleTorch = viewModel::toggleTorch,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.decodingImage) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                Spacer(Modifier.height(16.dp))
            } else if (hasPermission) {
                Text(
                    text = "Point the camera at any QR code or barcode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
                Spacer(Modifier.height(18.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassAction(
                    icon = Icons.Rounded.Image,
                    label = "Image",
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                GlassAction(
                    icon = Icons.Rounded.Dashboard,
                    label = "Batch",
                    onClick = onOpenBatch
                )
            }
        }
    }

    val result = state.result
    if (result != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissResult,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                ResultContent(
                    parsed = result.parsed,
                    format = result.format,
                    isFavorite = result.isFavorite,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onMessage = viewModel::showMessage
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = viewModel::dismissResult,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Scan again")
                }
            }
        }
    }
}

@Composable
private fun ScanTopBar(
    torchEnabled: Boolean,
    torchAvailable: Boolean,
    onClose: () -> Unit,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CircleGlassButton(icon = Icons.Rounded.Close, description = "Close", onClick = onClose)
        Text(
            text = "Scan",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        if (torchAvailable) {
            CircleGlassButton(
                icon = if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                description = "Flashlight",
                onClick = onToggleTorch,
                highlighted = torchEnabled
            )
        } else {
            Spacer(Modifier.size(44.dp))
        }
    }
}

@Composable
private fun CircleGlassButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (highlighted) Color.White else Color.Black.copy(alpha = 0.42f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (highlighted) Color.Black else Color.White,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun GlassAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Black.copy(alpha = 0.42f),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
    ) {
        Icon(icon, contentDescription = null, Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(label)
    }
}

@Composable
private fun PermissionPanel(
    requested: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.PhotoCamera,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Camera access needed",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Frames are analysed on this phone and never leave it.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        Button(onClick = onRequest) { Text("Allow camera") }
        if (requested) {
            TextButton(onClick = onOpenSettings) {
                Text("Open app settings", color = Color.White)
            }
        }
    }
}
