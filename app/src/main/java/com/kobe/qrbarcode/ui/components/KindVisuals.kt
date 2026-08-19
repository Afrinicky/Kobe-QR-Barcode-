package com.kobe.qrbarcode.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.kobe.qrbarcode.core.model.ContentKind
import com.kobe.qrbarcode.ui.theme.KindPalette

data class KindVisual(val icon: ImageVector, val color: Color)

fun ContentKind.visual(): KindVisual = when (this) {
    ContentKind.URL -> KindVisual(Icons.Rounded.Link, KindPalette.url)
    ContentKind.PHONE -> KindVisual(Icons.Rounded.Call, KindPalette.phone)
    ContentKind.EMAIL -> KindVisual(Icons.Rounded.Email, KindPalette.email)
    ContentKind.SMS -> KindVisual(Icons.Rounded.Sms, KindPalette.sms)
    ContentKind.WIFI -> KindVisual(Icons.Rounded.Wifi, KindPalette.wifi)
    ContentKind.CONTACT -> KindVisual(Icons.Rounded.Person, KindPalette.contact)
    ContentKind.LOCATION -> KindVisual(Icons.Rounded.Place, KindPalette.location)
    ContentKind.EVENT -> KindVisual(Icons.Rounded.CalendarMonth, KindPalette.event)
    ContentKind.PRODUCT -> KindVisual(Icons.Rounded.Inventory2, KindPalette.product)
    ContentKind.TEXT -> KindVisual(Icons.Rounded.Notes, KindPalette.text)
}
