package com.lifeos.app.data.repository

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.lifeos.app.core.format.IndianFormat
import com.lifeos.app.core.security.BackupCrypto
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.LifeOsDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.reflect.Modifier
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted, user-controlled backup of the whole database to a file the user picks. */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: LifeOsDatabase,
) {
    suspend fun createBackup(target: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        require(password.size >= 8) { "Use a password of at least 8 characters." }
        // Flush the write-ahead log so the main file holds everything.
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val bytes = context.getDatabasePath(LifeOsDatabase.NAME).readBytes()
        val encrypted = BackupCrypto.encrypt(bytes, password)
        context.contentResolver.openOutputStream(target, "wt")?.use { it.write(encrypted) } ?: error("Couldn't write the backup file.")
    }

    /**
     * Validates and restores a backup. The current data is replaced only after the file has been
     * decrypted and checked, so a wrong password or a damaged file changes nothing.
     * The app must restart afterwards.
     */
    suspend fun restoreBackup(source: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val data = context.contentResolver.openInputStream(source)?.use { it.readBytes() } ?: error("Couldn't read the backup file.")
        val plain = try {
            BackupCrypto.decrypt(data, password)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            error("Wrong password, or the backup file is damaged.")
        }
        val header = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        require(plain.size > 100 && plain.copyOfRange(0, 16).contentEquals(header)) { "The backup doesn't contain a valid database." }
        val version = ((plain[60].toInt() and 0xFF) shl 24) or ((plain[61].toInt() and 0xFF) shl 16) or
            ((plain[62].toInt() and 0xFF) shl 8) or (plain[63].toInt() and 0xFF)
        require(version in 1..CURRENT_DB_VERSION) { "This backup was made by a newer version of LIFE OS. Update the app first." }

        val path = context.getDatabasePath(LifeOsDatabase.NAME)
        val staging = File(path.parentFile, "restore.tmp")
        staging.writeBytes(plain)
        db.close()
        File(path.path + "-wal").delete()
        File(path.path + "-shm").delete()
        if (!staging.renameTo(path)) {
            path.writeBytes(plain)
            staging.delete()
        }
    }

    companion object {
        const val CURRENT_DB_VERSION = 1
    }
}

/** Exports personal records as JSON (everything) or CSV (transactions / tasks) to a user-chosen file. */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: LifeOsDatabase,
) {
    suspend fun exportJson(target: Uri) = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "LIFE OS")
        root.put("format", 1)
        root.put("exportedAt", java.time.Instant.now().toString())
        fun put(name: String, rows: List<Any>) = root.put(name, JSONArray(rows.map { toJson(it) }))
        put("accounts", db.accountDao().getAll())
        put("transactions", db.transactionDao().getAll())
        put("budgets", db.budgetDao().getAll())
        put("savingsGoals", db.savingsGoalDao().getAll())
        put("projects", db.projectDao().getAll())
        put("tasks", db.taskDao().getAll())
        put("subtasks", db.subTaskDao().getAll())
        put("notes", db.noteDao().getAll())
        put("familyMembers", db.familyMemberDao().getAll())
        put("healthRecords", db.healthDao().getAllRecords())
        put("medications", db.healthDao().getAllMedications())
        put("appointments", db.healthDao().getAllAppointments())
        put("events", db.calendarEventDao().getAll())
        put("reminders", db.reminderDao().getAll())
        put("documents", db.documentDao().getAll())
        put("goals", db.goalDao().getAll())
        put("habits", db.habitDao().getAll())
        put("habitEntries", db.habitDao().getAllEntries())
        write(target, root.toString(2))
    }

    suspend fun exportTransactionsCsv(target: Uri) = withContext(Dispatchers.IO) {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val zone = java.time.ZoneId.systemDefault()
        val sb = StringBuilder("date,type,category,amount_inr,note\n")
        db.transactionDao().getAll().forEach { t ->
            sb.append(fmt.format(t.occurredAt.atZone(zone).toLocalDateTime())).append(',')
                .append(t.type.name).append(',').append(t.category.label).append(',')
                .append(IndianFormat.rupees(t.amountPaise).removePrefix("₹").replace(",", "")).append(',')
                .append(csv(t.note)).append('\n')
        }
        write(target, sb.toString())
    }

    suspend fun exportTasksCsv(target: Uri) = withContext(Dispatchers.IO) {
        val zone = java.time.ZoneId.systemDefault()
        val sb = StringBuilder("title,status,priority,due,completed,category\n")
        db.taskDao().getAll().forEach { t ->
            sb.append(csv(t.title)).append(',').append(t.status.name).append(',').append(t.priority.label).append(',')
                .append(t.dueAt?.atZone(zone)?.toLocalDateTime()?.toString().orEmpty()).append(',')
                .append(t.completedAt?.atZone(zone)?.toLocalDateTime()?.toString().orEmpty()).append(',')
                .append(csv(t.category)).append('\n')
        }
        write(target, sb.toString())
    }

    private fun write(target: Uri, text: String) {
        context.contentResolver.openOutputStream(target, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: error("Couldn't write the export file.")
    }

    private fun csv(v: String?): String {
        if (v == null) return ""
        val needsQuote = v.any { it == ',' || it == '"' || it == '\n' }
        // Neutralise spreadsheet formula injection.
        val safe = if (v.startsWith("=") || v.startsWith("+") || v.startsWith("-") || v.startsWith("@")) "'$v" else v
        return if (needsQuote) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    /** Reflection-based row → JSON (entity fields are kept by R8 rules). */
    private fun toJson(row: Any): JSONObject {
        val o = JSONObject()
        row.javaClass.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }.forEach { f ->
            f.isAccessible = true
            when (val v = f.get(row)) {
                null -> o.put(f.name, JSONObject.NULL)
                is Number, is Boolean, is String -> o.put(f.name, v)
                else -> o.put(f.name, v.toString())
            }
        }
        return o
    }
}

