package com.lifeos.app.feature.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.lifeos.app.core.designsystem.component.ChoiceChips
import com.lifeos.app.core.designsystem.component.DateField
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.ErrorState
import com.lifeos.app.core.designsystem.component.FieldLabel
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeSecondaryButton
import com.lifeos.app.core.designsystem.component.LifeSheet
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.ListRow
import com.lifeos.app.core.designsystem.component.LoadingState
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.component.SearchField
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.entity.DocumentEntity
import com.lifeos.app.data.local.entity.DocumentFolderEntity
import com.lifeos.app.data.local.entity.FamilyMemberEntity
import com.lifeos.app.data.repository.DocumentRepository
import com.lifeos.app.data.repository.FamilyRepository
import com.lifeos.app.domain.model.DocumentCategory
import com.lifeos.app.domain.model.DocumentStorage
import com.lifeos.app.feature.common.LifeViewModel
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.navigation.DocumentRoute
import com.lifeos.app.navigation.screenContentPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val repo: DocumentRepository,
    family: FamilyRepository,
    private val time: TimeProvider,
) : LifeViewModel() {
    val documents = repo.observeAll().stateWhileSubscribed(emptyList())
    val folders = repo.observeFolders().stateWhileSubscribed(emptyList())
    val members = family.observeMembers().stateWhileSubscribed(emptyList())
    fun today() = time.now().atZone(time.zone()).toLocalDate()

    fun import(uri: Uri, storage: DocumentStorage, category: DocumentCategory, folderId: Long?) = act {
        val r = repo.import(uri, storage, category, folderId)
        say(r.note ?: "Document added to the vault.")
    }
    fun update(d: DocumentEntity) = act("Document saved.") { repo.update(d) }
    fun delete(d: DocumentEntity) = act("Removed from the vault.") { repo.delete(d) }
    fun addFolder(name: String) = act("Folder created.") { repo.addFolder(name) }
    fun deleteFolder(f: DocumentFolderEntity) = act("Folder deleted. Its documents are kept.") { repo.deleteFolder(f) }
}

