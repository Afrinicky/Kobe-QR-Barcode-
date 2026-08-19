package com.kobe.qrbarcode.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val BATCH = "batch"
    const val CREATE = "create?tab={tab}"
    const val HISTORY = "history?favorites={favorites}"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"

    fun create(tab: Int = 0) = "create?tab=$tab"
    fun history(favouritesOnly: Boolean = false) = "history?favorites=$favouritesOnly"
    fun detail(id: Long) = "detail/$id"
}

data class BottomDestination(
    val route: String,
    val baseRoute: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

val bottomDestinations = listOf(
    BottomDestination(
        route = Routes.HOME,
        baseRoute = "home",
        label = "Home",
        selectedIcon = Icons.Rounded.Home,
        icon = Icons.Outlined.Home
    ),
    BottomDestination(
        route = Routes.create(),
        baseRoute = "create",
        label = "Create",
        selectedIcon = Icons.Rounded.QrCode2,
        icon = Icons.Outlined.QrCode2
    ),
    BottomDestination(
        route = Routes.history(),
        baseRoute = "history",
        label = "History",
        selectedIcon = Icons.Rounded.History,
        icon = Icons.Outlined.History
    ),
    BottomDestination(
        route = Routes.SETTINGS,
        baseRoute = "settings",
        label = "Settings",
        selectedIcon = Icons.Rounded.Settings,
        icon = Icons.Outlined.Settings
    )
)
