package com.lifeos.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.designsystem.component.AiOrb
import com.lifeos.app.core.designsystem.component.LifeBackground
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.theme.LifeOsTheme
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val prefs: PreferencesRepository) : ViewModel() {
    /** null until DataStore has been read once; the splash screen stays up until then. */
    val preferences: StateFlow<AppPreferences?> = prefs.preferences
        .map<AppPreferences, AppPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markNotificationIntroShown() {
        viewModelScope.launch { prefs.setNotificationsIntroShown() }
    }
}

/** FragmentActivity (a ComponentActivity) so BiometricPrompt can be used for app lock and encrypted documents. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Locked on cold start and after 60 s in the background, when app lock is on. */
    private var locked by mutableStateOf(true)
    private var backgroundedAt = 0L
    private var authInProgress = false

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
            DisposableEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            LifeOsTheme(themeMode = current.themeMode, motionPreference = current.motion) {
                if (prefs != null && current.appLock && locked) {
                    LockScreen(onUnlock = { authenticate() })
                    LaunchedEffect(Unit) { authenticate() }
                } else {
                    LifeOsAppShell()
                    if (prefs != null && !current.notificationsIntroShown) NotificationPermissionOnce()
                }
            }
        }
    }

    @Composable
    private fun NotificationPermissionOnce() {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.markNotificationIntroShown() }
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else viewModel.markNotificationIntroShown()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !authInProgress) backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundedAt != 0L && SystemClock.elapsedRealtime() - backgroundedAt > 60_000) locked = true
        backgroundedAt = 0L
    }

    private fun authenticate() {
        if (authInProgress) return
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            // The device lock was removed; don't trap the user out of their own data.
            locked = false
            return
        }
        authInProgress = true
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authInProgress = false
                locked = false
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                authInProgress = false
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock LIFE OS")
                .setSubtitle("Use your fingerprint, face or screen lock")
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    LifeBackground {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AiOrb(size = 160.dp)
            Spacer(Modifier.height(Spacing.lg))
            Text("LIFE OS is locked", style = MaterialTheme.typography.headlineMedium, color = LifeTheme.colors.textPrimary)
            Spacer(Modifier.height(Spacing.md))
            LifePrimaryButton("Unlock", onClick = onUnlock)
        }
    }
}
