package com.lifeos.app.feature.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.lifeos.app.core.designsystem.component.ChoiceChips
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.FieldLabel
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifeConfirmDialog
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.component.SearchField
import com.lifeos.app.core.designsystem.component.SwitchRow
import com.lifeos.app.core.designsystem.component.readableWidth
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.entity.NoteEntity
import com.lifeos.app.data.local.entity.ProjectEntity
import com.lifeos.app.data.local.entity.TaskEntity
import com.lifeos.app.data.repository.NoteRepository
import com.lifeos.app.data.repository.TaskRepository
import com.lifeos.app.feature.common.LifeViewModel
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.navigation.NoteRoute
import com.lifeos.app.navigation.screenContentPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(private val repo: NoteRepository) : LifeViewModel() {
    val showArchived = MutableStateFlow(false)
    val notes: StateFlow<List<NoteEntity>> = showArchived.flatMapLatest { repo.observe(it) }.stateWhileSubscribed(emptyList())
    val tags = repo.observeTags().stateWhileSubscribed(emptyList())
    fun setPinned(n: NoteEntity) = act { repo.setPinned(n, !n.isPinned) }
}

@Composable
fun NotesScreen(onOpen: (Long) -> Unit, vm: NotesViewModel = hiltViewModel()) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val archived by vm.showArchived.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var tagFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = LifeTheme.colors
    val tagsByNote = tags.groupBy({ it.noteId }, { it.tag })
    val allTags = tags.map { it.tag }.distinct().sorted()
    val filtered = notes.filter { n ->
        (query.isBlank() || n.title.contains(query, true) || n.content.contains(query, true) || n.category.orEmpty().contains(query, true)) &&
            (tagFilter == null || tagsByNote[n.id]?.contains(tagFilter) == true)
    }
    MessageBox(vm) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(LifeTheme.layout.dashboardColumns),
            contentPadding = screenContentPadding(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalItemSpacing = Spacing.md,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Column {
                    ScreenHeader(if (archived) "Archived notes" else "Notes", "${notes.size} ${if (archived) "archived" else "notes"} · stored offline") {
                        IconButton(onClick = { vm.showArchived.value = !archived }) {
                            Icon(if (archived) Icons.Outlined.StickyNote2 else Icons.Outlined.Archive, if (archived) "Show notes" else "Show archive", tint = colors.textPrimary)
                        }
                        LifePrimaryButton("Note", onClick = { onOpen(0) }, icon = Icons.Outlined.Add)
                    }
                    SearchField(query, { query = it }, "Search notes")
                    if (allTags.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.xs))
                        ChoiceChips(listOf<String?>(null) + allTags, tagFilter, { it?.let { t -> "#$t" } ?: "All tags" }, { tagFilter = it })
                    }
                }
            }
            if (filtered.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    GlassCard { EmptyState(Icons.Outlined.StickyNote2, if (notes.isEmpty()) "No notes yet" else "Nothing matches", "Quick notes, checklists and ideas — all offline.") }
                }
            }
            items(filtered, key = { it.id }) { n ->
                GlassCard(onClick = { onOpen(n.id) }, onClickLabel = "Open ${n.title}") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(n.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!archived) IconButton(onClick = { vm.setPinned(n) }) {
                            Icon(Icons.Outlined.PushPin, if (n.isPinned) "Unpin" else "Pin", tint = if (n.isPinned) colors.accentPurple else colors.textTertiary)
                        }
                    }
                    Text(
                        if (n.isChecklist) checklistSummary(n.content) else n.content.take(220),
                        style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 6, overflow = TextOverflow.Ellipsis,
                    )
                    val nt = tagsByNote[n.id].orEmpty()
                    if (nt.isNotEmpty() || n.category != null) {
                        Spacer(Modifier.height(Spacing.xs))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            n.category?.let { LifeChip(it, colors.accentCyan) }
                            nt.take(3).forEach { LifeChip("#$it", colors.accentPurple) }
                        }
                    }
                }
            }
        }
    }
}

private fun checklistSummary(content: String): String {
    val items = parseChecklist(content)
    return "${items.count { it.second }}/${items.size} done\n" + items.take(4).joinToString("\n") { (if (it.second) "☑ " else "☐ ") + it.first }
}

/** Checklist lines are stored as Markdown task items: "- [ ] text" / "- [x] text". */
fun parseChecklist(content: String): List<Pair<String, Boolean>> = content.lines().mapNotNull { line ->
    val m = Regex("""^\s*-\s*\[( |x|X)]\s?(.*)$""").find(line) ?: return@mapNotNull line.takeIf { it.isNotBlank() }?.let { it.trim() to false }
    m.groupValues[2] to (m.groupValues[1].lowercase() == "x")
}

