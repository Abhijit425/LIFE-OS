package com.lifeos.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.domain.model.DashboardSnapshot
import com.lifeos.app.domain.model.DashboardWidget
import com.lifeos.app.domain.repository.DashboardRepository
import com.lifeos.app.domain.usecase.DashboardMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(
        val snapshot: DashboardSnapshot,
        val now: Instant,
        val today: LocalDate,
        val zone: ZoneId,
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val time: TimeProvider,
) : ViewModel() {

    /** Bumped on resume / pull-to-refresh so "today" and "now" are recomputed (handles midnight & TZ changes). */
    private val refreshTick = MutableStateFlow(0)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val state: StateFlow<DashboardUiState> = refreshTick
        .flatMapLatest {
            val now = time.now()
            val zone = time.zone()
            val today = now.atZone(zone).toLocalDate()
            repository.observeSnapshot(today, now, zone)
                .map<DashboardSnapshot, DashboardUiState> { DashboardUiState.Ready(it, now, today, zone) }
                // Errors are caught per-subscription so a later refresh can recover.
                .catch { e -> emit(DashboardUiState.Error(e.message ?: "Couldn't load your dashboard.")) }
        }
        .onEach { _refreshing.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    init {
        viewModelScope.launch {
            runCatching { repository.ensureWidgets() }
                .onFailure { _message.value = "Couldn't prepare dashboard widgets." }
        }
    }

    fun refresh(userInitiated: Boolean = false) {
        if (userInitiated) _refreshing.value = true
        refreshTick.update { it + 1 }
    }

    fun setVisible(widgets: List<DashboardWidget>, index: Int, visible: Boolean) {
        if (index !in widgets.indices) return
        save(widgets.mapIndexed { i, w -> if (i == index) w.copy(isVisible = visible) else w })
    }

    fun move(widgets: List<DashboardWidget>, index: Int, delta: Int) {
        save(DashboardMath.move(widgets, index, delta))
    }

    fun resetWidgets() {
        viewModelScope.launch {
            runCatching { repository.resetWidgets() }
                .onSuccess { _message.value = "Dashboard layout reset." }
                .onFailure { _message.value = "Couldn't reset the layout." }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun save(widgets: List<DashboardWidget>) {
        viewModelScope.launch {
            runCatching { repository.saveWidgets(widgets) }
                .onFailure { _message.value = "Couldn't save the layout." }
        }
    }
}
