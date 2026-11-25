package com.lifeos.app.data.repository

import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.dao.UserProfileDao
import com.lifeos.app.data.local.entity.UserProfileEntity
import com.lifeos.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val dao: UserProfileDao,
    private val time: TimeProvider,
) : ProfileRepository {

    override val displayName: Flow<String?> = dao.observe().map { it?.displayName?.takeIf(String::isNotBlank) }

    override suspend fun setDisplayName(name: String): Boolean {
        val clean = validateName(name) ?: return false
        val now = time.now()
        val existing = dao.observe().first()
        dao.upsert(
            UserProfileEntity(
                id = 1,
                displayName = clean,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
        return true
    }

    companion object {
        const val MAX_NAME_LENGTH = 40

        /** Trims and collapses whitespace; rejects blank, over-long or control-character names. */
        fun validateName(raw: String): String? {
            val clean = raw.trim().replace(Regex("\\s+"), " ")
            if (clean.isEmpty() || clean.length > MAX_NAME_LENGTH) return null
            if (clean.any { it.isISOControl() }) return null
            return clean
        }
    }
}
