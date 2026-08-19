package com.kobe.qrbarcode.ui.detail

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kobe.qrbarcode.core.parser.ContentParser
import com.kobe.qrbarcode.core.util.ImageStore
import com.kobe.qrbarcode.core.util.TimeFormat
import com.kobe.qrbarcode.ui.LocalSnackbarHostState
import com.kobe.qrbarcode.ui.components.CodeImageBox
import com.kobe.qrbarcode.ui.components.CodeImageState
import com.kobe.qrbarcode.ui.components.rememberCodeImage
import com.kobe.qrbarcode.ui.scan.ResultContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CodeDetailScreen(
    onBack: () -> Unit,
    viewModel: CodeDetailViewModel = hiltViewModel()
) {
    val record by viewModel.record.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val current = record
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Code details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.delete(onBack) }) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val parsed = ContentParser.parse(current.content, current.format)
        val imageState by rememberCodeImage(
            content = current.content,
            format = current.format,
            style = current.style
        )
        val bitmap: Bitmap? = (imageState as? CodeImageState.Ready)?.bitmap

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(10.dp)
                    ) {
                        CodeImageBox(
                            state = imageState,
                            aspectRatio = if (current.format.isMatrix) 1f else 1.5f
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = TimeFormat.full(current.createdAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val image = bitmap ?: return@Button
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    ImageStore.saveToGallery(
                                        context,
                                        image,
                                        ImageStore.defaultFileName("kobe")
                                    )
                                }
                            }
                            viewModel.showMessage(
                                if (result.isSuccess) "Saved to your gallery"
                                else "Could not save the image"
                            )
                        }
                    },
                    enabled = bitmap != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Save PNG")
                }
                FilledTonalButton(
                    onClick = {
                        val image = bitmap ?: return@FilledTonalButton
                        runCatching {
                            ImageStore.shareImage(
                                context,
                                image,
                                ImageStore.defaultFileName("kobe"),
                                current.content
                            )
                        }
                    },
                    enabled = bitmap != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Share image")
                }
                OutlinedButton(
                    onClick = {
                        val image = bitmap ?: return@OutlinedButton
                        runCatching { ImageStore.print(context, image, "Kobe code") }
                            .onFailure { viewModel.showMessage("Printing is not available") }
                    },
                    enabled = bitmap != null
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = "Print", Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            ResultContent(
                parsed = parsed,
                format = current.format,
                isFavorite = current.isFavorite,
                onToggleFavorite = viewModel::toggleFavorite,
                onMessage = viewModel::showMessage
            )
        }
    }
}
