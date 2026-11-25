package com.lifeos.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.lifeos.app.core.security.VaultCrypto
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.LifeOsDatabase
import com.lifeos.app.data.local.entity.DocumentEntity
import com.lifeos.app.data.local.entity.DocumentFolderEntity
import com.lifeos.app.data.local.entity.ReminderEntity
import com.lifeos.app.domain.model.DocumentCategory
import com.lifeos.app.domain.model.DocumentStorage
import com.lifeos.app.domain.model.ReminderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(val documentId: Long, val storage: DocumentStorage, val note: String?)

/**
 * Private document vault. Files are picked with the Storage Access Framework and never uploaded.
 * - REFERENCE keeps a persistable read permission to the original file (no copy).
 * - APP_PRIVATE copies into app storage — used when the source can't grant lasting access
 *   (e.g. some cloud or download providers) or when the user wants a copy that survives the original being moved.
 * - ENCRYPTED copies and encrypts with an Android Keystore key.
 */
@Singleton
class DocumentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: LifeOsDatabase,
    private val reminders: ReminderEngine,
    private val time: TimeProvider,
) {
    private val vaultDir: File get() = File(context.filesDir, "vault").apply { mkdirs() }

    fun observeAll(): Flow<List<DocumentEntity>> = db.documentDao().observeAll()
    fun observeFolders(): Flow<List<DocumentFolderEntity>> = db.documentFolderDao().observeAll()
    suspend fun get(id: Long) = db.documentDao().getById(id)

    suspend fun import(
        uri: Uri,
        requested: DocumentStorage,
        category: DocumentCategory,
        folderId: Long?,
    ): ImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name = "Document"
        var size: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let { size = c.getLong(it) }
            }
        }
        val mime = resolver.getType(uri)
        if ((size ?: 0L) > MAX_COPY_BYTES && requested != DocumentStorage.REFERENCE) {
            error("This file is larger than 50 MB. Store it as a reference instead of a copy.")
        }

        var storage = requested
        var note: String? = null
        val location: String = when (requested) {
            DocumentStorage.REFERENCE -> try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uri.toString()
            } catch (e: SecurityException) {
                // The provider doesn't offer lasting access, so a private copy is the only reliable option.
                storage = DocumentStorage.APP_PRIVATE
                note = "This app couldn't keep access to the original, so a private copy was saved instead."
                copyPlain(uri)
            }
            DocumentStorage.APP_PRIVATE -> copyPlain(uri)
            DocumentStorage.ENCRYPTED -> copyEncrypted(uri)
        }
        val id = db.documentDao().insert(
            DocumentEntity(
                displayName = name, location = location, storage = storage, mimeType = mime, sizeBytes = size,
                category = category, folderId = folderId, addedAt = time.now(),
            ),
        )
        ImportResult(id, storage, note)
    }

    private fun copyPlain(uri: Uri): String {
        val file = File(vaultDir, UUID.randomUUID().toString())
        context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            ?: error("Couldn't read the selected file.")
        return file.name
    }

    private fun copyEncrypted(uri: Uri): String {
        val file = File(vaultDir, UUID.randomUUID().toString() + ".enc")
        context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { VaultCrypto.encrypt(input, it) } }
            ?: error("Couldn't read the selected file.")
        return file.name
    }

    /** Opens the document's bytes for preview. Caller closes the stream. */
    fun open(doc: DocumentEntity): InputStream? = when (doc.storage) {
        DocumentStorage.REFERENCE -> runCatching { context.contentResolver.openInputStream(Uri.parse(doc.location)) }.getOrNull()
        DocumentStorage.APP_PRIVATE -> File(vaultDir, doc.location).takeIf { it.exists() }?.let { FileInputStream(it) }
        DocumentStorage.ENCRYPTED -> File(vaultDir, doc.location).takeIf { it.exists() }?.let { VaultCrypto.decryptStream(FileInputStream(it)) }
    }

    suspend fun update(doc: DocumentEntity) {
        db.documentDao().update(doc)
        syncExpiryReminder(doc)
    }

    /** Removes the record and our private copy. A referenced original file is never touched. */
    suspend fun delete(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        reminders.cancelReminderAlarms(db.reminderDao().getForDocument(doc.id))
        when (doc.storage) {
            DocumentStorage.REFERENCE -> runCatching {
                context.contentResolver.releasePersistableUriPermission(Uri.parse(doc.location), Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            else -> File(vaultDir, doc.location).delete()
        }
        db.documentDao().delete(doc)
    }

    /** Reminder 7 days before a document expires (passport, insurance, licence…). */
    private suspend fun syncExpiryReminder(doc: DocumentEntity) {
        db.reminderDao().getForDocument(doc.id).forEach { reminders.delete(it) }
        val exp = doc.expiresOn ?: return
        val zone = time.zone()
        val at = exp.minusDays(7).atTime(LocalTime.of(9, 0)).atZone(zone).toInstant()
        val trigger = if (at.isAfter(time.now())) at else exp.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant()
        if (trigger.isAfter(time.now())) {
            reminders.save(
                ReminderEntity(
                    title = "${doc.displayName} expires soon", message = "Expires on $exp", type = ReminderType.DOCUMENT,
                    triggerAt = trigger, documentId = doc.id, createdAt = time.now(),
                ),
            )
        }
    }

    suspend fun addFolder(name: String) {
        if (name.isNotBlank()) db.documentFolderDao().insert(DocumentFolderEntity(name = name.trim().take(40), createdAt = time.now()))
    }

    /** Documents in a deleted folder stay in the vault, unfiled. */
    suspend fun deleteFolder(f: DocumentFolderEntity) = db.documentFolderDao().delete(f)

    companion object {
        const val MAX_COPY_BYTES = 50L * 1024 * 1024
    }
}
