package com.kobe.qrbarcode.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.qrbarcode.core.model.CodeFormat
import com.kobe.qrbarcode.core.model.CodeSource
import com.kobe.qrbarcode.core.model.ParsedContent
import com.kobe.qrbarcode.core.parser.ContentParser
import com.kobe.qrbarcode.data.prefs.AppSettings
import com.kobe.qrbarcode.data.prefs.SettingsRepository
import com.kobe.qrbarcode.data.repo.CodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanResult(
    val parsed: ParsedContent,
    val format: CodeFormat,
    val recordId: Long? = null,
    val isFavorite: Boolean = false,
    /** Set once for a fresh scan so the UI knows to buzz and maybe auto-open. */
    val isNew: Boolean = true
)

data class ScanUiState(
    val torchEnabled: Boolean = false,
    val torchAvailable: Boolean = false,
    val result: ScanResult? = null,
    val decodingImage: Boolean = false,
    val message: String? = null
) {
    val isPaused: Boolean get() = result != null || decodingImage
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CodeRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings()
    )

    fun onDetected(codes: List<DetectedCode>) {
        val code = codes.firstOrNull() ?: return
        if (_state.value.isPaused) return
        accept(code)
    }

    private fun accept(code: DetectedCode) {
        val parsed = ContentParser.parse(code.content, code.format)
        _state.update { it.copy(result = ScanResult(parsed, code.format)) }
        if (settings.value.saveScanHistory) {
            viewModelScope.launch {
                val id = repository.record(parsed, code.format, CodeSource.SCANNED)
                _state.update { current ->
                    val result = current.result ?: return@update current
                    if (result.parsed.raw != parsed.raw) current
                    else current.copy(result = result.copy(recordId = id))
                }
            }
        }
    }

    fun scanFromImage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(decodingImage = true, message = null) }
            val codes = scanImage(context, uri)
            _state.update { it.copy(decodingImage = false) }
            if (codes.isEmpty()) {
                _state.update { it.copy(message = "No code found in that image") }
            } else {
                accept(codes.first())
            }
        }
    }

    fun toggleTorch() {
        _state.update { it.copy(torchEnabled = !it.torchEnabled) }
    }

    fun setTorchAvailable(available: Boolean) {
        _state.update { it.copy(torchAvailable = available) }
    }

    fun toggleFavorite() {
        val result = _state.value.result ?: return
        viewModelScope.launch {
            val id = result.recordId ?: repository.record(
                result.parsed,
                result.format,
                CodeSource.SCANNED
            )
            val next = !result.isFavorite
            repository.setFavorite(id, next)
            _state.update {
                it.copy(result = it.result?.copy(recordId = id, isFavorite = next))
            }
        }
    }

    /** Called once the UI has consumed the "new scan" side effects. */
    fun markResultSeen() {
        _state.update { it.copy(result = it.result?.copy(isNew = false)) }
    }

    fun dismissResult() {
        _state.update { it.copy(result = null) }
    }

    fun showMessage(message: String?) {
        _state.update { it.copy(message = message) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
