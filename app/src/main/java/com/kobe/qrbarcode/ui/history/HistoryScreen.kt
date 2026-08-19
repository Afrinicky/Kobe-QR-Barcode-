package com.kobe.qrbarcode.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kobe.qrbarcode.ui.components.CodeRow
import com.kobe.qrbarcode.ui.components.EmptyState

@Composable
fun HistoryScreen(
    favouritesFirst: Boolean,
    onOpenRecord: (Long) -> Unit,
    onScan: () -> Unit,
    onCreate: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(favouritesFirst) {
        if (favouritesFirst) viewModel.setFilter(HistoryFilter.FAVOURITES)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 8.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("History", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${records.size} ${if (records.size == 1) "entry" else "entries"} on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showClearDialog = true }) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear history")
            }
        }

        OutlinedTextField(
            value = query.query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Search content") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryFilter.entries.forEach { filter ->
                FilterChip(
                    selected = query.filter == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (records.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.History,
                title = if (query.query.isNotBlank()) "No matches" else "Nothing saved yet",
                message = if (query.query.isNotBlank()) {
                    "Try a different search term."
                } else {
                    "Scan a code or create one — everything stays on this phone."
                },
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onScan) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Scan")
                        }
                        TextButton(onClick = onCreate) { Text("Create a code") }
                    }
                }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    CodeRow(
                        record = record,
                        onClick = { onOpenRecord(record.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(record) },
                        onDelete = { viewModel.delete(record) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all history?") },
            text = { Text("Every scanned and created code on this device will be removed. Favourites are deleted too.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}
