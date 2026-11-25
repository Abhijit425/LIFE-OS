package com.lifeos.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.domain.model.MotionPreference
import com.lifeos.app.domain.model.ThemeMode
import com.lifeos.app.domain.repository.AppPreferences
import com.lifeos.app.domain.repository.PreferencesRepository
import com.lifeos.app.domain.repository.ProfileRepository
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
    private val prefs: PreferencesRepository,
    private val profile: ProfileRepository,
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> =
        prefs.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())

    val displayName: StateFlow<String?> =
        profile.displayName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setMotion(pref: MotionPreference) {
        viewModelScope.launch { prefs.setMotion(pref) }
    }

    fun saveName(name: String) {
        viewModelScope.launch {
            _message.value = if (profile.setDisplayName(name)) "Name saved." else "Please enter a name of 1–40 characters."
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
