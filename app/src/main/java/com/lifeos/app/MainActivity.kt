package com.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.designsystem.theme.LifeOsTheme
import com.lifeos.app.domain.model.ThemeMode
import com.lifeos.app.domain.repository.AppPreferences
import com.lifeos.app.domain.repository.PreferencesRepository
import com.lifeos.app.navigation.LifeOsAppShell
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(prefs: PreferencesRepository) : ViewModel() {
    /** null until DataStore has been read once; the splash screen stays up until then. */
    val preferences: StateFlow<AppPreferences?> = prefs.preferences
        .map<AppPreferences, AppPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.preferences.value == null }
        enableEdgeToEdge()
        setContent {
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val current = prefs ?: AppPreferences()
            val dark = when (current.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Keep status/navigation bar icons readable against the app theme (not the system one).
            DisposableEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            LifeOsTheme(themeMode = current.themeMode, motionPreference = current.motion) {
                LifeOsAppShell()
            }
        }
    }
}