fun serializeChecklist(items: List<Pair<String, Boolean>>): String =
    items.filter { it.first.isNotBlank() }.joinToString("\n") { "- [${if (it.second) "x" else " "}] ${it.first}" }

/** Renders the small Markdown subset used by notes: # headings, **bold**, *italic*, - bullets. */
fun renderMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { i, rawLine ->
        if (i > 0) append("\n")
        var line = rawLine
        val heading = Regex("^(#{1,3})\\s+(.*)$").find(line)
        if (heading != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (22 - heading.groupValues[1].length * 2).sp)) { append(heading.groupValues[2]) }
            return@forEachIndexed
        }
        if (Regex("^\\s*[-*]\\s+").containsMatchIn(line)) {
            append("•  ")
            line = line.replaceFirst(Regex("^\\s*[-*]\\s+"), "")
        }
        var idx = 0
        val token = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")
        token.findAll(line).forEach { m ->
            append(line.substring(idx, m.range.first))
            if (m.groupValues[1].isNotEmpty()) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
            else withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(m.groupValues[2]) }
            idx = m.range.last + 1
        }
        append(line.substring(idx))
    }
}

// ------------------------------------------------------------------ Editor

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: NoteRepository,
    tasksRepo: TaskRepository,
    private val time: TimeProvider,
) : LifeViewModel() {
    private val noteId: Long = savedState.toRoute<NoteRoute>().id
    val loaded = MutableStateFlow(noteId == 0L)
    var original: NoteEntity? = null
        private set
    var originalTags: List<String> = emptyList()
        private set
    val tasks: StateFlow<List<TaskEntity>> = tasksRepo.observeTasks().stateWhileSubscribed(emptyList())
    val projects: StateFlow<List<ProjectEntity>> = tasksRepo.observeProjects().stateWhileSubscribed(emptyList())

    init {
        if (noteId != 0L) viewModelScope.launch {
            original = repo.get(noteId)
            originalTags = repo.tagsFor(noteId)
            loaded.value = true
        }
    }

    fun save(n: NoteEntity, tags: List<String>, onDone: () -> Unit) = act("Note saved.") {
        repo.save(n, tags)
        onDone()
    }

    fun delete(onDone: () -> Unit) = act("Note deleted.") {
        original?.let { repo.delete(it) }
        onDone()
    }

    fun setArchived(archived: Boolean, onDone: () -> Unit) = act(if (archived) "Note archived." else "Note restored.") {
        original?.let { repo.setArchived(it, archived) }
        onDone()
    }

    fun blank() = NoteEntity(title = "", content = "", createdAt = time.now(), updatedAt = time.now())
}

@Composable
fun NoteEditorScreen(onBack: () -> Unit, vm: NoteEditorViewModel = hiltViewModel()) {
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    if (!loaded) return
    NoteEditorContent(vm, onBack)
}

