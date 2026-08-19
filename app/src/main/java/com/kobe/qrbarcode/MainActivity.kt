package com.kobe.qrbarcode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kobe.qrbarcode.ui.KobeAppRoot
import com.kobe.qrbarcode.ui.ThemeViewModel
import com.kobe.qrbarcode.ui.theme.KobeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: ThemeViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            KobeTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                KobeAppRoot()
            }
        }
    }
}
