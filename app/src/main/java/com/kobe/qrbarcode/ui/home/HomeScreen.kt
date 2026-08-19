package com.kobe.qrbarcode.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kobe.qrbarcode.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kobe.qrbarcode.ui.components.CodeRow
import com.kobe.qrbarcode.ui.components.EmptyState
import com.kobe.qrbarcode.ui.components.IconBadge
import com.kobe.qrbarcode.ui.components.InfoPill
import com.kobe.qrbarcode.ui.components.SectionHeader
import com.kobe.qrbarcode.ui.theme.KobeIndigo
import com.kobe.qrbarcode.ui.theme.KobeIndigoDark
import com.kobe.qrbarcode.ui.theme.KobeTeal
import com.kobe.qrbarcode.ui.theme.KindPalette

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onBatch: () -> Unit,
    onCreateQr: () -> Unit,
    onCreateBarcode: () -> Unit,
    onHistory: () -> Unit,
    onFavorites: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HomeHeader() }
        item { ScanHeroCard(onScan = onScan) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.QrCode2,
                    tint = KindPalette.url,
                    title = "Create QR",
                    subtitle = "10 content types",
                    onClick = onCreateQr
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.ViewList,
                    tint = KindPalette.product,
                    title = "Create Barcode",
                    subtitle = "8 symbologies",
                    onClick = onCreateBarcode
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Dashboard,
                    tint = KindPalette.event,
                    title = "Batch Scan",
                    subtitle = "Lists & CSV export",
                    onClick = onBatch
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Star,
                    tint = KindPalette.location,
                    title = "Favourites",
                    subtitle = "${state.favouriteCount} saved",
                    onClick = onFavorites
                )
            }
        }
        item {
            StatsStrip(
                scanned = state.scannedCount,
                generated = state.generatedCount,
                favourites = state.favouriteCount
            )
        }
        item {
            SectionHeader(
                title = "Recent activity",
                modifier = Modifier.padding(top = 8.dp)
            ) {
                if (state.recent.isNotEmpty()) {
                    TextButton(onClick = onHistory) {
                        Text("See all")
                        Spacer(Modifier.size(4.dp))
                        Icon(
                            Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        if (state.recent.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = "Nothing here yet",
                    message = "Scanned and created codes are kept on this phone only."
                )
            }
        } else {
            items(state.recent, key = { it.id }) { record ->
                CodeRow(
                    record = record,
                    onClick = { onOpenRecord(record.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(record) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 14.dp, bottom = 2.dp)
    ) {
        Text(
            text = stringResource(R.string.app_full_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoPill(text = "Works offline", icon = Icons.Rounded.CloudOff)
            Text(
                text = "No account · No cloud",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScanHeroCard(onScan: () -> Unit) {
    Card(
        onClick = onScan,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(listOf(KobeIndigoDark, KobeIndigo, KobeTeal))
                )
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Scan a code",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "QR, EAN, UPC, Code 128/39, ITF and Codabar — recognised automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Spacer(Modifier.size(14.dp))
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            IconBadge(icon = icon, tint = tint, size = 40)
            Spacer(Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsStrip(scanned: Int, generated: Int, favourites: Int) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCell("Scanned", scanned)
            StatCell("Created", generated)
            StatCell("Favourites", favourites)
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
