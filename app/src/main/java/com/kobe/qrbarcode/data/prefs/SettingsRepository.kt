package com.kobe.qrbarcode.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kobe.qrbarcode.core.generate.CodeStyle
import com.kobe.qrbarcode.core.generate.ErrorCorrection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kobe_settings")

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val vibrate: Boolean = true,
    val sound: Boolean = false,
    val autoOpenUrls: Boolean = false,
    val saveScanHistory: Boolean = true,
    val batchSkipDuplicates: Boolean = true,
    val defaultStyle: CodeStyle = CodeStyle()
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val vibrate = booleanPreferencesKey("vibrate")
        val sound = booleanPreferencesKey("sound")
        val autoOpen = booleanPreferencesKey("auto_open_urls")
        val saveHistory = booleanPreferencesKey("save_scan_history")
        val skipDuplicates = booleanPreferencesKey("batch_skip_duplicates")
        val foreground = intPreferencesKey("qr_foreground")
        val background = intPreferencesKey("qr_background")
        val margin = intPreferencesKey("qr_margin")
        val errorCorrection = stringPreferencesKey("qr_error_correction")
        val size = intPreferencesKey("qr_size")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        val defaults = CodeStyle()
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(preferences[Keys.theme] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = preferences[Keys.dynamicColor] ?: false,
            vibrate = preferences[Keys.vibrate] ?: true,
            sound = preferences[Keys.sound] ?: false,
            autoOpenUrls = preferences[Keys.autoOpen] ?: false,
            saveScanHistory = preferences[Keys.saveHistory] ?: true,
            batchSkipDuplicates = preferences[Keys.skipDuplicates] ?: true,
            defaultStyle = CodeStyle(
                sizePx = preferences[Keys.size] ?: defaults.sizePx,
                foreground = preferences[Keys.foreground] ?: defaults.foreground,
                background = preferences[Keys.background] ?: defaults.background,
                margin = preferences[Keys.margin] ?: defaults.margin,
                errorCorrection = ErrorCorrection.fromLevel(preferences[Keys.errorCorrection])
            )
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.theme] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.dynamicColor] = enabled }
    suspend fun setVibrate(enabled: Boolean) = edit { it[Keys.vibrate] = enabled }
    suspend fun setSound(enabled: Boolean) = edit { it[Keys.sound] = enabled }
    suspend fun setAutoOpenUrls(enabled: Boolean) = edit { it[Keys.autoOpen] = enabled }
    suspend fun setSaveScanHistory(enabled: Boolean) = edit { it[Keys.saveHistory] = enabled }
    suspend fun setBatchSkipDuplicates(enabled: Boolean) = edit { it[Keys.skipDuplicates] = enabled }

    suspend fun setDefaultStyle(style: CodeStyle) = edit {
        it[Keys.foreground] = style.foreground
        it[Keys.background] = style.background
        it[Keys.margin] = style.margin
        it[Keys.errorCorrection] = style.errorCorrection.level
        it[Keys.size] = style.sizePx
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}
