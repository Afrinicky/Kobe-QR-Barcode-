package com.kobe.qrbarcode.core

import com.kobe.qrbarcode.core.generate.BarcodeValidator
import com.kobe.qrbarcode.core.model.CodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeValidatorTest {

    @Test
    fun `ean13 appends the check digit to a 12 digit payload`() {
        val result = BarcodeValidator.validate(CodeFormat.EAN_13, "590123412345")
        assertTrue(result.isValid)
        assertEquals("5901234123457", result.normalized)
    }

    @Test
    fun `ean13 accepts a correct 13 digit code`() {
        val result = BarcodeValidator.validate(CodeFormat.EAN_13, "5901234123457")
        assertTrue(result.isValid)
        assertEquals("5901234123457", result.normalized)
    }

    @Test
    fun `ean13 rejects a wrong check digit and names the right one`() {
        val result = BarcodeValidator.validate(CodeFormat.EAN_13, "5901234123450")
        assertFalse(result.isValid)
        assertEquals("Check digit should be 7", result.message)
    }

    @Test
    fun `upc a computes its check digit`() {
        val result = BarcodeValidator.validate(CodeFormat.UPC_A, "03600029145")
        assertTrue(result.isValid)
        assertEquals("036000291452", result.normalized)
    }

    @Test
    fun `ean8 computes its check digit`() {
        val result = BarcodeValidator.validate(CodeFormat.EAN_8, "9638507")
        assertTrue(result.isValid)
        assertEquals("96385074", result.normalized)
    }

    @Test
    fun `itf needs an even number of digits`() {
        assertFalse(BarcodeValidator.validate(CodeFormat.ITF, "12345").isValid)
        assertTrue(BarcodeValidator.validate(CodeFormat.ITF, "123456").isValid)
    }

    @Test
    fun `code39 rejects characters outside its alphabet`() {
        assertFalse(BarcodeValidator.validate(CodeFormat.CODE_39, "hello!").isValid)
        assertTrue(BarcodeValidator.validate(CodeFormat.CODE_39, "KOBE-123").isValid)
    }

    @Test
    fun `codabar wraps a bare payload in start and stop characters`() {
        val result = BarcodeValidator.validate(CodeFormat.CODABAR, "12345")
        assertTrue(result.isValid)
        assertEquals("A12345A", result.normalized)
    }

    @Test
    fun `code128 rejects non ascii input`() {
        assertFalse(BarcodeValidator.validate(CodeFormat.CODE_128, "café").isValid)
        assertTrue(BarcodeValidator.validate(CodeFormat.CODE_128, "KOBE-2026").isValid)
    }
}
