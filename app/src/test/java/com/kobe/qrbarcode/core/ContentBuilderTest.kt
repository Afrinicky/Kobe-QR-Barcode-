package com.kobe.qrbarcode.core

import com.kobe.qrbarcode.core.model.ContentKind
import com.kobe.qrbarcode.core.model.SmartAction
import com.kobe.qrbarcode.core.model.WifiInfo
import com.kobe.qrbarcode.core.parser.ContentBuilder
import com.kobe.qrbarcode.core.parser.ContentParser
import com.kobe.qrbarcode.ui.generate.QrContentType
import org.junit.Assert.assertEquals
import org.junit.Test

/** What the generator writes, the scanner has to be able to read back. */
class ContentBuilderTest {

    @Test
    fun `wifi round trips through the parser`() {
        val payload = ContentBuilder.wifi(
            WifiInfo(ssid = "Kobe; Guest", password = "let,me:in", security = "WPA")
        )
        val parsed = ContentParser.parse(payload)
        val action = parsed.actions.filterIsInstance<SmartAction.ConnectWifi>().single()
        assertEquals("Kobe; Guest", action.wifi.ssid)
        assertEquals("let,me:in", action.wifi.password)
    }

    @Test
    fun `contact form round trips through the parser`() {
        val payload = QrContentType.CONTACT.build(
            mapOf(
                "name" to "Ama Mensah",
                "phone" to "+233201234567",
                "email" to "ama@kobe.app",
                "organization" to "Kobe Labs"
            )
        )
        val parsed = ContentParser.parse(payload)
        assertEquals(ContentKind.CONTACT, parsed.kind)
        val contact = parsed.actions.filterIsInstance<SmartAction.AddContact>().single().contact
        assertEquals("Ama Mensah", contact.name)
        assertEquals("Kobe Labs", contact.organization)
        assertEquals(listOf("+233201234567"), contact.phones)
    }

    @Test
    fun `url form adds the missing scheme`() {
        val payload = QrContentType.URL.build(mapOf("url" to "kobe.app/download"))
        assertEquals("https://kobe.app/download", payload)
    }

    @Test
    fun `social form builds the platform link`() {
        val payload = QrContentType.SOCIAL.build(
            mapOf("platform" to "Instagram", "handle" to "@kobeapps")
        )
        assertEquals("https://instagram.com/kobeapps", payload)
    }

    @Test
    fun `required fields decide whether a form is complete`() {
        val incomplete = mapOf("ssid" to "", "password" to "x")
        val complete = mapOf("ssid" to "Kobe", "password" to "x")
        assertEquals(false, QrContentType.WIFI.isComplete(incomplete))
        assertEquals(true, QrContentType.WIFI.isComplete(complete))
    }
}
