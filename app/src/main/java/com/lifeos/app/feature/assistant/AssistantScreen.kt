package com.lifeos.app.feature.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.designsystem.component.AiOrb
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifeConfirmDialog
import com.lifeos.app.core.designsystem.component.LifeSheet
import com.lifeos.app.core.designsystem.component.ListRow
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Radii
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.entity.AiConversationEntity
import com.lifeos.app.data.local.entity.AiMessageEntity
import com.lifeos.app.data.repository.AssistantRepository
import com.lifeos.app.data.repository.ModelManager
import com.lifeos.app.data.repository.PendingAction
import com.lifeos.app.domain.model.MessageRole
import com.lifeos.app.feature.common.LifeViewModel
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.navigation.screenContentPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repo: AssistantRepository,
    models: ModelManager,
    private val time: TimeProvider,
) : LifeViewModel() {
    val conversationId = MutableStateFlow<Long?>(null)
    val conversations = repo.observeConversations().stateWhileSubscribed(emptyList())
    val messages: StateFlow<List<AiMessageEntity>> = conversationId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeMessages(id) }
        .stateWhileSubscribed(emptyList())
    val pending = MutableStateFlow<PendingAction?>(null)
    val thinking = MutableStateFlow(false)
    val modelInstalled = models.installed()?.fileName
    val runtimeAvailable = models.runtimeAvailable

    fun now() = time.now()
    fun zone() = time.zone()

    fun send(text: String) {
        val input = text.trim()
        if (input.isEmpty() || thinking.value) return
        viewModelScope.launch {
            thinking.value = true
            try {
                val id = conversationId.value ?: repo.newConversation().also { conversationId.value = it }
                repo.record(id, MessageRole.USER, input)
                val reply = repo.handle(input)
                repo.record(id, MessageRole.ASSISTANT, reply.text, reply.status)
                pending.value = reply.pending
            } catch (e: Exception) {
                say(e.message ?: "Something went wrong.")
            } finally {
                thinking.value = false
            }
        }
    }

    fun confirm(yes: Boolean) {
        val action = pending.value ?: return
        pending.value = null
        val id = conversationId.value ?: return
        viewModelScope.launch {
            if (yes) {
                repo.record(id, MessageRole.USER, "Yes, ${action.description.replaceFirstChar { it.lowercase() }}")
                val reply = repo.confirm(action)
                repo.record(id, MessageRole.ASSISTANT, reply.text, reply.status)
            } else {
                repo.record(id, MessageRole.USER, "Cancel")
                repo.record(id, MessageRole.ASSISTANT, "Okay, nothing was deleted.", null)
            }
        }
    }

    fun open(c: AiConversationEntity) {
        pending.value = null
        conversationId.value = c.id
    }

    fun newChat() {
        pending.value = null
        conversationId.value = null
    }

    fun delete(c: AiConversationEntity) = act("Conversation deleted.") {
        if (conversationId.value == c.id) conversationId.value = null
        repo.deleteConversation(c)
    }
}

private val suggestions = listOf(
    "How much did I spend this month?", "Show my pending tasks", "Summarize my week", "Show my expenses by category",
    "Remind me to pay electricity bill tomorrow", "What appointments do I have tomorrow?", "Create a savings goal of ₹50,000", "help",
)

