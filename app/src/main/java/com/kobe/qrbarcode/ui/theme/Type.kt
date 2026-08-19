package com.kobe.qrbarcode.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val default = Typography()

val KobeTypography = Typography(
    displaySmall = default.displaySmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = default.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = default.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = default.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = default.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyMedium = default.bodyMedium.copy(lineHeight = 21.sp)
)

/** Monospace style for payloads, so 0/O and 1/l stay distinguishable. */
val PayloadTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
    lineHeight = 21.sp
)
