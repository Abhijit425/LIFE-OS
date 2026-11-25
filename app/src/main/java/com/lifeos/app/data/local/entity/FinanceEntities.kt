package com.lifeos.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifeos.app.domain.model.AccountType
import com.lifeos.app.domain.model.Recurrence
import com.lifeos.app.domain.model.TransactionCategory
import com.lifeos.app.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate

/**
 * A manually tracked account. LIFE OS never connects to banks; balances are what the user enters.
 * For LOAN accounts, [emiPaise]/[emiDayOfMonth] drive EMI reminders.
 */
@Entity(tableName = "accounts", indices = [Index("type")])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val openingBalancePaise: Long = 0,
    val creditLimitPaise: Long? = null,
    val emiPaise: Long? = null,
    val emiDayOfMonth: Int? = null,
    val isArchived: Boolean = false,
    val createdAt: Instant,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = FamilyMemberEntity::class, parentColumns = ["id"], childColumns = ["familyMemberId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("occurredAt"), Index("accountId"), Index("familyMemberId"), Index(value = ["type", "occurredAt"])],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    /** Always positive; [type] carries the direction. */
    val amountPaise: Long,
    val category: TransactionCategory,
    val note: String? = null,
    val occurredAt: Instant,
    val accountId: Long? = null,
    val familyMemberId: Long? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val createdAt: Instant,
)

/** Monthly budget per category. [monthKey] = year * 100 + month (e.g. 202611). */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category", "monthKey"], unique = true)],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: TransactionCategory,
    val monthKey: Int,
    val limitPaise: Long,
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetPaise: Long,
    val savedPaise: Long = 0,
    val targetDate: LocalDate? = null,
    val createdAt: Instant,
)
