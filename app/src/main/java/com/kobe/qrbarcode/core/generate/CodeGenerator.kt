package com.kobe.qrbarcode.core.generate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.kobe.qrbarcode.core.model.CodeFormat

/**
 * Renders payloads to bitmaps entirely on device. No network, no rendering service.
 */
object CodeGenerator {

    /** Encoding failed — the message is safe to show to the user. */
    class EncodeException(message: String) : Exception(message)

    fun toZXing(format: CodeFormat): BarcodeFormat? = when (format) {
        CodeFormat.QR_CODE -> BarcodeFormat.QR_CODE
        CodeFormat.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
        CodeFormat.AZTEC -> BarcodeFormat.AZTEC
        CodeFormat.PDF417 -> BarcodeFormat.PDF_417
        CodeFormat.EAN_13 -> BarcodeFormat.EAN_13
        CodeFormat.EAN_8 -> BarcodeFormat.EAN_8
        CodeFormat.UPC_A -> BarcodeFormat.UPC_A
        CodeFormat.UPC_E -> BarcodeFormat.UPC_E
        CodeFormat.CODE_128 -> BarcodeFormat.CODE_128
        CodeFormat.CODE_39 -> BarcodeFormat.CODE_39
        CodeFormat.CODE_93 -> BarcodeFormat.CODE_93
        CodeFormat.ITF -> BarcodeFormat.ITF
        CodeFormat.CODABAR -> BarcodeFormat.CODABAR
        CodeFormat.UNKNOWN -> null
    }

    /**
     * @param logo optional bitmap drawn in the middle of a QR code.
     * @throws EncodeException when the payload cannot be encoded in [format].
     */
    fun generate(
        content: String,
        format: CodeFormat,
        style: CodeStyle = CodeStyle(),
        logo: Bitmap? = null
    ): Bitmap {
        if (content.isEmpty()) throw EncodeException("Nothing to encode")
        val zxingFormat = toZXing(format)
            ?: throw EncodeException("${format.displayName} cannot be generated")

        return if (format.isMatrix) {
            matrixBitmap(content, zxingFormat, style, logo)
        } else {
            linearBitmap(content, zxingFormat, format, style)
        }
    }

    // ------------------------------------------------------------------ 2D

    private fun matrixBitmap(
        content: String,
        format: BarcodeFormat,
        style: CodeStyle,
        logo: Bitmap?
    ): Bitmap {
        val effectiveCorrection =
            if (logo != null && style.errorCorrection.ordinal < ErrorCorrection.H.ordinal) {
                ErrorCorrection.H
            } else {
                style.errorCorrection
            }
        val hints = mutableMapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to style.margin
        )
        if (format == BarcodeFormat.QR_CODE) {
            hints[EncodeHintType.ERROR_CORRECTION] =
                ErrorCorrectionLevel.valueOf(effectiveCorrection.level)
        }
        val size = style.sizePx.coerceIn(256, 2048)
        val matrix = encode(content, format, size, size, hints)
        val bitmap = matrix.toBitmap(style.foreground, style.background)
        return if (logo != null) bitmap.withLogo(logo, style.background) else bitmap
    }

    // ------------------------------------------------------------------ 1D

    private fun linearBitmap(
        content: String,
        format: BarcodeFormat,
        codeFormat: CodeFormat,
        style: CodeStyle
    ): Bitmap {
        val width = style.sizePx.coerceIn(256, 2048)
        val barHeight = (width * 0.42f).toInt()
        val hints = mapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to style.margin.coerceAtLeast(4))
        val matrix = encode(content, format, width, barHeight, hints)
        val bars = matrix.toBitmap(style.foreground, style.background)
        if (!style.showHumanReadableText) return bars

        val textSize = width * 0.085f
        val captionHeight = (textSize * 1.7f).toInt()
        val output = Bitmap.createBitmap(bars.width, bars.height + captionHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(style.background)
        canvas.drawBitmap(bars, 0f, 0f, null)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.foreground
            this.textSize = textSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val label = spacedLabel(content, codeFormat)
        canvas.drawText(
            label,
            output.width / 2f,
            bars.height + textSize * 1.15f,
            paint
        )
        bars.recycle()
        return output
    }

    /** EAN/UPC digits read better in their conventional groups. */
    private fun spacedLabel(content: String, format: CodeFormat): String = when (format) {
        CodeFormat.EAN_13 -> if (content.length == 13) {
            "${content[0]}  ${content.substring(1, 7)}  ${content.substring(7)}"
        } else content

        CodeFormat.UPC_A -> if (content.length == 12) {
            "${content[0]}  ${content.substring(1, 6)}  ${content.substring(6, 11)}  ${content.last()}"
        } else content

        CodeFormat.EAN_8 -> if (content.length == 8) {
            "${content.substring(0, 4)}  ${content.substring(4)}"
        } else content

        else -> content
    }

    // -------------------------------------------------------------- plumbing

    private fun encode(
        content: String,
        format: BarcodeFormat,
        width: Int,
        height: Int,
        hints: Map<EncodeHintType, Any>
    ): BitMatrix = try {
        MultiFormatWriter().encode(content, format, width, height, hints)
    } catch (error: Exception) {
        throw EncodeException(friendlyMessage(error))
    }

    private fun friendlyMessage(error: Exception): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("Data too big", true) ||
                message.contains("data too large", true) -> "That content is too long for this code"

            message.contains("Requested contents should be", true) ||
                message.contains("digits", true) -> message

            message.isBlank() -> "This content cannot be encoded in that format"
            else -> message
        }
    }

    private fun BitMatrix.toBitmap(foreground: Int, background: Int): Bitmap {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (this[x, y]) foreground else background
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun Bitmap.withLogo(logo: Bitmap, background: Int): Bitmap {
        val output = copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val target = (output.width * 0.2f).toInt().coerceAtLeast(24)
        val padding = target * 0.12f
        val left = (output.width - target) / 2f
        val top = (output.height - target) / 2f
        val plate = RectF(
            left - padding,
            top - padding,
            left + target + padding,
            top + target + padding
        )
        val radius = target * 0.18f
        canvas.drawRoundRect(
            plate,
            radius,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (Color.alpha(background) == 0) Color.WHITE else background
            }
        )
        val scaled = Bitmap.createScaledBitmap(logo, target, target, true)
        canvas.drawBitmap(
            scaled,
            Rect(0, 0, scaled.width, scaled.height),
            RectF(left, top, left + target, top + target),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        if (scaled != logo) scaled.recycle()
        return output
    }
}
