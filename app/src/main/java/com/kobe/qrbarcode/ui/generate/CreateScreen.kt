package com.kobe.qrbarcode.ui.generate

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kobe.qrbarcode.core.generate.BarcodeValidator
import com.kobe.qrbarcode.core.generate.ErrorCorrection
import com.kobe.qrbarcode.core.model.CodeFormat
import com.kobe.qrbarcode.core.util.ImageStore
import com.kobe.qrbarcode.ui.LocalSnackbarHostState
import com.kobe.qrbarcode.ui.components.CodeImageBox
import com.kobe.qrbarcode.ui.components.CodeImageState
import com.kobe.qrbarcode.ui.components.ColorRowPicker
import com.kobe.qrbarcode.ui.components.SectionHeader
import com.kobe.qrbarcode.ui.components.rememberCodeImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    initialTab: Int = 0,
    onOpenHistory: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 6.dp)
        ) {
            Text("Create a code", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Generated on this phone — nothing is uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("QR Code") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Barcode") }
            )
        }
        if (selectedTab == 0) {
            QrGeneratorTab(onOpenHistory = onOpenHistory)
        } else {
            BarcodeGeneratorTab(onOpenHistory = onOpenHistory)
        }
    }
}

// ------------------------------------------------------------------ QR tab

@Composable
private fun QrGeneratorTab(
    onOpenHistory: () -> Unit,
    viewModel: QrGenerateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    var showStyle by remember { mutableStateOf(false) }

    val imageState by rememberCodeImage(
        content = state.payload,
        format = CodeFormat.QR_CODE,
        style = state.style,
        logo = state.logo
    )

    val logoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.setLogo(uri) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QrContentType.entries.toList()) { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.selectType(type) },
                        label = { Text(type.label) }
                    )
                }
            }
        }

        item {
            PreviewCard(
                imageState = imageState,
                placeholder = "Fill in the fields to see your QR code"
            )
        }

        item {
            OutputActions(
                imageState = imageState,
                fileNamePrefix = "qr",
                caption = state.payload,
                onSave = { viewModel.saveToHistory() },
                onFavorite = { viewModel.saveToHistory(favorite = true) },
                onMessage = viewModel::showMessage
            )
        }

        item {
            SectionHeader(title = "Content") {
                TextButton(onClick = viewModel::clear) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Clear")
                }
            }
        }

        items(state.type.fields, key = { it.key }) { field ->
            DynamicField(
                spec = field,
                value = state.values[field.key].orEmpty(),
                onChange = { viewModel.setValue(field.key, it) }
            )
        }

        item {
            SectionHeader(title = "Style") {
                TextButton(onClick = { showStyle = !showStyle }) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (showStyle) "Hide" else "Customise")
                }
            }
        }

        if (showStyle) {
            item {
                Column(
                    modifier = Modifier.animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ColorRowPicker(
                        label = "Foreground",
                        selected = state.style.foreground,
                        onSelected = viewModel::setForeground
                    )
                    ColorRowPicker(
                        label = "Background",
                        selected = state.style.background,
                        onSelected = viewModel::setBackground
                    )
                    LabelledSlider(
                        label = "Quiet zone",
                        value = state.style.margin.toFloat(),
                        range = 0f..8f,
                        steps = 7,
                        display = "${state.style.margin} modules",
                        onChange = { viewModel.setMargin(it.toInt()) }
                    )
                    LabelledSlider(
                        label = "Export size",
                        value = state.style.sizePx.toFloat(),
                        range = 512f..2048f,
                        steps = 5,
                        display = "${state.style.sizePx} px",
                        onChange = { viewModel.setSize((it / 128).toInt() * 128) }
                    )
                    Column {
                        Text("Error correction", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ErrorCorrection.entries.forEach { level ->
                                FilterChip(
                                    selected = state.style.errorCorrection == level,
                                    onClick = { viewModel.setErrorCorrection(level) },
                                    label = { Text("${level.level} · ${level.recovery}") }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                logoPicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Rounded.AddPhotoAlternate,
                                contentDescription = null,
                                Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(if (state.logo == null) "Add logo" else "Change logo")
                        }
                        if (state.logo != null) {
                            OutlinedButton(onClick = { viewModel.setLogo(null) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Remove logo",
                                    Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    if (state.logo != null) {
                        Text(
                            text = "Error correction is raised to High automatically so the logo " +
                                "never breaks the code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = viewModel::saveDefaultStyle) {
                        Text("Save as my default style")
                    }
                }
            }
        }

        item {
            TextButton(onClick = onOpenHistory) { Text("View saved codes") }
        }
    }
}

// ------------------------------------------------------------- Barcode tab

@Composable
private fun BarcodeGeneratorTab(
    onOpenHistory: () -> Unit,
    viewModel: BarcodeGenerateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current

    val imageState by rememberCodeImage(
        content = if (state.isValid) state.encoded else "",
        format = state.format,
        style = state.style
    )

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CodeFormat.generatable1D) { format ->
                    FilterChip(
                        selected = state.format == format,
                        onClick = { viewModel.selectFormat(format) },
                        label = { Text(format.displayName) }
                    )
                }
            }
        }

        item {
            PreviewCard(
                imageState = imageState,
                aspectRatio = 1.5f,
                placeholder = "Enter the data to encode"
            )
        }

        item {
            OutputActions(
                imageState = imageState,
                fileNamePrefix = "barcode",
                caption = state.encoded,
                onSave = { viewModel.saveToHistory() },
                onFavorite = { viewModel.saveToHistory(favorite = true) },
                onMessage = viewModel::showMessage
            )
        }

        item {
            androidx.compose.material3.OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::setInput,
                label = { Text("${state.format.displayName} content") },
                supportingText = {
                    Text(state.error ?: BarcodeValidator.hint(state.format))
                },
                isError = state.error != null,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = if (BarcodeValidator.isNumericOnly(state.format)) {
                        androidx.compose.ui.text.input.KeyboardType.Number
                    } else {
                        androidx.compose.ui.text.input.KeyboardType.Text
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.isValid && state.encoded != state.input) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Check digit added — encoding ${state.encoded}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item { SectionHeader(title = "Style") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ColorRowPicker(
                    label = "Bars",
                    selected = state.style.foreground,
                    onSelected = viewModel::setForeground
                )
                ColorRowPicker(
                    label = "Background",
                    selected = state.style.background,
                    onSelected = viewModel::setBackground
                )
                LabelledSlider(
                    label = "Export size",
                    value = state.style.sizePx.toFloat(),
                    range = 512f..2048f,
                    steps = 5,
                    display = "${state.style.sizePx} px",
                    onChange = { viewModel.setSize((it / 128).toInt() * 128) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show the digits under the bars", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = state.style.showHumanReadableText,
                        onCheckedChange = viewModel::setShowText
                    )
                }
            }
        }

        item {
            TextButton(onClick = onOpenHistory) { Text("View saved codes") }
        }
    }
}

// -------------------------------------------------------------- shared bits

@Composable
private fun PreviewCard(
    imageState: CodeImageState,
    placeholder: String,
    aspectRatio: Float = 1f
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(10.dp)
            ) {
                CodeImageBox(
                    state = imageState,
                    placeholder = placeholder,
                    aspectRatio = aspectRatio
                )
            }
        }
    }
}

