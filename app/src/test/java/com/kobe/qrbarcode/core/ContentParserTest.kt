package com.kobe.qrbarcode.core

import com.kobe.qrbarcode.core.model.CodeFormat
import com.kobe.qrbarcode.core.model.ContentKind
import com.kobe.qrbarcode.core.model.SmartAction
import com.kobe.qrbarcode.core.parser.ContentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentParserTest {

    @Test
    fun `https url is recognised and shown without its scheme`() {
        val parsed = ContentParser.parse("https://example.com/page")
        assertEquals(ContentKind.URL, parsed.kind)
        assertEquals("example.com/page", parsed.primary)
        assertTrue(parsed.actions.any { it is SmartAction.OpenUrl })
    }

    @Test
    fun `bare domain becomes an https link`() {
        val parsed = ContentParser.parse("www.kobe.app")
        assertEquals(ContentKind.URL, parsed.kind)
        assertEquals("https://www.kobe.app", parsed.raw)
    }

    @Test
    fun `phone numbers offer call and sms`() {
        val parsed = ContentParser.parse("+233201234567")
        assertEquals(ContentKind.PHONE, parsed.kind)
        assertTrue(parsed.actions.any { it is SmartAction.Call })
        assertTrue(parsed.actions.any { it is SmartAction.SendSms })
    }

    @Test
    fun `wifi payload exposes the network details`() {
        val parsed = ContentParser.parse("WIFI:T:WPA;S:HomeWiFi;P:secret123;H:false;;")
        assertEquals(ContentKind.WIFI, parsed.kind)
        assertEquals("HomeWiFi", parsed.primary)
        val action = parsed.actions.filterIsInstance<SmartAction.ConnectWifi>().single()
        assertEquals("secret123", action.wifi.password)
    }

    @Test
    fun `wifi payload honours escaped separators`() {
        val parsed = ContentParser.parse("WIFI:S:Cafe\\; Bar;T:WPA;P:pa\\:ss;;")
        val action = parsed.actions.filterIsInstance<SmartAction.ConnectWifi>().single()
        assertEquals("Cafe; Bar", action.wifi.ssid)
        assertEquals("pa:ss", action.wifi.password)
    }

    @Test
    fun `vcard produces a contact with phone and email`() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            N:Mensah;Ama;;;
            FN:Ama Mensah
            ORG:Kobe Labs
            TEL;TYPE=CELL:+233201234567
            EMAIL;TYPE=INTERNET:ama@kobe.app
            END:VCARD
        """.trimIndent()
        val parsed = ContentParser.parse(vcard)
        assertEquals(ContentKind.CONTACT, parsed.kind)
        assertEquals("Ama Mensah", parsed.primary)
        val action = parsed.actions.filterIsInstance<SmartAction.AddContact>().single()
        assertEquals(listOf("+233201234567"), action.contact.phones)
        assertEquals(listOf("ama@kobe.app"), action.contact.emails)
    }

    @Test
    fun `mailto carries subject and body`() {
        val parsed = ContentParser.parse("mailto:hi@kobe.app?subject=Hello&body=Test%20message")
        assertEquals(ContentKind.EMAIL, parsed.kind)
        val action = parsed.actions.filterIsInstance<SmartAction.SendEmail>().single()
        assertEquals("Hello", action.subject)
        assertEquals("Test message", action.body)
    }

    @Test
    fun `geo payload keeps its coordinates`() {
        val parsed = ContentParser.parse("geo:5.603700,-0.187000?q=Accra")
        assertEquals(ContentKind.LOCATION, parsed.kind)
        val action = parsed.actions.filterIsInstance<SmartAction.OpenMap>().single()
        assertEquals(5.6037, action.geo.latitude, 0.0001)
        assertEquals(-0.187, action.geo.longitude, 0.0001)
        assertEquals("Accra", action.geo.label)
    }

    @Test
    fun `smsto keeps the message body`() {
        val parsed = ContentParser.parse("smsto:+233201234567:On my way")
        assertEquals(ContentKind.SMS, parsed.kind)
        val action = parsed.actions.filterIsInstance<SmartAction.SendSms>().single()
        assertEquals("On my way", action.body)
    }

    @Test
    fun `a scanned ean13 reads as a product, not as a phone number`() {
        val parsed = ContentParser.parse("5901234123457", CodeFormat.EAN_13)
        assertEquals(ContentKind.PRODUCT, parsed.kind)
    }

    @Test
    fun `plain text falls through to the text kind`() {
        val parsed = ContentParser.parse("Shelf B, row 4")
        assertEquals(ContentKind.TEXT, parsed.kind)
        assertTrue(parsed.actions.isEmpty())
    }
}
