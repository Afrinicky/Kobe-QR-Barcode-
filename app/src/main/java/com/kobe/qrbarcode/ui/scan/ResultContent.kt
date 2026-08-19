package com.kobe.qrbarcode.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kobe.qrbarcode.core.model.CodeFormat
import com.kobe.qrbarcode.core.model.ContentKind
import com.kobe.qrbarcode.core.model.ParsedContent
import com.kobe.qrbarcode.core.model.SmartAction
import com.kobe.qrbarcode.core.util.ActionRunner
import com.kobe.qrbarcode.ui.components.DetailRow
import com.kobe.qrbarcode.ui.components.IconBadge
import com.kobe.qrbarcode.ui.components.visual
import com.kobe.qrbarcode.ui.theme.PayloadTextStyle

fun SmartAction.icon(): ImageVector = when (this) {
    is SmartAction.OpenUrl -> Icons.Rounded.OpenInNew
    is SmartAction.Call -> Icons.Rounded.Call
    is SmartAction.SendSms -> Icons.Rounded.Sms
    is SmartAction.SendEmail -> Icons.Rounded.Email
    is SmartAction.OpenMap -> Icons.Rounded.Place
    is SmartAction.AddContact -> Icons.Rounded.PersonAdd
    is SmartAction.AddEvent -> Icons.Rounded.CalendarMonth
    is SmartAction.ConnectWifi -> Icons.Rounded.Wifi
}

/**
 * The "Smart Result" panel: what the code is, what it says and what you can do
 * with it, in that order.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultContent(
    parsed: ParsedContent,
    format: CodeFormat,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onMessage: (String) -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val visual = parsed.kind.visual()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = visual.icon, tint = visual.color, size = 46)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = parsed.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = format.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onToggleFavorite != null) {
                TextButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Favourite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(if (isFavorite) "Saved" else "Save")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = parsed.primary.ifBlank { parsed.raw },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (parsed.fields.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    parsed.fields.forEach { (label, value) ->
                        DetailRow(label = label, value = value)
                    }
                }
                if (parsed.kind == ContentKind.TEXT || parsed.raw != parsed.primary) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "RAW CONTENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    SelectionContainer {
                        Text(
                            text = parsed.raw,
                            style = PayloadTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }

        if (parsed.actions.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                parsed.actions.forEachIndexed { index, action ->
                    val runAction = {
                        ActionRunner.run(context, action)?.let(onMessage)
                        Unit
                    }
                    if (index == 0) {
                        Button(onClick = runAction) {
                            Icon(action.icon(), contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(action.label)
                        }
                    } else {
                        FilledTonalButton(onClick = runAction) {
                            Icon(action.icon(), contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(action.label)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(
                onClick = {
                    ActionRunner.copy(context, parsed.title, parsed.raw)
                    onMessage("Copied to clipboard")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Copy")
            }
            FilledTonalButton(
                onClick = { ActionRunner.shareText(context, parsed.raw) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Share")
            }
        }

        trailingContent?.let {
            Spacer(Modifier.height(12.dp))
            it()
        }
    }
}
