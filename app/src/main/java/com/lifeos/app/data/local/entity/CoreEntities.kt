package com.lifeos.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/** Single-row profile (id is always 1). No account or registration exists. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(tableName = "family_members", indices = [Index("name")])
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relation: String,
    val birthDate: LocalDate? = null,
    val phone: String? = null,
    val isEmergencyContact: Boolean = false,
    val notes: String? = null,
    val createdAt: Instant,
)

/** [widgetKey] is the [WidgetType] name; stored as text so retired widget types never crash parsing. */
@Entity(tableName = "dashboard_widgets")
data class DashboardWidgetEntity(
    @PrimaryKey val widgetKey: String,
    val position: Int,
    val isVisible: Boolean,
)

@Entity(
    tableName = "ai_conversations",
    indices = [Index("updatedAt")],
)
data class AiConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiConversationEntity::class,
            parentColumns = ["id"], childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conversationId"), Index("createdAt")],
)
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: com.lifeos.app.domain.model.MessageRole,
    val text: String,
    /** e.g. "EXECUTED", "NEEDS_CONFIRMATION", "FAILED" — only set when a command touched data. */
    val actionStatus: String? = null,
    val createdAt: Instant,
)
