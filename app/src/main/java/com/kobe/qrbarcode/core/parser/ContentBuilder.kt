package com.kobe.qrbarcode.core.parser

import com.kobe.qrbarcode.core.model.ContactInfo
import com.kobe.qrbarcode.core.model.EventInfo
import com.kobe.qrbarcode.core.model.WifiInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Builds the standard payload strings the QR generator encodes. */
object ContentBuilder {

    fun url(value: String): String = ContentParser.normalizeUrl(value.trim())

    fun phone(number: String): String = "tel:${number.trim()}"

    fun email(address: String, subject: String, body: String): String = buildString {
        append("mailto:").append(address.trim())
        val params = buildList {
            if (subject.isNotBlank()) add("subject=" + encode(subject))
            if (body.isNotBlank()) add("body=" + encode(body))
        }
        if (params.isNotEmpty()) append('?').append(params.joinToString("&"))
    }

    fun sms(number: String, message: String): String =
        if (message.isBlank()) "smsto:${number.trim()}"
        else "smsto:${number.trim()}:$message"

    fun wifi(info: WifiInfo): String = buildString {
        append("WIFI:")
        append("T:").append(if (info.security.isBlank()) "nopass" else info.security).append(';')
        append("S:").append(escape(info.ssid)).append(';')
        if (info.password.isNotBlank() && !info.security.equals("nopass", true)) {
            append("P:").append(escape(info.password)).append(';')
        }
        if (info.hidden) append("H:true;")
        append(';')
    }

    fun geo(latitude: Double, longitude: Double, label: String): String = buildString {
        append("geo:").append(format(latitude)).append(',').append(format(longitude))
        if (label.isNotBlank()) append("?q=").append(encode(label))
    }

    fun vCard(contact: ContactInfo): String = buildString {
        append("BEGIN:VCARD\nVERSION:3.0\n")
        if (contact.name.isNotBlank()) {
            val parts = contact.name.trim().split(" ")
            val last = if (parts.size > 1) parts.last() else contact.name.trim()
            val first = if (parts.size > 1) parts.dropLast(1).joinToString(" ") else ""
            append("N:").append(escapeVCard(last)).append(';').append(escapeVCard(first))
                .append(";;;\n")
            append("FN:").append(escapeVCard(contact.name)).append('\n')
        }
        if (contact.organization.isNotBlank()) {
            append("ORG:").append(escapeVCard(contact.organization)).append('\n')
        }
        if (contact.jobTitle.isNotBlank()) {
            append("TITLE:").append(escapeVCard(contact.jobTitle)).append('\n')
        }
        contact.phones.filter { it.isNotBlank() }.forEach {
            append("TEL;TYPE=CELL:").append(it.trim()).append('\n')
        }
        contact.emails.filter { it.isNotBlank() }.forEach {
            append("EMAIL;TYPE=INTERNET:").append(it.trim()).append('\n')
        }
        if (contact.address.isNotBlank()) {
            append("ADR:;;").append(escapeVCard(contact.address)).append(";;;;\n")
        }
        if (contact.website.isNotBlank()) {
            append("URL:").append(ContentParser.normalizeUrl(contact.website)).append('\n')
        }
        if (contact.note.isNotBlank()) {
            append("NOTE:").append(escapeVCard(contact.note)).append('\n')
        }
        append("END:VCARD")
    }

    fun event(info: EventInfo): String = buildString {
        append("BEGIN:VEVENT\n")
        if (info.title.isNotBlank()) append("SUMMARY:").append(escapeVCard(info.title)).append('\n')
        if (info.location.isNotBlank()) {
            append("LOCATION:").append(escapeVCard(info.location)).append('\n')
        }
        if (info.description.isNotBlank()) {
            append("DESCRIPTION:").append(escapeVCard(info.description)).append('\n')
        }
        if (info.startMillis > 0) {
            append("DTSTART:").append(formatICal(info.startMillis, info.allDay)).append('\n')
        }
        if (info.endMillis > 0) {
            append("DTEND:").append(formatICal(info.endMillis, info.allDay)).append('\n')
        }
        append("END:VEVENT")
    }

    private fun formatICal(millis: Long, allDay: Boolean): String {
        val pattern = if (allDay) "yyyyMMdd" else "yyyyMMdd'T'HHmmss'Z'"
        val formatter = SimpleDateFormat(pattern, Locale.US)
        if (!allDay) formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(millis))
    }

    private fun format(value: Double) = "%.6f".format(Locale.US, value)

    private fun encode(value: String) =
        runCatching { java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20") }
            .getOrDefault(value)

    private fun escape(value: String) =
        value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
            .replace(":", "\\:").replace("\"", "\\\"")

    private fun escapeVCard(value: String) =
        value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
            .replace("\n", "\\n")
}
