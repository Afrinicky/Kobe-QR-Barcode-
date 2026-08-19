package com.kobe.qrbarcode.ui.batch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.qrbarcode.core.model.CodeFormat
import com.kobe.qrbarcode.core.model.CodeSource
import com.kobe.qrbarcode.core.model.ParsedContent
import com.kobe.qrbarcode.core.parser.ContentParser
import com.kobe.qrbarcode.core.util.ActionRunner
import com.kobe.qrbarcode.core.util.TextExport
import com.kobe.qrbarcode.core.util.TimeFormat
import com.kobe.qrbarcode.data.prefs.SettingsRepository
import com.kobe.qrbarcode.data.repo.CodeRepository
import com.kobe.qrbarcode.ui.scan.DetectedCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class BatchItem(
    val content: String,
    val format: CodeFormat,
    val parsed: ParsedContent,
    val scannedAt: Long,
    val count: Int = 1
)

data class BatchUiState(
    val items: List<BatchItem> = emptyList(),
    val skipDuplicates: Boolean = true,
    val torchEnabled: Boolean = false,
    val torchAvailable: Boolean = false,
    val paused: Boolean = false,
    val message: String? = null,
    val lastAdded: String? = null
)

@HiltViewModel
class BatchScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CodeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BatchUiState())
    val state: StateFlow<BatchUiState> = _state.asStateFlow()

    /** Guards against the same symbol being reported by several frames in a row. */
    private var lastAcceptedAt = 0L
    private var lastAcceptedContent: String? = null

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _state.update { it.copy(skipDuplicates = settings.batchSkipDuplicates) }
        }
    }

    /** @return true when the code was added, so the screen can buzz. */
    fun onDetected(codes: List<DetectedCode>): Boolean {
        val current = _state.value
        if (current.paused) return false
        val code = codes.firstOrNull() ?: return false
        val now = System.currentTimeMillis()
        if (code.content == lastAcceptedContent && now - lastAcceptedAt < 1_200) return false

        val existingIndex = current.items.indexOfFirst { it.content == code.content }
        if (existingIndex >= 0) {
            if (current.skipDuplicates) {
                lastAcceptedContent = code.content
                lastAcceptedAt = now
                _state.update { it.copy(message = "Already in the list") }
                return false
            }
            _state.update { state ->
                val items = state.items.toMutableList()
                val item = items[existingIndex]
                items[existingIndex] = item.copy(count = item.count + 1, scannedAt = now)
                state.copy(items = items, lastAdded = code.content)
            }
        } else {
            val item = BatchItem(
                content = code.content,
                format = code.format,
                parsed = ContentParser.parse(code.content, code.format),
                scannedAt = now
            )
            _state.update { it.copy(items = listOf(item) + it.items, lastAdded = code.content) }
        }
        lastAcceptedContent = code.content
        lastAcceptedAt = now
        return true
    }

    fun remove(content: String) {
        _state.update { state -> state.copy(items = state.items.filterNot { it.content == content }) }
    }

    fun clear() {
        lastAcceptedContent = null
        _state.update { it.copy(items = emptyList(), message = "List cleared") }
    }

    fun toggleTorch() = _state.update { it.copy(torchEnabled = !it.torchEnabled) }

    fun setTorchAvailable(available: Boolean) =
        _state.update { it.copy(torchAvailable = available) }

    fun togglePause() = _state.update { it.copy(paused = !it.paused) }

    fun toggleSkipDuplicates() {
        val next = !_state.value.skipDuplicates
        _state.update { it.copy(skipDuplicates = next) }
        viewModelScope.launch { settingsRepository.setBatchSkipDuplicates(next) }
    }

    fun copyAll() {
        val items = _state.value.items
        if (items.isEmpty()) return
        ActionRunner.copy(context, "Batch scan", items.joinToString("\n") { it.content })
        _state.update { it.copy(message = "${items.size} codes copied") }
    }

    fun exportCsv(share: Boolean) {
        val items = _state.value.items.asReversed()
        if (items.isEmpty()) return
        val rows = listOf(listOf("index", "content", "type", "format", "scanned_at", "count")) +
            items.mapIndexed { index, item ->
                listOf(
                    (index + 1).toString(),
                    item.content,
                    item.parsed.kind.label,
                    item.format.displayName,
                    TimeFormat.stamp(item.scannedAt),
                    item.count.toString()
                )
            }
        write(fileName("csv"), TextExport.toCsv(rows), "text/csv", share)
    }

    fun exportTxt(share: Boolean) {
        val items = _state.value.items.asReversed()
        if (items.isEmpty()) return
        write(fileName("txt"), items.joinToString("\n") { it.content }, "text/plain", share)
    }

    fun saveToHistory() {
        val items = _state.value.items
        if (items.isEmpty()) return
        viewModelScope.launch {
            repository.recordAll(items.map { Triple(it.parsed, it.format, CodeSource.SCANNED) })
            _state.update { it.copy(message = "${items.size} codes saved to history") }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun write(name: String, content: String, mime: String, share: Boolean) {
        runCatching {
            if (share) {
                TextExport.shareText(context, name, content, mime)
            } else {
                TextExport.saveToDownloads(context, name, content, mime)
                _state.update { it.copy(message = "Saved to Downloads/Kobe QR/$name") }
            }
        }.onFailure {
            _state.update { state -> state.copy(message = it.message ?: "Export failed") }
        }
    }

    private fun fileName(extension: String): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "kobe_batch_$stamp.$extension"
    }
}
