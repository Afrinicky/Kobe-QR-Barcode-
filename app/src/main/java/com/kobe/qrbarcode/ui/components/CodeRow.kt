package com.kobe.qrbarcode.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kobe.qrbarcode.core.model.CodeSource
import com.kobe.qrbarcode.core.util.ActionRunner
import com.kobe.qrbarcode.core.util.TimeFormat
import com.kobe.qrbarcode.data.repo.CodeRecord

@Composable
fun CodeRow(
    record: CodeRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val visual = record.kind.visual()
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(icon = visual.icon, tint = visual.color, size = 44)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = if (record.source == CodeSource.SCANNED) "Scanned" else "Created",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = record.content.replace("\n", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${record.format.displayName} · ${TimeFormat.relative(record.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (record.isFavorite) Icons.Rounded.Star
                        else Icons.Rounded.StarBorder,
                        contentDescription = if (record.isFavorite) "Remove from favourites"
                        else "Add to favourites",
                        tint = if (record.isFavorite) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (onDelete != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "More actions",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Copy content") },
                            onClick = {
                                ActionRunner.copy(context, record.title, record.content)
                                menuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                ActionRunner.shareText(context, record.content)
                                menuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDelete()
                                menuOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}