@Composable
fun DocumentsScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, vm: DocumentsViewModel = hiltViewModel()) {
    val docs by vm.documents.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<DocumentCategory?>(null) }
    var folder by rememberSaveable { mutableStateOf<Long?>(null) }
    var importCategory by rememberSaveable { mutableStateOf(DocumentCategory.PERSONAL) }
    var importStorage by rememberSaveable { mutableStateOf(DocumentStorage.REFERENCE) }
    var showImport by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DocumentEntity?>(null) }
    val colors = LifeTheme.colors
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import(uri, importStorage, importCategory, folder)
    }
    val filtered = docs.filter { d ->
        (category == null || d.category == category) && (folder == null || d.folderId == folder) &&
            (query.isBlank() || d.displayName.contains(query, true) || d.tags.contains(query.lowercase()) || d.description.orEmpty().contains(query, true))
    }

    MessageBox(vm) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = screenContentPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            item {
                ScreenHeader("Documents", "Private vault on this device. Nothing is ever uploaded.", onBack = onBack) {
                    IconButton(onClick = { newFolder = true }) { Icon(Icons.Outlined.CreateNewFolder, "New folder", tint = colors.textPrimary) }
                    LifePrimaryButton("Import", onClick = { showImport = true }, icon = Icons.Outlined.Add)
                }
                SearchField(query, { query = it }, "Search names, tags, descriptions")
                Spacer(Modifier.height(Spacing.xs))
                ChoiceChips(listOf<DocumentCategory?>(null) + DocumentCategory.entries, category, { it?.label ?: "All categories" }, { category = it })
                if (folders.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xs))
                    ChoiceChips(listOf<DocumentFolderEntity?>(null) + folders, folders.firstOrNull { it.id == folder }, { it?.name ?: "All folders" }, { folder = it?.id })
                }
            }
            if (filtered.isEmpty()) item {
                GlassCard { EmptyState(Icons.Outlined.Description, if (docs.isEmpty()) "Your vault is empty" else "No matches", "Import PDFs, images or text files. Choose reference, private copy or encrypted copy.") }
            }
            items(filtered, key = { it.id }) { d ->
                GlassCard(onClick = { onOpen(d.id) }, onClickLabel = "Preview ${d.displayName}") {
                    ListRow(
                        d.displayName,
                        listOfNotNull(
                            d.category.label,
                            folders.firstOrNull { it.id == d.folderId }?.name,
                            members.firstOrNull { it.id == d.familyMemberId }?.name,
                            d.expiresOn?.let { "expires ${TimeFormat.shortDate(it)} ${it.year}" },
                            d.sizeBytes?.let { formatSize(it) },
                        ).joinToString(" · "),
                        leading = {
                            Icon(if (d.storage == DocumentStorage.ENCRYPTED) Icons.Outlined.Lock else Icons.Outlined.Description, null,
                                tint = if (d.storage == DocumentStorage.ENCRYPTED) colors.accentPurple else colors.accentCyan)
                        },
                        trailing = { IconButton(onClick = { editing = d }) { Icon(Icons.Outlined.Edit, "Edit ${d.displayName}", tint = colors.textSecondary) } },
                    )
                    d.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 2) }
                    if (d.tags.isNotBlank()) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { d.tags.split(',').filter { it.isNotBlank() }.take(4).forEach { LifeChip("#$it", colors.accentPurple) } }
                }
            }
            if (folders.isNotEmpty()) item {
                GlassCard {
                    Text("Folders", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    folders.forEach { f ->
                        ListRow(f.name, "${docs.count { it.folderId == f.id }} documents", trailing = {
                            TextButton(onClick = { vm.deleteFolder(f) }) { Text("Delete", color = colors.textTertiary) }
                        })
                    }
                }
            }
        }
    }

    if (showImport) {
        LifeSheet("Import a document", { showImport = false }, onSave = { showImport = false; picker.launch(arrayOf("application/pdf", "image/*", "text/*")) }, saveLabel = "Choose file") {
            FieldLabel("Category")
            ChoiceChips(DocumentCategory.entries, importCategory, { it.label }, { importCategory = it })
            FieldLabel("How to store it")
            ChoiceChips(DocumentStorage.entries, importStorage, { storageLabel(it) }, { importStorage = it })
            Text(
                when (importStorage) {
                    DocumentStorage.REFERENCE -> "Keeps a link to the original file — no extra space used. If the original is moved or deleted, the vault entry stops working. Some sources (e.g. certain cloud apps) can't grant lasting access; then a private copy is made automatically and you'll be told."
                    DocumentStorage.APP_PRIVATE -> "Copies the file into LIFE OS's private storage (up to 50 MB). The copy stays even if the original is deleted."
                    DocumentStorage.ENCRYPTED -> "Copies and encrypts the file with a key kept in Android Keystore. Opening it requires your screen lock or biometrics. Encrypted copies can only be read on this device."
                },
                style = MaterialTheme.typography.bodySmall, color = colors.textSecondary,
            )
        }
    }
    if (newFolder) {
        var name by remember { mutableStateOf("") }
        LifeSheet("New folder", { newFolder = false }, onSave = { vm.addFolder(name); newFolder = false }, saveEnabled = name.isNotBlank()) {
            LifeTextField(name, { name = it.take(40) }, "Folder name", Modifier.fillMaxWidth())
        }
    }
    editing?.let { d -> DocumentEditSheet(d, folders, members, vm) { editing = null } }
}

