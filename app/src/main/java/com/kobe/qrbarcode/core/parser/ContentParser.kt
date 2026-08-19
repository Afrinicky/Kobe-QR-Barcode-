package com.kobe.qrbarcode.core.parser

import com.kobe.qrbarcode.core.model.CodeFormat
import com.kobe.qrbarcode.core.model.ContactInfo
import com.kobe.qrbarcode.core.model.ContentKind
import com.kobe.qrbarcode.core.model.EventInfo
import com.kobe.qrbarcode.core.model.GeoInfo
import com.kobe.qrbarcode.core.model.ParsedContent
import com.kobe.qrbarcode.core.model.SmartAction
import com.kobe.qrbarcode.core.model.WifiInfo
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Turns a raw decoded payload into something the UI can present with useful actions.
 * Everything here is pure string work — no Android, no network.
 */
object ContentParser {

    private val URL_REGEX = Regex(
        "^(https?|ftp)://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$",
        RegexOption.IGNORE_CASE
    )
    private val BARE_DOMAIN_REGEX = Regex(
        "^(www\\.)?[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+" +
            "(?::\\d{2,5})?(?:[/?#][^\\s]*)?$",
        RegexOption.IGNORE_CASE
    )
    private val EMAIL_REGEX = Regex(
        "^[A-Z0-9._%+\\-]+@[A-Z0-9.\\-]+\\.[A-Z]{2,}$",
        RegexOption.IGNORE_CASE
    )
    private val PHONE_REGEX = Regex("^\\+?[0-9][0-9 ()\\-.]{5,19}$")

    fun parse(raw: String, format: CodeFormat = CodeFormat.QR_CODE): ParsedContent {
        val value = raw.trim()
        if (value.isEmpty()) {
            return ParsedContent(ContentKind.TEXT, ContentKind.TEXT.label, "", raw)
        }
        val upper = value.uppercase(Locale.ROOT)

        return when {
            upper.startsWith("WIFI:") -> parseWifi(value)
            upper.startsWith("BEGIN:VCARD") -> parseVCard(value)
            upper.startsWith("MECARD:") -> parseMeCard(value)
            upper.startsWith("BEGIN:VEVENT") || upper.startsWith("BEGIN:VCALENDAR") ->
                parseEvent(value)

            upper.startsWith("GEO:") -> parseGeo(value)
            upper.startsWith("TEL:") -> phone(value.substring(4).trim(), value)
            upper.startsWith("SMSTO:") || upper.startsWith("SMS:") -> parseSms(value)
            upper.startsWith("MAILTO:") -> parseMailto(value)
            upper.startsWith("MATMSG:") -> parseMatMsg(value)
            !format.isMatrix && format != CodeFormat.CODE_128 && format != CodeFormat.CODE_39 &&
                value.all { it.isDigit() } -> product(value, format)

            URL_REGEX.matches(value) -> url(value, value)
            EMAIL_REGEX.matches(value) -> email(value, "", "", value)
            PHONE_REGEX.matches(value) -> phone(value, value)
            BARE_DOMAIN_REGEX.matches(value) && value.contains('.') -> url("https://$value", value)
            else -> ParsedContent(
                kind = ContentKind.TEXT,
                title = ContentKind.TEXT.label,
                primary = value,
                raw = raw,
                fields = listOf("Characters" to value.length.toString())
            )
        }
    }

    // ---------------------------------------------------------------- builders

    private fun url(fullUrl: String, display: String) = ParsedContent(
        kind = ContentKind.URL,
        title = ContentKind.URL.label,
        primary = display.removePrefix("https://").removePrefix("http://").trimEnd('/'),
        raw = fullUrl,
        fields = listOf("Address" to fullUrl),
        actions = listOf(SmartAction.OpenUrl(fullUrl))
    )

    private fun phone(number: String, raw: String) = ParsedContent(
        kind = ContentKind.PHONE,
        title = ContentKind.PHONE.label,
        primary = number,
        raw = raw,
        actions = listOf(SmartAction.Call(number), SmartAction.SendSms(number))
    )

