package com.kobe.qrbarcode.ui.scan

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private suspend fun Context.cameraProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }
}

/**
 * Live camera preview with ML Kit analysis attached. Analysis pauses without
 * tearing down the session, so resuming a scan is instant.
 */
@Composable
fun ScannerCamera(
    paused: Boolean,
    torchEnabled: Boolean,
    onDetected: (List<DetectedCode>) -> Unit,
    modifier: Modifier = Modifier,
    onTorchAvailable: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { ScannerFactory.create() }
    val pausedState = rememberUpdatedState(paused)
    val detectedCallback = rememberUpdatedState(onDetected)
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    LaunchedEffect(previewView) {
        runCatching {
            val provider = context.cameraProvider()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(
                        executor,
                        BarcodeAnalyzer(
                            scanner = scanner,
                            isActive = { !pausedState.value },
                            onDetected = { detectedCallback.value(it) }
                        )
                    )
                }
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
            onTorchAvailable(camera?.cameraInfo?.hasFlashUnit() == true)
        }.onFailure {
            onError("The camera could not be started")
        }
    }

    LaunchedEffect(torchEnabled, camera) {
        runCatching { camera?.cameraControl?.enableTorch(torchEnabled) }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
