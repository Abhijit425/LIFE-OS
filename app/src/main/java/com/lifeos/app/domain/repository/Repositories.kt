package com.lifeos.app.domain.repository

import com.lifeos.app.domain.model.DashboardSnapshot
import com.lifeos.app.domain.model.DashboardWidget
import com.lifeos.app.domain.model.MotionPreference
import com.lifeos.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val motion: MotionPreference = MotionPreference.FOLLOW_SYSTEM,
)

interface PreferencesRepository {
    val preferences: Flow<AppPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setMotion(pref: MotionPreference)
}

interface ProfileRepository {
    val displayName: Flow<String?>

    /** Returns false (and stores nothing) when the name fails validation. */
    suspend fun setDisplayName(name: String): Boolean
}

interface DashboardRepository {
    fun observeSnapshot(today: LocalDate, now: Instant, zone: ZoneId): Flow<DashboardSnapshot>
    suspend fun ensureWidgets()
    suspend fun saveWidgets(widgets: List<DashboardWidget>)
    suspend fun resetWidgets()
}