    private fun email(address: String, subject: String, body: String, raw: String) = ParsedContent(
        kind = ContentKind.EMAIL,
        title = ContentKind.EMAIL.label,
        primary = address,
        raw = raw,
        fields = buildList {
            if (subject.isNotBlank()) add("Subject" to subject)
            if (body.isNotBlank()) add("Message" to body)
        },
        actions = listOf(SmartAction.SendEmail(address, subject, body))
    )

    private fun product(code: String, format: CodeFormat) = ParsedContent(
        kind = ContentKind.PRODUCT,
        title = ContentKind.PRODUCT.label,
        primary = code,
        raw = code,
        fields = listOf("Symbology" to format.displayName, "Digits" to code.length.toString())
    )

    // ------------------------------------------------------------------ Wi-Fi

    private fun parseWifi(value: String): ParsedContent {
        val map = splitFields(value.substring(5))
        val wifi = WifiInfo(
            ssid = map["S"].orEmpty(),
            password = map["P"].orEmpty(),
            security = map["T"]?.uppercase(Locale.ROOT)?.ifBlank { "NOPASS" } ?: "NOPASS",
            hidden = map["H"].equals("true", ignoreCase = true)
        )
        val securityLabel = when (wifi.security) {
            "WPA", "WPA2", "WPA2-EAP", "WPA3" -> "WPA/WPA2"
            "WEP" -> "WEP"
            else -> "Open"
        }
        return ParsedContent(
            kind = ContentKind.WIFI,
            title = ContentKind.WIFI.label,
            primary = wifi.ssid,
            raw = value,
            fields = buildList {
                add("Network" to wifi.ssid)
                add("Security" to securityLabel)
                if (wifi.password.isNotBlank()) add("Password" to wifi.password)
                if (wifi.hidden) add("Hidden" to "Yes")
            },
            actions = listOf(SmartAction.ConnectWifi(wifi))
        )
    }

    // --------------------------------------------------------------- contacts

    private fun parseMeCard(value: String): ParsedContent {
        val map = splitFields(value.substring(7))
        val name = map["N"]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?.reversed()?.joinToString(" ").orEmpty()
        val contact = ContactInfo(
            name = name,
            organization = map["ORG"].orEmpty(),
            phones = listOfNotNull(map["TEL"], map["TEL2"]).filter { it.isNotBlank() },
            emails = listOfNotNull(map["EMAIL"]).filter { it.isNotBlank() },
            address = map["ADR"].orEmpty().replace(',', ' ').trim(),
            website = map["URL"].orEmpty(),
            note = map["NOTE"].orEmpty()
        )
        return contactContent(contact, value)
    }

    private fun parseVCard(value: String): ParsedContent {
        val lines = unfold(value)
        var name = ""
        var structuredName = ""
        var org = ""
        var title = ""
        val phones = mutableListOf<String>()
        val emails = mutableListOf<String>()
        var address = ""
        var website = ""
        var note = ""

        for (line in lines) {
            val separator = line.indexOf(':')
            if (separator <= 0) continue
            val key = line.substring(0, separator).uppercase(Locale.ROOT)
            val data = decodeVCardValue(line.substring(separator + 1).trim())
            val name0 = key.substringBefore(';')
            when (name0) {
                "FN" -> name = data
                "N" -> structuredName = data.split(';').map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .let { parts ->
                        if (parts.size >= 2) "${parts[1]} ${parts[0]}" else parts.joinToString(" ")
                    }

                "ORG" -> org = data.replace(';', ' ').trim()
                "TITLE" -> title = data
                "TEL" -> if (data.isNotBlank()) phones += data
                "EMAIL" -> if (data.isNotBlank()) emails += data
                "ADR" -> address = data.split(';').filter { it.isNotBlank() }.joinToString(", ")
                "URL" -> website = data
                "NOTE" -> note = data
            }
        }
        val contact = ContactInfo(
            name = name.ifBlank { structuredName },
            organization = org,
            jobTitle = title,
            phones = phones.distinct(),
            emails = emails.distinct(),
            address = address,
            website = website,
            note = note
        )
        return contactContent(contact, value)
    }

