package com.kobe.qrbarcode.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.qrbarcode.data.prefs.AppSettings
import com.kobe.qrbarcode.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Supplies the theme choice to the activity before any screen is composed. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )
}
