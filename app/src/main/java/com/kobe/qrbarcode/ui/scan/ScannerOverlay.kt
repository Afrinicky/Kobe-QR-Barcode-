package com.kobe.qrbarcode.ui.scan

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Dims everything outside the scan window, draws the corner brackets and the
 * sweeping indicator line.
 */
@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    accent: Color = Color.White,
    windowFraction: Float = 0.74f
) {
    val transition = rememberInfiniteTransition(label = "scan-sweep")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val side = minOf(size.width, size.height) * windowFraction
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f - size.height * 0.04f
        val radius = 26.dp.toPx()

        drawRect(Color.Black.copy(alpha = 0.58f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(radius, radius),
            blendMode = BlendMode.Clear
        )

        val bracket = side * 0.17f
        val right = left + side
        val bottom = top + side
        val path = Path().apply {
            moveTo(left, top + bracket)
            lineTo(left, top + radius)
            quadraticBezierTo(left, top, left + radius, top)
            lineTo(left + bracket, top)

            moveTo(right - bracket, top)
            lineTo(right - radius, top)
            quadraticBezierTo(right, top, right, top + radius)
            lineTo(right, top + bracket)

            moveTo(right, bottom - bracket)
            lineTo(right, bottom - radius)
            quadraticBezierTo(right, bottom, right - radius, bottom)
            lineTo(right - bracket, bottom)

            moveTo(left + bracket, bottom)
            lineTo(left + radius, bottom)
            quadraticBezierTo(left, bottom, left, bottom - radius)
            lineTo(left, bottom - bracket)
        }
        drawPath(
            path = path,
            color = accent,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )

        if (active) {
            val lineY = top + side * (0.08f + 0.84f * sweep)
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, accent.copy(alpha = 0.9f), Color.Transparent)
                ),
                start = Offset(left + 12.dp.toPx(), lineY),
                end = Offset(right - 12.dp.toPx(), lineY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
