package com.kobe.qrbarcode.core.util

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.kobe.qrbarcode.core.model.ContactInfo
import com.kobe.qrbarcode.core.model.EventInfo
import com.kobe.qrbarcode.core.model.GeoInfo
import com.kobe.qrbarcode.core.model.SmartAction
import com.kobe.qrbarcode.core.model.WifiInfo
import java.util.Locale

/**
 * Turns a [SmartAction] into a system intent. Every hand-off is explicit and
 * user initiated — nothing leaves the device on its own.
 */
object ActionRunner {

    /** @return null on success, otherwise a message explaining what went wrong. */
    fun run(context: Context, action: SmartAction): String? = when (action) {
        is SmartAction.OpenUrl -> view(context, Uri.parse(action.url), "No browser installed")
        is SmartAction.Call -> launch(
            context,
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.number.filterPhone()}")),
            "No dialer available"
        )

        is SmartAction.SendSms -> launch(
            context,
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${action.number.filterPhone()}")).apply {
                if (action.body.isNotBlank()) putExtra("sms_body", action.body)
            },
            "No messaging app available"
        )

        is SmartAction.SendEmail -> launch(
            context,
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${action.address}")).apply {
                if (action.subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, action.subject)
                if (action.body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, action.body)
            },
            "No email app available"
        )

        is SmartAction.OpenMap -> view(context, action.geo.toUri(), "No maps app available")
        is SmartAction.AddContact -> launch(
            context,
            contactIntent(action.contact),
            "No contacts app available"
        )

        is SmartAction.AddEvent -> launch(
            context,
            eventIntent(action.event),
            "No calendar app available"
        )

        is SmartAction.ConnectWifi -> connectWifi(context, action.wifi)
    }

    fun copy(context: Context, label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    }

    fun shareText(context: Context, value: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, value)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ---------------------------------------------------------------- private

    private fun contactIntent(contact: ContactInfo): Intent =
        Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            if (contact.name.isNotBlank()) putExtra(ContactsContract.Intents.Insert.NAME, contact.name)
            if (contact.organization.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.COMPANY, contact.organization)
            }
            if (contact.jobTitle.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.JOB_TITLE, contact.jobTitle)
            }
            contact.phones.getOrNull(0)?.let {
                putExtra(ContactsContract.Intents.Insert.PHONE, it)
            }
            contact.phones.getOrNull(1)?.let {
                putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, it)
            }
            contact.emails.getOrNull(0)?.let {
                putExtra(ContactsContract.Intents.Insert.EMAIL, it)
            }
            contact.emails.getOrNull(1)?.let {
                putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, it)
            }
            if (contact.address.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.POSTAL, contact.address)
            }
            if (contact.note.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.NOTES, contact.note)
            }
        }

    private fun eventIntent(event: EventInfo): Intent =
        Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, event.title)
            if (event.description.isNotBlank()) {
                putExtra(CalendarContract.Events.DESCRIPTION, event.description)
            }
            if (event.location.isNotBlank()) {
                putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
            }
            if (event.startMillis > 0) {
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startMillis)
            }
            if (event.endMillis > 0) {
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endMillis)
            }
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.allDay)
        }

    private fun GeoInfo.toUri(): Uri = when {
        latitude == 0.0 && longitude == 0.0 && label.isNotBlank() ->
            Uri.parse("geo:0,0?q=" + Uri.encode(label))

        label.isNotBlank() -> Uri.parse(
            "geo:%f,%f?q=%f,%f(%s)".format(
                Locale.US, latitude, longitude, latitude, longitude, Uri.encode(label)
            )
        )

        else -> Uri.parse("geo:%f,%f?q=%f,%f".format(Locale.US, latitude, longitude, latitude, longitude))
    }

    private fun connectWifi(context: Context, wifi: WifiInfo): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val result = addNetworkSuggestion(context, wifi)
            if (result == null) return null
        }
        copy(context, "Wi-Fi password", wifi.password)
        val opened = launch(
            context,
            Intent(Settings.ACTION_WIFI_SETTINGS),
            "Wi-Fi settings are unavailable"
        )
        return opened ?: if (wifi.password.isNotBlank()) {
            "Password copied — pick \"${wifi.ssid}\" and paste it"
        } else {
            "Pick \"${wifi.ssid}\" in the Wi-Fi list"
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private fun addNetworkSuggestion(context: Context, wifi: WifiInfo): String? {
        val builder = WifiNetworkSuggestion.Builder().setSsid(wifi.ssid)
        when {
            wifi.security.startsWith("WPA3") -> builder.setWpa3Passphrase(wifi.password)
            wifi.password.isNotBlank() -> builder.setWpa2Passphrase(wifi.password)
        }
        if (wifi.hidden) builder.setIsHiddenSsid(true)
        val suggestions = arrayListOf(runCatching { builder.build() }.getOrNull() ?: return "invalid")
        val bundle = Bundle().apply {
            putParcelableArrayList(Settings.EXTRA_WIFI_NETWORK_LIST, suggestions)
        }
        val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
            .putExtras(bundle)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launch(context, intent, "Could not open the Wi-Fi dialog")
    }

    private fun view(context: Context, uri: Uri, failure: String): String? =
        launch(context, Intent(Intent.ACTION_VIEW, uri), failure)

    private fun launch(context: Context, intent: Intent, failure: String): String? = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    } catch (_: ActivityNotFoundException) {
        failure
    } catch (_: SecurityException) {
        failure
    }

    private fun String.filterPhone(): String = filter { it.isDigit() || it == '+' || it == '#' || it == '*' }
}
