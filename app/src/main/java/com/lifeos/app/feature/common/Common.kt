package com.lifeos.app.feature.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Base ViewModel: one-shot user messages and error-safe launching. */
abstract class LifeViewModel : ViewModel() {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    protected fun say(text: String) {
        _message.value = text
    }

    /** Runs [block]; reports [success] only if it completed, otherwise the error message. */
    protected fun act(success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                if (success != null) say(success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say(e.message ?: "Something went wrong. Nothing was changed.")
            }
        }
    }

    protected fun <T> Flow<T>.stateWhileSubscribed(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
}

/** Screen container with a snackbar wired to the ViewModel's messages. */
@Composable
fun MessageBox(vm: LifeViewModel, content: @Composable BoxScope.() -> Unit) {
    val message by vm.message.collectAsStateWithLifecycle()
    val host = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            host.showSnackbar(it)
            vm.consumeMessage()
        }
    }
    Box(Modifier.fillMaxSize()) {
        content()
        SnackbarHost(host, Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
    }
}
