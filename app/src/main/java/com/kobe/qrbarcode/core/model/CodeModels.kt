package com.kobe.qrbarcode.core.model

/** Every symbology the app can read or write. */
enum class CodeFormat(val displayName: String, val isMatrix: Boolean) {
    QR_CODE("QR Code", true),
    DATA_MATRIX("Data Matrix", true),
    AZTEC("Aztec", true),
    PDF417("PDF417", true),
    EAN_13("EAN-13", false),
    EAN_8("EAN-8", false),
    UPC_A("UPC-A", false),
    UPC_E("UPC-E", false),
    CODE_128("Code 128", false),
    CODE_39("Code 39", false),
    CODE_93("Code 93", false),
    ITF("ITF", false),
    CODABAR("Codabar", false),
    UNKNOWN("Unknown", false);

    companion object {
        fun fromName(name: String?): CodeFormat =
            entries.firstOrNull { it.name == name } ?: UNKNOWN

        /** Formats offered by the barcode generator, in the order shown in the UI. */
        val generatable1D = listOf(CODE_128, CODE_39, EAN_13, EAN_8, UPC_A, UPC_E, ITF, CODABAR)
    }
}

enum class CodeSource { SCANNED, GENERATED }

/** What the payload actually means — drives the smart result actions. */
enum class ContentKind(val label: String) {
    URL("Website"),
    PHONE("Phone Number"),
    EMAIL("Email"),
    SMS("SMS"),
    WIFI("Wi-Fi Network"),
    CONTACT("Contact"),
    LOCATION("Location"),
    EVENT("Event"),
    PRODUCT("Product"),
    TEXT("Text");

    companion object {
        fun fromName(name: String?): ContentKind =
            entries.firstOrNull { it.name == name } ?: TEXT
    }
}

data class WifiInfo(
    val ssid: String,
    val password: String = "",
    val security: String = "WPA",
    val hidden: Boolean = false
)

data class ContactInfo(
    val name: String = "",
    val organization: String = "",
    val jobTitle: String = "",
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val address: String = "",
    val website: String = "",
    val note: String = ""
)

data class EventInfo(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    /** Epoch millis, or 0 when the code carried no usable date. */
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val allDay: Boolean = false
)

data class GeoInfo(
    val latitude: Double,
    val longitude: Double,
    val label: String = ""
)

/** A single tappable action offered for a decoded payload. */
sealed interface SmartAction {
    val label: String

    data class OpenUrl(val url: String, override val label: String = "Open Website") : SmartAction
    data class Call(val number: String, override val label: String = "Call") : SmartAction
    data class SendSms(
        val number: String,
        val body: String = "",
        override val label: String = "Send SMS"
    ) : SmartAction

    data class SendEmail(
        val address: String,
        val subject: String = "",
        val body: String = "",
        override val label: String = "Send Email"
    ) : SmartAction

    data class OpenMap(val geo: GeoInfo, override val label: String = "Open Map") : SmartAction
    data class AddContact(val contact: ContactInfo, override val label: String = "Add Contact") :
        SmartAction

    data class AddEvent(val event: EventInfo, override val label: String = "Add to Calendar") :
        SmartAction

    data class ConnectWifi(val wifi: WifiInfo, override val label: String = "Connect") : SmartAction
}

/**
 * The result of interpreting a raw payload: a headline, a human readable value,
 * an optional detail table and the actions that make sense for it.
 */
data class ParsedContent(
    val kind: ContentKind,
    val title: String,
    val primary: String,
    val raw: String,
    val fields: List<Pair<String, String>> = emptyList(),
    val actions: List<SmartAction> = emptyList()
) {
    /** Short single-line label used in history rows and batch lists. */
    val shortLabel: String
        get() = primary.lineSequence().firstOrNull()?.take(120).orEmpty().ifBlank { raw.take(120) }
}