@Composable
fun AssistantScreen(onOpenSettings: () -> Unit, vm: AssistantViewModel = hiltViewModel()) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val thinking by vm.thinking.collectAsStateWithLifecycle()
    val currentId by vm.conversationId.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<AiConversationEntity?>(null) }
    val colors = LifeTheme.colors
    val listState = rememberLazyListState()
    val wide = LifeTheme.layout.useNavRail && LifeTheme.layout.dashboardColumns >= 3
    LaunchedEffect(messages.size, thinking) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size) }

    MessageBox(vm) {
        Row(Modifier.fillMaxSize().padding(screenContentPadding())) {
            if (wide) {
                GlassCard(Modifier.width(280.dp).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("History", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.newChat() }) { Icon(Icons.Outlined.Add, "New conversation", tint = colors.accentCyan) }
                    }
                    HistoryList(conversations, currentId, vm::open) { deleting = it }
                }
                Spacer(Modifier.width(Spacing.md))
            }
            Column(Modifier.weight(1f).fillMaxHeight().imePadding()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AiOrb(size = 56.dp, active = thinking)
                    Spacer(Modifier.width(Spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text("LIFE AI", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        Text(
                            if (vm.runtimeAvailable && vm.modelInstalled != null) "Local model: ${vm.modelInstalled}" else "Basic mode · offline command engine (not a generative AI)",
                            style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
                        )
                    }
                    if (!wide) IconButton(onClick = { showHistory = true }) { Icon(Icons.Outlined.History, "Conversation history", tint = colors.textPrimary) }
                    IconButton(onClick = { vm.newChat() }) { Icon(Icons.Outlined.Add, "New conversation", tint = colors.textPrimary) }
                }
                Spacer(Modifier.height(Spacing.sm))
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (messages.isEmpty()) item {
                        GlassCard {
                            Text("Ask about your money, tasks and schedule, or tell me to add something. Everything runs on this device.",
                                style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            TextButton(onClick = onOpenSettings) { Text("Local model settings", color = colors.accentCyan) }
                        }
                    }
                    items(messages, key = { it.id }) { m -> Bubble(m, vm) }
                    if (thinking) item { Text("Working…", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary) }
                    pending?.let { p ->
                        item {
                            GlassCard(strong = true) {
                                Text("Confirm: ${p.description}?", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Row {
                                    TextButton(onClick = { vm.confirm(false) }) { Text("Cancel", color = colors.textSecondary) }
                                    TextButton(onClick = { vm.confirm(true) }) { Text("Delete", color = colors.danger) }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    suggestions.forEach { s ->
                        AssistChip(onClick = { vm.send(s) }, label = { Text(s, maxLines = 1) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = colors.textSecondary))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it.take(500) },
                        placeholder = { Text("Ask LIFE AI…", color = colors.textTertiary) },
                        modifier = Modifier.weight(1f),
                        shape = Radii.pill, maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { vm.send(input); input = "" }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentCyan, unfocusedBorderColor = colors.glassBorder,
                            focusedContainerColor = colors.glassFill, unfocusedContainerColor = colors.glassFill,
                            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                        ),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    IconButton(onClick = { vm.send(input); input = "" }, enabled = input.isNotBlank() && !thinking,
                        modifier = Modifier.size(52.dp).clip(Radii.pill).background(colors.accentBrush)) {
                        Icon(Icons.AutoMirrored.Outlined.Send, "Send", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }

    if (showHistory) {
        LifeSheet("Conversations", { showHistory = false }, onSave = null) {
            HistoryList(conversations, currentId, { vm.open(it); showHistory = false }) { deleting = it }
        }
    }
    deleting?.let { c ->
        LifeConfirmDialog("Delete conversation?", "“${c.title}” and its messages will be deleted.", "Delete",
            onConfirm = { vm.delete(c); deleting = null }, onDismiss = { deleting = null }, destructive = true)
    }
}

@Composable
private fun HistoryList(list: List<AiConversationEntity>, currentId: Long?, onOpen: (AiConversationEntity) -> Unit, onDelete: (AiConversationEntity) -> Unit) {
    val colors = LifeTheme.colors
    if (list.isEmpty()) Text("No conversations yet.", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
    list.forEach { c ->
        ListRow(
            c.title, null,
            trailing = { IconButton(onClick = { onDelete(c) }) { Icon(Icons.Outlined.Delete, "Delete ${c.title}", tint = colors.textTertiary) } },
            onClick = { onOpen(c) },
            modifier = if (c.id == currentId) Modifier.background(colors.glassFillStrong, Radii.md) else Modifier,
        )
    }
}

@Composable
private fun Bubble(m: AiMessageEntity, vm: AssistantViewModel) {
    val colors = LifeTheme.colors
    val mine = m.role == MessageRole.USER
    val today = vm.now().atZone(vm.zone()).toLocalDate()
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .clip(Radii.lg)
                .background(if (mine) colors.accentPurple.copy(alpha = 0.28f) else colors.glassFillStrong)
                .padding(Spacing.md),
        ) {
            Text(m.text, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(TimeFormat.relative(m.createdAt, today, vm.zone()), style = MaterialTheme.typography.labelSmall, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Clip)
                when (m.actionStatus) {
                    "EXECUTED" -> { Spacer(Modifier.width(6.dp)); LifeChip("Done", colors.success) }
                    "FAILED" -> { Spacer(Modifier.width(6.dp)); LifeChip("Not done", colors.danger) }
                    "NEEDS_CONFIRMATION" -> { Spacer(Modifier.width(6.dp)); LifeChip("Needs confirmation", colors.warning) }
                }
            }
        }
    }
}
