package com.lifeos.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lifeos.app.domain.model.MotionPreference
import com.lifeos.app.domain.model.ThemeMode
import com.lifeos.app.domain.repository.AppPreferences
import com.lifeos.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val MOTION = stringPreferencesKey("motion_pref")
    }

    override val preferences: Flow<AppPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            AppPreferences(
                themeMode = p[Keys.THEME]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.DARK,
                motion = p[Keys.MOTION]?.let { v -> MotionPreference.entries.firstOrNull { it.name == v } }
                    ?: MotionPreference.FOLLOW_SYSTEM,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setMotion(pref: MotionPreference) {
        dataStore.edit { it[Keys.MOTION] = pref.name }
    }
}