    private fun contactContent(contact: ContactInfo, raw: String): ParsedContent {
        val actions = buildList {
            add(SmartAction.AddContact(contact))
            contact.phones.firstOrNull()?.let {
                add(SmartAction.Call(it))
                add(SmartAction.SendSms(it))
            }
            contact.emails.firstOrNull()?.let { add(SmartAction.SendEmail(it)) }
            if (contact.website.isNotBlank()) add(SmartAction.OpenUrl(normalizeUrl(contact.website)))
        }
        return ParsedContent(
            kind = ContentKind.CONTACT,
            title = ContentKind.CONTACT.label,
            primary = contact.name.ifBlank { contact.organization.ifBlank { "Contact card" } },
            raw = raw,
            fields = buildList {
                if (contact.jobTitle.isNotBlank()) add("Title" to contact.jobTitle)
                if (contact.organization.isNotBlank()) add("Organization" to contact.organization)
                contact.phones.forEachIndexed { index, phone ->
                    add((if (index == 0) "Phone" else "Phone ${index + 1}") to phone)
                }
                contact.emails.forEachIndexed { index, mail ->
                    add((if (index == 0) "Email" else "Email ${index + 1}") to mail)
                }
                if (contact.address.isNotBlank()) add("Address" to contact.address)
                if (contact.website.isNotBlank()) add("Website" to contact.website)
                if (contact.note.isNotBlank()) add("Note" to contact.note)
            },
            actions = actions
        )
    }

    // ----------------------------------------------------------------- events

    private fun parseEvent(value: String): ParsedContent {
        val lines = unfold(value)
        var title = ""
        var description = ""
        var location = ""
        var start = 0L
        var end = 0L
        var allDay = false

        for (line in lines) {
            val separator = line.indexOf(':')
            if (separator <= 0) continue
            val key = line.substring(0, separator).uppercase(Locale.ROOT)
            val data = decodeVCardValue(line.substring(separator + 1).trim())
            when (key.substringBefore(';')) {
                "SUMMARY" -> title = data
                "DESCRIPTION" -> description = data
                "LOCATION" -> location = data
                "DTSTART" -> {
                    start = parseICalDate(data)
                    allDay = data.length == 8
                }

                "DTEND" -> end = parseICalDate(data)
            }
        }
        val event = EventInfo(title, description, location, start, end, allDay)
        return ParsedContent(
            kind = ContentKind.EVENT,
            title = ContentKind.EVENT.label,
            primary = event.title.ifBlank { "Calendar event" },
            raw = value,
            fields = buildList {
                if (start > 0) add("Starts" to formatDisplayDate(start, allDay))
                if (end > 0) add("Ends" to formatDisplayDate(end, allDay))
                if (location.isNotBlank()) add("Location" to location)
                if (description.isNotBlank()) add("Details" to description)
            },
            actions = buildList {
                add(SmartAction.AddEvent(event))
                if (location.isNotBlank()) {
                    add(SmartAction.OpenMap(GeoInfo(0.0, 0.0, location), "Find Location"))
                }
            }
        )
    }

    // -------------------------------------------------------------- geo / sms

    private fun parseGeo(value: String): ParsedContent {
        val payload = value.substring(4)
        val coordinatePart = payload.substringBefore('?')
        val query = payload.substringAfter("q=", "").substringBefore('&')
            .replace('+', ' ').let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        val pieces = coordinatePart.split(',')
        val lat = pieces.getOrNull(0)?.toDoubleOrNull() ?: 0.0
        val lng = pieces.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        val geo = GeoInfo(lat, lng, query)
        return ParsedContent(
            kind = ContentKind.LOCATION,
            title = ContentKind.LOCATION.label,
            primary = query.ifBlank { "%.6f, %.6f".format(Locale.US, lat, lng) },
            raw = value,
            fields = buildList {
                add("Latitude" to "%.6f".format(Locale.US, lat))
                add("Longitude" to "%.6f".format(Locale.US, lng))
                if (query.isNotBlank()) add("Place" to query)
            },
            actions = listOf(SmartAction.OpenMap(geo))
        )
    }

