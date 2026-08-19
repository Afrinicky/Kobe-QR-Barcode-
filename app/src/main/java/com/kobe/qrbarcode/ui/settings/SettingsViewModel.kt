package com.kobe.qrbarcode.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.qrbarcode.core.generate.CodeStyle
import com.kobe.qrbarcode.core.generate.ErrorCorrection
import com.kobe.qrbarcode.data.prefs.AppSettings
import com.kobe.qrbarcode.data.prefs.SettingsRepository
import com.kobe.qrbarcode.data.prefs.ThemeMode
import com.kobe.qrbarcode.data.repo.CodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val codeRepository: CodeRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { settingsRepository.setDynamicColor(enabled) }
    fun setVibrate(enabled: Boolean) = launch { settingsRepository.setVibrate(enabled) }
    fun setSound(enabled: Boolean) = launch { settingsRepository.setSound(enabled) }
    fun setAutoOpenUrls(enabled: Boolean) = launch { settingsRepository.setAutoOpenUrls(enabled) }
    fun setSaveScanHistory(enabled: Boolean) =
        launch { settingsRepository.setSaveScanHistory(enabled) }

    fun setSkipDuplicates(enabled: Boolean) =
        launch { settingsRepository.setBatchSkipDuplicates(enabled) }

    fun setDefaultForeground(color: Int) = updateStyle { it.copy(foreground = color) }
    fun setDefaultBackground(color: Int) = updateStyle { it.copy(background = color) }
    fun setDefaultMargin(margin: Int) = updateStyle { it.copy(margin = margin) }
    fun setDefaultErrorCorrection(level: ErrorCorrection) =
        updateStyle { it.copy(errorCorrection = level) }

    fun clearHistory() = launch {
        codeRepository.clearAll()
        _message.value = "History cleared"
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun updateStyle(transform: (CodeStyle) -> CodeStyle) = launch {
        settingsRepository.setDefaultStyle(transform(settings.value.defaultStyle))
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
