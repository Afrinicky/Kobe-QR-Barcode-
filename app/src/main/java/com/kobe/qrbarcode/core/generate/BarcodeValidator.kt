package com.kobe.qrbarcode.core.generate

import com.kobe.qrbarcode.core.model.CodeFormat

/**
 * Format specific input rules for the barcode generator. Validation happens before
 * encoding so the user gets a readable message instead of an encoder exception.
 */
object BarcodeValidator {

    data class Result(
        val isValid: Boolean,
        val normalized: String,
        val message: String?
    )

    private const val CODE_39_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
    private const val CODABAR_ALPHABET = "0123456789-$:/.+"

    fun hint(format: CodeFormat): String = when (format) {
        CodeFormat.EAN_13 -> "12 digits (check digit added) or 13 digits"
        CodeFormat.EAN_8 -> "7 digits (check digit added) or 8 digits"
        CodeFormat.UPC_A -> "11 digits (check digit added) or 12 digits"
        CodeFormat.UPC_E -> "7 digits (check digit added) or 8 digits"
        CodeFormat.CODE_128 -> "Any text — letters, digits and symbols"
        CodeFormat.CODE_39 -> "A–Z, 0–9, space and - . $ / + %"
        CodeFormat.ITF -> "An even number of digits"
        CodeFormat.CODABAR -> "0–9 and - $ : / . +, optional A–D start/stop"
        else -> "Any text"
    }

    fun isNumericOnly(format: CodeFormat): Boolean = when (format) {
        CodeFormat.EAN_13, CodeFormat.EAN_8, CodeFormat.UPC_A, CodeFormat.UPC_E,
        CodeFormat.ITF -> true

        else -> false
    }

    fun validate(format: CodeFormat, rawInput: String): Result {
        val input = rawInput.trim()
        if (input.isEmpty()) return Result(false, input, "Enter the content to encode")

        return when (format) {
            CodeFormat.EAN_13 -> fixedLengthNumeric(input, 12, 13, "EAN-13")
            CodeFormat.EAN_8 -> fixedLengthNumeric(input, 7, 8, "EAN-8")
            CodeFormat.UPC_A -> fixedLengthNumeric(input, 11, 12, "UPC-A")
            CodeFormat.UPC_E -> upcE(input)
            CodeFormat.ITF -> itf(input)
            CodeFormat.CODE_39 -> alphabet(input.uppercase(), CODE_39_ALPHABET, "Code 39")
            CodeFormat.CODABAR -> codabar(input)
            CodeFormat.CODE_128 -> {
                if (input.any { it.code > 127 }) {
                    Result(false, input, "Code 128 supports ASCII characters only")
                } else {
                    Result(true, input, null)
                }
            }

            else -> Result(true, input, null)
        }
    }

    /** Standard modulo-10 check digit used by EAN/UPC. */
    fun checkDigit(payload: String): Int {
        var sum = 0
        var weight = 3
        for (index in payload.indices.reversed()) {
            sum += (payload[index] - '0') * weight
            weight = if (weight == 3) 1 else 3
        }
        return (10 - sum % 10) % 10
    }

    private fun fixedLengthNumeric(
        input: String,
        payloadLength: Int,
        fullLength: Int,
        label: String
    ): Result {
        if (!input.all { it.isDigit() }) {
            return Result(false, input, "$label accepts digits only")
        }
        return when (input.length) {
            payloadLength -> Result(true, input + checkDigit(input), null)
            fullLength -> {
                val expected = checkDigit(input.dropLast(1))
                if (expected != input.last().digitToInt()) {
                    Result(false, input, "Check digit should be $expected")
                } else {
                    Result(true, input, null)
                }
            }

            else -> Result(false, input, "$label needs $payloadLength or $fullLength digits")
        }
    }

    private fun upcE(input: String): Result {
        if (!input.all { it.isDigit() }) return Result(false, input, "UPC-E accepts digits only")
        return when (input.length) {
            // A 6 digit body is encoded with number system 0; the encoder adds the check digit.
            6 -> Result(true, "0$input", null)
            7, 8 -> Result(true, input, null)
            else -> Result(false, input, "UPC-E needs 7 or 8 digits")
        }
    }

    private fun itf(input: String): Result {
        if (!input.all { it.isDigit() }) return Result(false, input, "ITF accepts digits only")
        if (input.length % 2 != 0) {
            return Result(false, input, "ITF needs an even number of digits")
        }
        return Result(true, input, null)
    }

    private fun alphabet(input: String, alphabet: String, label: String): Result {
        val invalid = input.firstOrNull { it !in alphabet }
        return if (invalid != null) {
            Result(false, input, "$label cannot encode '$invalid'")
        } else {
            Result(true, input, null)
        }
    }

    private fun codabar(input: String): Result {
        val body = input.uppercase()
        val hasDelimiters = body.length > 2 &&
            body.first() in "ABCD" && body.last() in "ABCD"
        val payload = if (hasDelimiters) body.substring(1, body.length - 1) else body
        val invalid = payload.firstOrNull { it !in CODABAR_ALPHABET }
        if (invalid != null) return Result(false, input, "Codabar cannot encode '$invalid'")
        return Result(true, if (hasDelimiters) body else "A${payload}A", null)
    }
}