    private fun parseSms(value: String): ParsedContent {
        val payload = value.substringAfter(':')
        var number: String
        var body = ""
        if (payload.contains('?')) {
            number = payload.substringBefore('?')
            body = payload.substringAfter("body=", "").substringBefore('&')
                .replace('+', ' ')
                .let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        } else {
            val parts = payload.split(':')
            number = parts.getOrElse(0) { "" }
            body = parts.drop(1).joinToString(":")
        }
        number = number.trim()
        return ParsedContent(
            kind = ContentKind.SMS,
            title = ContentKind.SMS.label,
            primary = number,
            raw = value,
            fields = buildList { if (body.isNotBlank()) add("Message" to body) },
            actions = listOf(SmartAction.SendSms(number, body), SmartAction.Call(number))
        )
    }

    private fun parseMailto(value: String): ParsedContent {
        val payload = value.substring(7)
        val address = payload.substringBefore('?')
        val queryString = payload.substringAfter('?', "")
        val params = queryString.split('&').mapNotNull {
            val key = it.substringBefore('=', "")
            if (key.isBlank()) return@mapNotNull null
            val decoded = runCatching {
                java.net.URLDecoder.decode(it.substringAfter('=', ""), "UTF-8")
            }.getOrDefault(it.substringAfter('=', ""))
            key.lowercase(Locale.ROOT) to decoded
        }.toMap()
        return email(address, params["subject"].orEmpty(), params["body"].orEmpty(), value)
    }

    private fun parseMatMsg(value: String): ParsedContent {
        val map = splitFields(value.substring(7))
        return email(map["TO"].orEmpty(), map["SUB"].orEmpty(), map["BODY"].orEmpty(), value)
    }

    // ---------------------------------------------------------------- helpers

    /** Splits `KEY:value;KEY:value;;` payloads, honouring backslash escapes. */
    private fun splitFields(payload: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        val current = StringBuilder()
        var index = 0
        val segments = mutableListOf<String>()
        while (index < payload.length) {
            val char = payload[index]
            when {
                char == '\\' && index + 1 < payload.length -> {
                    current.append(payload[index + 1]); index++
                }

                char == ';' -> {
                    segments += current.toString(); current.setLength(0)
                }

                else -> current.append(char)
            }
            index++
        }
        if (current.isNotEmpty()) segments += current.toString()

        for (segment in segments) {
            if (segment.isBlank()) continue
            val separator = segment.indexOf(':')
            if (separator <= 0) continue
            result[segment.substring(0, separator).uppercase(Locale.ROOT)] =
                segment.substring(separator + 1)
        }
        return result
    }

    /** vCard/iCal lines may be folded onto continuation lines starting with whitespace. */
    private fun unfold(value: String): List<String> {
        val out = mutableListOf<String>()
        value.split('\n').forEach { rawLine ->
            val line = rawLine.trimEnd('\r')
            if ((line.startsWith(" ") || line.startsWith("\t")) && out.isNotEmpty()) {
                out[out.lastIndex] = out.last() + line.trimStart()
            } else {
                out += line
            }
        }
        return out.filter { it.isNotBlank() }
    }

    private fun decodeVCardValue(value: String): String =
        value.replace("\\n", "\n").replace("\\N", "\n")
            .replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

    private fun parseICalDate(value: String): Long {
        val cleaned = value.trim()
        val patterns = listOf(
            "yyyyMMdd'T'HHmmss'Z'" to true,
            "yyyyMMdd'T'HHmmss" to false,
            "yyyyMMdd" to false
        )
        for ((pattern, utc) in patterns) {
            val expectedLength = pattern.count { it != '\'' }
            if (cleaned.length != expectedLength) continue
            val format = SimpleDateFormat(pattern, Locale.US)
            if (utc) format.timeZone = TimeZone.getTimeZone("UTC")
            runCatching { return format.parse(cleaned)?.time ?: 0L }
        }
        return 0L
    }

    private fun formatDisplayDate(millis: Long, allDay: Boolean): String {
        val pattern = if (allDay) "EEE, d MMM yyyy" else "EEE, d MMM yyyy · HH:mm"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(java.util.Date(millis))
    }

    fun normalizeUrl(value: String): String =
        if (value.startsWith("http://", true) || value.startsWith("https://", true)) value
        else "https://$value"
}