@Composable
private fun NoteEditorContent(vm: NoteEditorViewModel, onBack: () -> Unit) {
    val colors = LifeTheme.colors
    val base = vm.original ?: vm.blank()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf(base.title) }
    var content by rememberSaveable { mutableStateOf(base.content) }
    var checklist by rememberSaveable { mutableStateOf(base.isChecklist) }
    var category by rememberSaveable { mutableStateOf(base.category.orEmpty()) }
    var tagsText by rememberSaveable { mutableStateOf(vm.originalTags.joinToString(" ") { "#$it" }) }
    var pinned by rememberSaveable { mutableStateOf(base.isPinned) }
    var taskId by rememberSaveable { mutableStateOf(base.taskId) }
    var projectId by rememberSaveable { mutableStateOf(base.projectId) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var newItem by rememberSaveable { mutableStateOf("") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val dirty = title != base.title || content != base.content || checklist != base.isChecklist || category != base.category.orEmpty() ||
        pinned != base.isPinned || taskId != base.taskId || projectId != base.projectId || tagsText != vm.originalTags.joinToString(" ") { "#$it" }
    val canSave = title.isNotBlank() || content.isNotBlank()

    fun doSave() = vm.save(
        base.copy(
            title = title.trim().ifBlank { content.lineSequence().firstOrNull { it.isNotBlank() }?.take(60) ?: "Untitled" },
            content = content, isChecklist = checklist, category = category.trim().ifBlank { null },
            isPinned = pinned, taskId = taskId, projectId = projectId,
        ),
        tagsText.split(' ', ',').filter { it.isNotBlank() },
        onBack,
    )

    BackHandler(enabled = dirty) { confirmDiscard = true }

    MessageBox(vm) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenContentPadding()),
        ) {
            ScreenHeader(if (vm.original == null) "New note" else "Edit note", onBack = { if (dirty) confirmDiscard = true else onBack() }) {
                if (vm.original != null) {
                    IconButton(onClick = { vm.setArchived(!base.isArchived, onBack) }) {
                        Icon(if (base.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, if (base.isArchived) "Restore" else "Archive", tint = colors.textPrimary)
                    }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete note", tint = colors.danger) }
                }
                LifePrimaryButton("Save", onClick = { doSave() }, enabled = canSave && (dirty || vm.original == null))
            }
            Column(Modifier.readableWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                LifeTextField(title, { title = it.take(120) }, "Title", Modifier.fillMaxWidth())
                SwitchRow("Checklist", checklist, { on ->
                    content = if (on) serializeChecklist(content.lines().filter { it.isNotBlank() }.map { it.trim() to false }) else parseChecklist(content).joinToString("\n") { it.first }
                    checklist = on
                })
                if (checklist) {
                    val items = parseChecklist(content)
                    items.forEachIndexed { i, (text, done) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { content = serializeChecklist(items.mapIndexed { j, p -> if (j == i) p.first to !p.second else p }) }) {
                                Icon(if (done) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, if (done) "Uncheck $text" else "Check $text",
                                    tint = if (done) colors.success else colors.textSecondary)
                            }
                            Text(text, style = MaterialTheme.typography.bodyLarge, color = if (done) colors.textTertiary else colors.textPrimary, modifier = Modifier.weight(1f))
                            TextButton(onClick = { content = serializeChecklist(items.filterIndexed { j, _ -> j != i }) }) { Text("Remove", color = colors.textTertiary) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LifeTextField(newItem, { newItem = it.take(120) }, "Add item", Modifier.weight(1f))
                        IconButton(onClick = { content = serializeChecklist(items + (newItem.trim() to false)); newItem = "" }, enabled = newItem.isNotBlank()) {
                            Icon(Icons.Outlined.Add, "Add item", tint = colors.accentCyan)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { content += "**bold**" }) { Text("B", fontWeight = FontWeight.Bold, color = colors.textPrimary) }
                        TextButton(onClick = { content += "*italic*" }) { Text("I", fontStyle = FontStyle.Italic, color = colors.textPrimary) }
                        TextButton(onClick = { content += (if (content.isEmpty() || content.endsWith("\n")) "" else "\n") + "# " }) { Text("H", color = colors.textPrimary) }
                        TextButton(onClick = { content += (if (content.isEmpty() || content.endsWith("\n")) "" else "\n") + "- " }) { Text("•", color = colors.textPrimary) }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { preview = !preview }) {
                            Icon(if (preview) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (preview) "Edit" else "Preview", tint = colors.textSecondary)
                        }
                    }
                    if (preview) {
                        GlassCard(Modifier.fillMaxWidth()) { Text(renderMarkdown(content), style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary) }
                    } else {
                        LifeTextField(content, { content = it.take(20_000) }, "Write… (supports # heading, **bold**, *italic*, - bullets)",
                            Modifier.fillMaxWidth().heightIn(min = 220.dp), singleLine = false)
                    }
                }
                LifeTextField(category, { category = it.take(30) }, "Category", Modifier.fillMaxWidth())
                LifeTextField(tagsText, { tagsText = it.take(200) }, "Tags (e.g. #ideas #home)", Modifier.fillMaxWidth())
                SwitchRow("Pinned", pinned, { pinned = it })
                val openTasks = tasks.filter { it.status != com.lifeos.app.domain.model.TaskStatus.DONE || it.id == taskId }
                if (openTasks.isNotEmpty()) {
                    FieldLabel("Linked task")
                    ChoiceChips(listOf<TaskEntity?>(null) + openTasks.take(30), openTasks.firstOrNull { it.id == taskId }, { it?.title?.take(30) ?: "None" }, { taskId = it?.id })
                }
                if (projects.isNotEmpty()) {
                    FieldLabel("Linked project")
                    ChoiceChips(listOf<ProjectEntity?>(null) + projects, projects.firstOrNull { it.id == projectId }, { it?.title ?: "None" }, { projectId = it?.id })
                }
                Spacer(Modifier.size(Spacing.xl))
            }
        }
    }

    if (confirmDelete) LifeConfirmDialog("Delete note?", "This note will be permanently deleted.", "Delete", onConfirm = { confirmDelete = false; vm.delete(onBack) },
        onDismiss = { confirmDelete = false }, destructive = true)
    if (confirmDiscard) LifeConfirmDialog("Discard changes?", "Your unsaved changes will be lost.", "Discard", onConfirm = { confirmDiscard = false; onBack() },
        onDismiss = { confirmDiscard = false }, destructive = true)
}
