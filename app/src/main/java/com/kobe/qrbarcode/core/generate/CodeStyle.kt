package com.kobe.qrbarcode.core.generate

/** Visual options shared by the QR and barcode generators. */
data class CodeStyle(
    val sizePx: Int = 1024,
    val foreground: Int = 0xFF101010.toInt(),
    val background: Int = 0xFFFFFFFF.toInt(),
    val margin: Int = 2,
    val errorCorrection: ErrorCorrection = ErrorCorrection.M,
    val showHumanReadableText: Boolean = true
)

enum class ErrorCorrection(val level: String, val label: String, val recovery: String) {
    L("L", "Low", "7%"),
    M("M", "Medium", "15%"),
    Q("Q", "Quartile", "25%"),
    H("H", "High", "30%");

    companion object {
        fun fromLevel(level: String?): ErrorCorrection =
            entries.firstOrNull { it.level == level } ?: M
    }
}
