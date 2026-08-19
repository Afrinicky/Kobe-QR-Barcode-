package com.kobe.qrbarcode.ui.scan

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kobe.qrbarcode.core.model.CodeFormat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** A code the camera (or an image) produced. */
data class DetectedCode(val content: String, val format: CodeFormat)

object ScannerFactory {

    /** Every symbology the app advertises, bundled on device. */
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_PDF417
        )
        .build()

    fun create(): BarcodeScanner = BarcodeScanning.getClient(options)
}

fun Barcode.toCodeFormat(): CodeFormat = when (format) {
    Barcode.FORMAT_QR_CODE -> CodeFormat.QR_CODE
    Barcode.FORMAT_EAN_13 -> CodeFormat.EAN_13
    Barcode.FORMAT_EAN_8 -> CodeFormat.EAN_8
    Barcode.FORMAT_UPC_A -> CodeFormat.UPC_A
    Barcode.FORMAT_UPC_E -> CodeFormat.UPC_E
    Barcode.FORMAT_CODE_128 -> CodeFormat.CODE_128
    Barcode.FORMAT_CODE_39 -> CodeFormat.CODE_39
    Barcode.FORMAT_CODE_93 -> CodeFormat.CODE_93
    Barcode.FORMAT_ITF -> CodeFormat.ITF
    Barcode.FORMAT_CODABAR -> CodeFormat.CODABAR
    Barcode.FORMAT_DATA_MATRIX -> CodeFormat.DATA_MATRIX
    Barcode.FORMAT_AZTEC -> CodeFormat.AZTEC
    Barcode.FORMAT_PDF417 -> CodeFormat.PDF417
    else -> CodeFormat.UNKNOWN
}

fun Barcode.toDetectedCode(): DetectedCode? {
    val value = rawValue ?: displayValue ?: return null
    if (value.isBlank()) return null
    return DetectedCode(value, toCodeFormat())
}

/** Feeds camera frames to ML Kit while [isActive] returns true. */
class BarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val isActive: () -> Boolean,
    private val onDetected: (List<DetectedCode>) -> Unit
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        if (!isActive()) {
            proxy.close()
            return
        }
        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val detected = barcodes.mapNotNull { it.toDetectedCode() }
                if (detected.isNotEmpty()) onDetected(detected)
            }
            .addOnCompleteListener { proxy.close() }
    }
}

/** Decodes every code present in a picked image. */
suspend fun scanImage(context: Context, uri: Uri): List<DetectedCode> =
    suspendCancellableCoroutine { continuation ->
        val scanner = ScannerFactory.create()
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (error: Exception) {
            scanner.close()
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                continuation.resume(barcodes.mapNotNull { it.toDetectedCode() })
            }
            .addOnFailureListener { continuation.resume(emptyList()) }
            .addOnCompleteListener { scanner.close() }
    }