@Composable
private fun OutputActions(
    imageState: CodeImageState,
    fileNamePrefix: String,
    caption: String,
    onSave: () -> Unit,
    onFavorite: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap: Bitmap? = (imageState as? CodeImageState.Ready)?.bitmap
    val enabled = bitmap != null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    ImageStore.defaultFileName(fileNamePrefix)
                                )
                            }
                        }
                        onMessage(
                            if (result.isSuccess) "Saved to your gallery"
                            else result.exceptionOrNull()?.message ?: "Could not save the image"
                        )
                    }
                },
                enabled = enabled,
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
                            ImageStore.defaultFileName(fileNamePrefix),
                            caption
                        )
                    }.onFailure { onMessage("Could not share the image") }
                },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Share")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onSave,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) { Text("Save to history") }
            OutlinedButton(
                onClick = onFavorite,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Star, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Favourite")
            }
            OutlinedButton(
                onClick = {
                    val image = bitmap ?: return@OutlinedButton
                    runCatching { ImageStore.print(context, image, "Kobe code") }
                        .onFailure { onMessage("Printing is not available") }
                },
                enabled = enabled
            ) {
                Icon(Icons.Rounded.Print, contentDescription = "Print", Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun LabelledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    display: String,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                display,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps
        )
    }
}
