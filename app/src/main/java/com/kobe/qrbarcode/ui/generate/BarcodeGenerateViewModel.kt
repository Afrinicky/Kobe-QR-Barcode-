package com.kobe.qrbarcode.ui.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.qrbarcode.core.generate.BarcodeValidator
import com.kobe.qrbarcode.core.generate.CodeStyle
import com.kobe.qrbarcode.core.model.CodeFormat
import com.kobe.qrbarcode.core.model.CodeSource
import com.kobe.qrbarcode.core.parser.ContentParser
import com.kobe.qrbarcode.data.repo.CodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BarcodeUiState(
    val format: CodeFormat = CodeFormat.CODE_128,
    val input: String = "",
    val style: CodeStyle = CodeStyle(sizePx = 1024, margin = 8),
    val message: String? = null
) {
    val validation: BarcodeValidator.Result
        get() = BarcodeValidator.validate(format, input)

    /** Content actually encoded — check digits are appended for you. */
    val encoded: String get() = if (input.isBlank()) "" else validation.normalized

    val error: String? get() = if (input.isBlank()) null else validation.message

    val isValid: Boolean get() = input.isNotBlank() && validation.isValid
}

@HiltViewModel
class BarcodeGenerateViewModel @Inject constructor(
    private val repository: CodeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BarcodeUiState())
    val state: StateFlow<BarcodeUiState> = _state.asStateFlow()

    fun selectFormat(format: CodeFormat) {
        _state.update { current ->
            val keepInput = !BarcodeValidator.isNumericOnly(format) ||
                current.input.all { it.isDigit() }
            current.copy(format = format, input = if (keepInput) current.input else "")
        }
    }

    fun setInput(value: String) {
        val format = _state.value.format
        val filtered = if (BarcodeValidator.isNumericOnly(format)) value.filter { it.isDigit() } else value
        _state.update { it.copy(input = filtered) }
    }

    fun setForeground(color: Int) = _state.update { it.copy(style = it.style.copy(foreground = color)) }

    fun setBackground(color: Int) = _state.update { it.copy(style = it.style.copy(background = color)) }

    fun setShowText(show: Boolean) =
        _state.update { it.copy(style = it.style.copy(showHumanReadableText = show)) }

    fun setSize(size: Int) = _state.update { it.copy(style = it.style.copy(sizePx = size)) }

    fun saveToHistory(favorite: Boolean = false) {
        val current = _state.value
        if (!current.isValid) return
        viewModelScope.launch {
            val parsed = ContentParser.parse(current.encoded, current.format)
            repository.record(
                parsed = parsed,
                format = current.format,
                source = CodeSource.GENERATED,
                style = current.style,
                favorite = favorite
            )
            _state.update {
                it.copy(message = if (favorite) "Saved to favourites" else "Saved to history")
            }
        }
    }

    fun showMessage(message: String) = _state.update { it.copy(message = message) }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
