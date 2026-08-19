package com.kobe.qrbarcode.ui.generate

import com.kobe.qrbarcode.core.model.ContactInfo
import com.kobe.qrbarcode.core.model.EventInfo
import com.kobe.qrbarcode.core.model.WifiInfo
import com.kobe.qrbarcode.core.parser.ContentBuilder

sealed interface FieldType {
    data object Text : FieldType
    data object Multiline : FieldType
    data object Number : FieldType
    data object Phone : FieldType
    data object Email : FieldType
    data object Url : FieldType
    data object Password : FieldType
    data object Toggle : FieldType
    data object DateTime : FieldType
    data class Options(val values: List<String>) : FieldType
}

data class FieldSpec(
    val key: String,
    val label: String,
    val type: FieldType = FieldType.Text,
    val required: Boolean = false,
    val placeholder: String = ""
)

private val SOCIAL_PLATFORMS = listOf(
    "Instagram", "Facebook", "X (Twitter)", "LinkedIn", "TikTok",
    "YouTube", "WhatsApp", "Telegram", "Snapchat", "GitHub"
)

private val WIFI_SECURITY = listOf("WPA/WPA2", "WPA3", "WEP", "None")

/** The content types offered by the QR generator, with their form and payload rules. */
enum class QrContentType(val label: String, val fields: List<FieldSpec>) {

    TEXT(
        "Text",
        listOf(FieldSpec("text", "Text", FieldType.Multiline, required = true))
    ),

    URL(
        "Website",
        listOf(FieldSpec("url", "Address", FieldType.Url, required = true, placeholder = "example.com"))
    ),

    PHONE(
        "Phone",
        listOf(FieldSpec("number", "Phone number", FieldType.Phone, required = true))
    ),

    EMAIL(
        "Email",
        listOf(
            FieldSpec("address", "To", FieldType.Email, required = true),
            FieldSpec("subject", "Subject"),
            FieldSpec("body", "Message", FieldType.Multiline)
        )
    ),

    SMS(
        "SMS",
        listOf(
            FieldSpec("number", "Phone number", FieldType.Phone, required = true),
            FieldSpec("message", "Message", FieldType.Multiline)
        )
    ),

    WIFI(
        "Wi-Fi",
        listOf(
            FieldSpec("ssid", "Network name (SSID)", required = true),
            FieldSpec("password", "Password", FieldType.Password),
            FieldSpec("security", "Security", FieldType.Options(WIFI_SECURITY)),
            FieldSpec("hidden", "Hidden network", FieldType.Toggle)
        )
    ),

    CONTACT(
        "Contact",
        listOf(
            FieldSpec("name", "Full name", required = true),
            FieldSpec("phone", "Phone", FieldType.Phone),
            FieldSpec("email", "Email", FieldType.Email),
            FieldSpec("organization", "Organization"),
            FieldSpec("jobTitle", "Job title"),
            FieldSpec("address", "Address"),
            FieldSpec("website", "Website", FieldType.Url),
            FieldSpec("note", "Note", FieldType.Multiline)
        )
    ),

    LOCATION(
        "Location",
        listOf(
            FieldSpec("latitude", "Latitude", FieldType.Number, required = true, placeholder = "5.6037"),
            FieldSpec("longitude", "Longitude", FieldType.Number, required = true, placeholder = "-0.1870"),
            FieldSpec("label", "Place name")
        )
    ),

    EVENT(
        "Event",
        listOf(
            FieldSpec("title", "Event title", required = true),
            FieldSpec("start", "Starts", FieldType.DateTime, required = true),
            FieldSpec("end", "Ends", FieldType.DateTime),
            FieldSpec("location", "Location"),
            FieldSpec("description", "Details", FieldType.Multiline)
        )
    ),

    SOCIAL(
        "Social",
        listOf(
            FieldSpec("platform", "Platform", FieldType.Options(SOCIAL_PLATFORMS), required = true),
            FieldSpec("handle", "Username or number", required = true)
        )
    );

    fun isComplete(values: Map<String, String>): Boolean =
        fields.filter { it.required }.all { values[it.key]?.isNotBlank() == true }

    fun build(values: Map<String, String>): String {
        fun value(key: String) = values[key].orEmpty().trim()
        return when (this) {
            TEXT -> value("text")
            URL -> ContentBuilder.url(value("url"))
            PHONE -> ContentBuilder.phone(value("number"))
            EMAIL -> ContentBuilder.email(value("address"), value("subject"), value("body"))
            SMS -> ContentBuilder.sms(value("number"), value("message"))
            WIFI -> ContentBuilder.wifi(
                WifiInfo(
                    ssid = value("ssid"),
                    password = value("password"),
                    security = when (value("security")) {
                        "WEP" -> "WEP"
                        "WPA3" -> "WPA3"
                        "None" -> "nopass"
                        else -> "WPA"
                    },
                    hidden = value("hidden") == "true"
                )
            )

            CONTACT -> ContentBuilder.vCard(
                ContactInfo(
                    name = value("name"),
                    organization = value("organization"),
                    jobTitle = value("jobTitle"),
                    phones = listOf(value("phone")).filter { it.isNotBlank() },
                    emails = listOf(value("email")).filter { it.isNotBlank() },
                    address = value("address"),
                    website = value("website"),
                    note = value("note")
                )
            )

            LOCATION -> ContentBuilder.geo(
                value("latitude").toDoubleOrNull() ?: 0.0,
                value("longitude").toDoubleOrNull() ?: 0.0,
                value("label")
            )

            EVENT -> ContentBuilder.event(
                EventInfo(
                    title = value("title"),
                    description = value("description"),
                    location = value("location"),
                    startMillis = value("start").toLongOrNull() ?: 0L,
                    endMillis = value("end").toLongOrNull() ?: 0L
                )
            )

            SOCIAL -> socialUrl(value("platform"), value("handle"))
        }
    }

    private fun socialUrl(platform: String, handle: String): String {
        val clean = handle.trim().removePrefix("@")
        return when (platform) {
            "Instagram" -> "https://instagram.com/$clean"
            "Facebook" -> "https://facebook.com/$clean"
            "X (Twitter)" -> "https://x.com/$clean"
            "LinkedIn" -> "https://linkedin.com/in/$clean"
            "TikTok" -> "https://tiktok.com/@$clean"
            "YouTube" -> "https://youtube.com/@$clean"
            "WhatsApp" -> "https://wa.me/" + clean.filter { it.isDigit() }
            "Telegram" -> "https://t.me/$clean"
            "Snapchat" -> "https://snapchat.com/add/$clean"
            "GitHub" -> "https://github.com/$clean"
            else -> ContentBuilder.url(clean)
        }
    }
}