fun storageLabel(s: DocumentStorage) = when (s) {
    DocumentStorage.REFERENCE -> "Reference original"
    DocumentStorage.APP_PRIVATE -> "Private copy"
    DocumentStorage.ENCRYPTED -> "Encrypted copy"
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

@Composable
private fun DocumentEditSheet(d: DocumentEntity, folders: List<DocumentFolderEntity>, members: List<FamilyMemberEntity>, vm: DocumentsViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(d.displayName) }
    var description by remember { mutableStateOf(d.description.orEmpty()) }
    var tags by remember { mutableStateOf(d.tags.split(',').filter { it.isNotBlank() }.joinToString(" ") { "#$it" }) }
    var category by remember { mutableStateOf(d.category) }
    var folderId by remember { mutableStateOf(d.folderId) }
    var memberId by remember { mutableStateOf(d.familyMemberId) }
    var expires by remember { mutableStateOf(d.expiresOn) }
    LifeSheet("Document details", onDismiss, saveEnabled = name.isNotBlank(),
        onSave = {
            vm.update(d.copy(
                displayName = name.trim(), description = description.trim().ifBlank { null },
                tags = tags.split(' ', ',').map { it.trim().removePrefix("#").lowercase() }.filter { it.isNotBlank() }.distinct().joinToString(","),
                category = category, folderId = folderId, familyMemberId = memberId, expiresOn = expires))
            onDismiss()
        },
        onDelete = { vm.delete(d); onDismiss() },
        deleteMessage = if (d.storage == DocumentStorage.REFERENCE) "Removes it from the vault. Your original file is not touched." else "Removes it from the vault and deletes LIFE OS's private copy.",
    ) {
        LifeTextField(name, { name = it.take(120) }, "Name", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(description, { description = it.take(500) }, "Description", Modifier.fillMaxWidth(), singleLine = false)
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(tags, { tags = it.take(200) }, "Tags (e.g. #passport #id)", Modifier.fillMaxWidth())
        FieldLabel("Category")
        ChoiceChips(DocumentCategory.entries, category, { it.label }, { category = it })
        if (folders.isNotEmpty()) {
            FieldLabel("Folder")
            ChoiceChips(listOf<DocumentFolderEntity?>(null) + folders, folders.firstOrNull { it.id == folderId }, { it?.name ?: "None" }, { folderId = it?.id })
        }
        if (members.isNotEmpty()) {
            FieldLabel("Belongs to")
            ChoiceChips(listOf<FamilyMemberEntity?>(null) + members, members.firstOrNull { it.id == memberId }, { it?.name ?: "Me" }, { memberId = it?.id })
        }
        DateField("Expiry date (reminder 7 days before)", expires, { expires = it })
        Text("Stored as: ${storageLabel(d.storage)}", style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textTertiary)
    }
}

// ------------------------------------------------------------------ Preview

sealed interface PreviewState {
    data object Loading : PreviewState
    data object Locked : PreviewState
    data class Images(val pages: List<Bitmap>, val pageCount: Int) : PreviewState
    data class TextContent(val text: String) : PreviewState
    data class Unsupported(val reason: String) : PreviewState
    data class Failed(val reason: String) : PreviewState
}

@HiltViewModel
class DocumentPreviewViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: DocumentRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : LifeViewModel() {
    private val id = savedState.toRoute<DocumentRoute>().id
    val doc = MutableStateFlow<DocumentEntity?>(null)
    val state = MutableStateFlow<PreviewState>(PreviewState.Loading)

    init {
        viewModelScope.launch {
            val d = repo.get(id)
            doc.value = d
            if (d == null) state.value = PreviewState.Failed("This document no longer exists.")
            else if (d.storage == DocumentStorage.ENCRYPTED) state.value = PreviewState.Locked
            else render(d)
        }
    }

    fun unlocked() {
        doc.value?.let { d -> viewModelScope.launch { render(d) } }
    }

    private suspend fun render(d: DocumentEntity) {
        state.value = PreviewState.Loading
        state.value = withContext(Dispatchers.IO) {
            try {
                val mime = d.mimeType.orEmpty()
                when {
                    mime.startsWith("image/") -> repo.open(d)?.use { s ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                        BitmapFactory.decodeStream(s, null, opts)?.let { PreviewState.Images(listOf(it), 1) }
                    } ?: PreviewState.Failed("Couldn't open the image. If it was a reference, the original may have moved.")
                    mime == "application/pdf" -> renderPdf(d)
                    mime.startsWith("text/") -> repo.open(d)?.use { s ->
                        PreviewState.TextContent(s.readBytes().take(200_000).toByteArray().toString(Charsets.UTF_8))
                    } ?: PreviewState.Failed("Couldn't open the file.")
                    else -> PreviewState.Unsupported("Preview isn't available for this file type.")
                }
            } catch (e: Exception) {
                PreviewState.Failed(e.message ?: "Couldn't open this document.")
            }
        }
    }

    /** PdfRenderer needs a seekable file, so non-plain files are decrypted to a temporary cache file first. */
    private fun renderPdf(d: DocumentEntity): PreviewState {
        val tmp = File(context.cacheDir, "preview.pdf")
        try {
            val pfd: ParcelFileDescriptor = if (d.storage == DocumentStorage.REFERENCE) {
                context.contentResolver.openFileDescriptor(Uri.parse(d.location), "r") ?: return PreviewState.Failed("Couldn't open the PDF.")
            } else {
                repo.open(d)?.use { input -> tmp.outputStream().use { input.copyTo(it) } } ?: return PreviewState.Failed("Couldn't open the PDF.")
                ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            pfd.use { fd ->
                PdfRenderer(fd).use { renderer ->
                    val count = renderer.pageCount
                    val pages = (0 until minOf(count, 10)).map { i ->
                        renderer.openPage(i).use { page ->
                            val scale = 1400f / page.width.coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    }
                    return PreviewState.Images(pages, count)
                }
            }
        } finally {
            tmp.delete()
        }
    }

    override fun onCleared() {
        (state.value as? PreviewState.Images)?.pages?.forEach { it.recycle() }
    }
}

@Composable
fun DocumentPreviewScreen(onBack: () -> Unit, vm: DocumentPreviewViewModel = hiltViewModel()) {
    val doc by vm.doc.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = LifeTheme.colors
    var authError by remember { mutableStateOf<String?>(null) }

    fun authenticate() {
        val activity = context as? FragmentActivity ?: return
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(context).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            authError = "Set up a screen lock (PIN, pattern, password or biometrics) to open encrypted documents."
            return
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { authError = null; vm.unlocked() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { authError = errString.toString() }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock document").setSubtitle(doc?.displayName ?: "").setAllowedAuthenticators(authenticators).build())
    }
    LaunchedEffect(state) { if (state == PreviewState.Locked) authenticate() }

    MessageBox(vm) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = screenContentPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            item {
                ScreenHeader(doc?.displayName ?: "Document", doc?.let { "${it.category.label} · ${storageLabel(it.storage)}" }, onBack = onBack) {
                    val d = doc
                    if (d != null && d.storage == DocumentStorage.REFERENCE) {
                        IconButton(onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(d.location), d.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            } catch (e: ActivityNotFoundException) {
                                authError = "No app on this device can open this file type."
                            } catch (e: SecurityException) {
                                authError = "Access to the original file was lost. Re-import it."
                            }
                        }) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Open with another app", tint = colors.textPrimary) }
                    }
                }
                authError?.let { GlassCard { ErrorState(it) } }
            }
            when (val s = state) {
                PreviewState.Loading -> item { GlassCard { LoadingState() } }
                PreviewState.Locked -> item {
                    GlassCard {
                        EmptyState(Icons.Outlined.Lock, "Encrypted document", "Authenticate to decrypt and view it.")
                        LifeSecondaryButton("Unlock", onClick = { authenticate() })
                    }
                }
                is PreviewState.Images -> {
                    items(s.pages.size) { i ->
                        Image(s.pages[i].asImageBitmap(), "Page ${i + 1}", Modifier.fillMaxWidth().background(Color.White), contentScale = ContentScale.FillWidth)
                    }
                    if (s.pageCount > s.pages.size) item {
                        Text("Showing the first ${s.pages.size} of ${s.pageCount} pages.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
                is PreviewState.TextContent -> item { GlassCard { Text(s.text, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary) } }
                is PreviewState.Unsupported -> item { GlassCard { EmptyState(Icons.Outlined.Description, "No preview", s.reason) } }
                is PreviewState.Failed -> item { GlassCard { ErrorState(s.reason) } }
            }
            item { Column(Modifier.padding(bottom = Spacing.xl)) {} }
        }
    }
}
