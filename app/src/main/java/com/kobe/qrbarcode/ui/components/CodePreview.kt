package com.kobe.qrbarcode.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kobe.qrbarcode.core.generate.CodeGenerator
import com.kobe.qrbarcode.core.generate.CodeStyle
import com.kobe.qrbarcode.core.model.CodeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface CodeImageState {
    data object Idle : CodeImageState
    data object Loading : CodeImageState
    data class Ready(val bitmap: Bitmap) : CodeImageState
    data class Failed(val message: String) : CodeImageState
}

/** Renders the code off the main thread and re-renders whenever the inputs change. */
@Composable
fun rememberCodeImage(
    content: String,
    format: CodeFormat,
    style: CodeStyle,
    logo: Bitmap? = null
): State<CodeImageState> = produceState<CodeImageState>(
    initialValue = CodeImageState.Idle,
    content,
    format,
    style,
    logo
) {
    if (content.isBlank()) {
        value = CodeImageState.Idle
        return@produceState
    }
    value = CodeImageState.Loading
    value = withContext(Dispatchers.Default) {
        try {
            CodeImageState.Ready(CodeGenerator.generate(content, format, style, logo))
        } catch (error: CodeGenerator.EncodeException) {
            CodeImageState.Failed(error.message ?: "This content cannot be encoded")
        } catch (error: Exception) {
            CodeImageState.Failed(error.message ?: "This content cannot be encoded")
        }
    }
}

@Composable
fun CodeImageBox(
    state: CodeImageState,
    modifier: Modifier = Modifier,
    placeholder: String = "Your code will appear here",
    aspectRatio: Float = 1f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = state, label = "code-preview") { current ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (current) {
                    is CodeImageState.Ready -> Image(
                        bitmap = current.bitmap.asImageBitmap(),
                        contentDescription = "Generated code preview",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None
                    )

                    CodeImageState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 3.dp
                    )

                    is CodeImageState.Failed -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    CodeImageState.Idle -> Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}