data class ModelInfo(val fileName: String, val sizeBytes: Long)

data class DeviceCapacity(val totalRamBytes: Long, val availableRamBytes: Long, val freeStorageBytes: Long)

/**
 * Optional local LLM: manages a user-imported GGUF model file. Models are never bundled or
 * downloaded automatically. This build includes model import + device checks; the llama.cpp
 * inference runtime is not compiled into it, so LIFE AI stays in basic (rule-based) mode.
 */
@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val time: TimeProvider,
) {
    private val dir: File get() = File(context.filesDir, "models").apply { mkdirs() }

    val runtimeAvailable: Boolean = false

    fun installed(): ModelInfo? = dir.listFiles()?.firstOrNull { it.name.endsWith(".gguf") }?.let { ModelInfo(it.name, it.length()) }

    fun capacity(): DeviceCapacity {
        val am = context.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return DeviceCapacity(mi.totalMem, mi.availMem, context.filesDir.usableSpace)
    }

    /** Returns a reason the model can't be imported, or null if it's OK. */
    fun checkFits(sizeBytes: Long): String? {
        val c = capacity()
        return when {
            c.freeStorageBytes < sizeBytes + 512L * 1024 * 1024 -> "Not enough free storage (needs the model size plus 512 MB spare)."
            c.totalRamBytes < sizeBytes * 2 + 1024L * 1024 * 1024 -> "This device's RAM is too small for this model. Choose a smaller quantized model (e.g. 1–3B parameters, Q4)."
            else -> null
        }
    }

    suspend fun import(uri: Uri): ModelInfo = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name = "model.gguf"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let { size = c.getLong(it) }
            }
        }
        val magic = resolver.openInputStream(uri)?.use { s -> ByteArray(4).also { s.read(it) } } ?: error("Couldn't read the file.")
        require(String(magic, Charsets.US_ASCII) == "GGUF") { "That file isn't a GGUF model." }
        if (size > 0) checkFits(size)?.let { error(it) }
        dir.listFiles()?.forEach { it.delete() }
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_").let { if (it.endsWith(".gguf")) it else "$it.gguf" }
        val out = File(dir, safeName)
        resolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it, 1 shl 20) } }
        ModelInfo(out.name, out.length())
    }

    fun delete() {
        dir.listFiles()?.forEach { it.delete() }
    }
}
