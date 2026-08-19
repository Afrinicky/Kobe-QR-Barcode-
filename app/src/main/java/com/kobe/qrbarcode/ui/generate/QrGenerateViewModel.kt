package com.kobe.qrbarcode.ui.generate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.qrbarcode.core.generate.CodeStyle
import com.kobe.qrbarcode.core.generate.ErrorCorrection
import com.kobe.qrbarcode.core.model.CodeFormat
import com.kobe.qrbarcode.core.model.CodeSource
import com.kobe.qrbarcode.core.parser.ContentParser
import com.kobe.qrbarcode.data.prefs.SettingsRepository
import com.kobe.qrbarcode.data.repo.CodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class QrUiState(
    val type: QrContentType = QrContentType.TEXT,
    val values: Map<String, String> = emptyMap(),
    val style: CodeStyle = CodeStyle(),
    val logo: Bitmap? = null,
    val message: String? = null,
    val savedId: Long? = null
) {
    val payload: String get() = if (type.isComplete(values)) type.build(values) else ""
    val isComplete: Boolean get() = type.isComplete(values)
}

@HiltViewModel
class QrGenerateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CodeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(QrUiState())
    val state: StateFlow<QrUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val defaults = settingsRepository.settings.first().defaultStyle
            _state.update { it.copy(style = defaults) }
        }
    }

    fun selectType(type: QrContentType) {
        _state.update {
            it.copy(
                type = type,
                values = defaultValues(type),
                savedId = null
            )
        }
    }

    fun setValue(key: String, value: String) {
        _state.update { it.copy(values = it.values + (key to value), savedId = null) }
    }

    fun setForeground(color: Int) = _state.update { it.copy(style = it.style.copy(foreground = color)) }

    fun setBackground(color: Int) = _state.update { it.copy(style = it.style.copy(background = color)) }

    fun setMargin(margin: Int) = _state.update { it.copy(style = it.style.copy(margin = margin)) }

    fun setSize(size: Int) = _state.update { it.copy(style = it.style.copy(sizePx = size)) }

    fun setErrorCorrection(level: ErrorCorrection) =
        _state.update { it.copy(style = it.style.copy(errorCorrection = level)) }

    fun setLogo(uri: Uri?) {
        if (uri == null) {
            _state.update { it.copy(logo = null) }
            return
        }
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { loadBitmap(uri) }
            if (bitmap == null) {
                _state.update { it.copy(message = "That image could not be opened") }
            } else {
                _state.update { it.copy(logo = bitmap) }
            }
        }
    }

    fun clear() {
        _state.update { it.copy(values = defaultValues(it.type), logo = null, savedId = null) }
    }

    fun saveDefaultStyle() {
        viewModelScope.launch {
            settingsRepository.setDefaultStyle(_state.value.style)
            _state.update { it.copy(message = "Saved as your default QR style") }
        }
    }

    fun saveToHistory(favorite: Boolean = false) {
        val current = _state.value
        val payload = current.payload
        if (payload.isBlank()) return
        viewModelScope.launch {
            val parsed = ContentParser.parse(payload, CodeFormat.QR_CODE)
            val id = repository.record(
                parsed = parsed,
                format = CodeFormat.QR_CODE,
                source = CodeSource.GENERATED,
                style = current.style,
                favorite = favorite
            )
            _state.update {
                it.copy(
                    savedId = id,
                    message = if (favorite) "Saved to favourites" else "Saved to history"
                )
            }
        }
    }

    fun showMessage(message: String) = _state.update { it.copy(message = message) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun defaultValues(type: QrContentType): Map<String, String> = buildMap {
        type.fields.forEach { field ->
            when (val fieldType = field.type) {
                is FieldType.Options -> put(field.key, fieldType.values.first())
                FieldType.Toggle -> put(field.key, "false")
                else -> put(field.key, "")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun loadBitmap(uri: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()
}
