package com.kobe.qrbarcode.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kobe.qrbarcode.ui.batch.BatchScanScreen
import com.kobe.qrbarcode.ui.detail.CodeDetailScreen
import com.kobe.qrbarcode.ui.generate.CreateScreen
import com.kobe.qrbarcode.ui.history.HistoryScreen
import com.kobe.qrbarcode.ui.home.HomeScreen
import com.kobe.qrbarcode.ui.nav.Routes
import com.kobe.qrbarcode.ui.nav.bottomDestinations
import com.kobe.qrbarcode.ui.scan.ScanScreen
import com.kobe.qrbarcode.ui.settings.SettingsScreen
import com.kobe.qrbarcode.ui.theme.KobeIndigo
import com.kobe.qrbarcode.ui.theme.KobeTeal

val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun KobeAppRoot() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore('?')
    val showChrome = bottomDestinations.any { it.baseRoute == currentRoute }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
                    SnackbarHost(snackbarHostState)
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showChrome,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    KobeBottomBar(
                        currentRoute = currentRoute,
                        onSelect = { navController.switchTo(it) },
                        onScan = { navController.navigate(Routes.SCAN) }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) }
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onScan = { navController.navigate(Routes.SCAN) },
                        onBatch = { navController.navigate(Routes.BATCH) },
                        onCreateQr = { navController.switchTo(Routes.create(0)) },
                        onCreateBarcode = { navController.switchTo(Routes.create(1)) },
                        onHistory = { navController.switchTo(Routes.history()) },
                        onFavorites = { navController.switchTo(Routes.history(true)) },
                        onOpenRecord = { navController.navigate(Routes.detail(it)) }
                    )
                }
                composable(Routes.SCAN) {
                    ScanScreen(
                        onClose = { navController.popBackStack() },
                        onOpenBatch = {
                            navController.popBackStack()
                            navController.navigate(Routes.BATCH)
                        }
                    )
                }
                composable(Routes.BATCH) {
                    BatchScanScreen(onClose = { navController.popBackStack() })
                }
                composable(
                    route = Routes.CREATE,
                    arguments = listOf(
                        navArgument("tab") { type = NavType.IntType; defaultValue = 0 }
                    )
                ) { entry ->
                    CreateScreen(
                        initialTab = entry.arguments?.getInt("tab") ?: 0,
                        onOpenHistory = { navController.switchTo(Routes.history()) }
                    )
                }
                composable(
                    route = Routes.HISTORY,
                    arguments = listOf(
                        navArgument("favorites") { type = NavType.BoolType; defaultValue = false }
                    )
                ) { entry ->
                    HistoryScreen(
                        favouritesFirst = entry.arguments?.getBoolean("favorites") ?: false,
                        onOpenRecord = { navController.navigate(Routes.detail(it)) },
                        onScan = { navController.navigate(Routes.SCAN) },
                        onCreate = { navController.switchTo(Routes.create(0)) }
                    )
                }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) {
                    CodeDetailScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

private fun NavController.switchTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun KobeBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    onScan: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomDestinations.take(2).forEach { destination ->
                BarItem(
                    modifier = Modifier.weight(1f),
                    label = destination.label,
                    selected = currentRoute == destination.baseRoute,
                    icon = destination.icon,
                    selectedIcon = destination.selectedIcon,
                    onClick = { onSelect(destination.route) }
                )
            }
            ScanButton(modifier = Modifier.weight(1f), onClick = onScan)
            bottomDestinations.drop(2).forEach { destination ->
                BarItem(
                    modifier = Modifier.weight(1f),
                    label = destination.label,
                    selected = currentRoute == destination.baseRoute,
                    icon = destination.icon,
                    selectedIcon = destination.selectedIcon,
                    onClick = { onSelect(destination.route) }
                )
            }
        }
    }
}

@Composable
private fun BarItem(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(23.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun ScanButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(10.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(KobeIndigo, KobeTeal))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.QrCodeScanner,
                contentDescription = "Scan",
                tint = Color.White,
                modifier = Modifier.size(27.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Scan",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

