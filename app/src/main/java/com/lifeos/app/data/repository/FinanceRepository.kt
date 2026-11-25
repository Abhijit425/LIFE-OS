package com.lifeos.app.data.repository

import androidx.room.withTransaction
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.core.time.monthKey
import com.lifeos.app.core.time.monthRange
import com.lifeos.app.data.local.LifeOsDatabase
import com.lifeos.app.data.local.dao.CategoryTotal
import com.lifeos.app.data.local.entity.AccountEntity
import com.lifeos.app.data.local.entity.BudgetEntity
import com.lifeos.app.data.local.entity.GoalEntity
import com.lifeos.app.data.local.entity.SavingsGoalEntity
import com.lifeos.app.data.local.entity.TransactionEntity
import com.lifeos.app.domain.logic.RecurrenceMath
import com.lifeos.app.domain.model.AccountType
import com.lifeos.app.domain.model.GoalType
import com.lifeos.app.domain.model.Recurrence
import com.lifeos.app.domain.model.TransactionCategory
import com.lifeos.app.domain.model.TransactionType
import com.lifeos.app.domain.usecase.DashboardMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

data class AccountWithBalance(val account: AccountEntity, val balancePaise: Long)

data class BudgetStatus(val budget: BudgetEntity, val spentPaise: Long) {
    val progress: Float get() = if (budget.limitPaise <= 0) 0f else spentPaise.toFloat() / budget.limitPaise
}

data class MonthFlow(val month: YearMonth, val incomePaise: Long, val expensePaise: Long)

/** Manual personal finance tracker. Nothing here talks to a bank. */
@Singleton
class FinanceRepository @Inject constructor(
    private val db: LifeOsDatabase,
    private val time: TimeProvider,
) {
    private val tx get() = db.transactionDao()

    fun currentMonth(): YearMonth = YearMonth.from(time.now().atZone(time.zone()))

    fun observeMonth(month: YearMonth): Flow<List<TransactionEntity>> {
        val r = month.monthRange(time.zone())
        return tx.observeBetween(r.start, r.end)
    }

    fun observeCategoryTotals(month: YearMonth, type: TransactionType = TransactionType.EXPENSE): Flow<List<CategoryTotal>> {
        val r = month.monthRange(time.zone())
        return tx.observeCategoryTotals(type.name, r.start, r.end)
    }

    fun observeSixMonths(endMonth: YearMonth): Flow<List<MonthFlow>> {
        val zone = time.zone()
        val start = endMonth.minusMonths(5).monthRange(zone).start
        return combine(
            tx.observeAmountsSince(TransactionType.INCOME.name, start),
            tx.observeAmountsSince(TransactionType.EXPENSE.name, start),
        ) { inc, exp ->
            val i = DashboardMath.bucketByMonth(inc.map { it.amountPaise to it.occurredAt }, endMonth, zone)
            val e = DashboardMath.bucketByMonth(exp.map { it.amountPaise to it.occurredAt }, endMonth, zone)
            i.zip(e).map { (a, b) -> MonthFlow(a.month, a.paise, b.paise) }
        }
    }

    fun observeAccounts(): Flow<List<AccountWithBalance>> =
        combine(db.accountDao().observeActive(), tx.observeAccountFlows()) { accounts, flows ->
            val byId = flows.associateBy { it.accountId }
            accounts.map { a ->
                val f = byId[a.id]
                val net = (f?.incomePaise ?: 0) - (f?.expensePaise ?: 0)
                // For credit cards and loans, "balance" is what is owed: opening + spending − payments.
                val balance = when (a.type) {
                    AccountType.CREDIT_CARD, AccountType.LOAN -> a.openingBalancePaise - net
                    else -> a.openingBalancePaise + net
                }
                AccountWithBalance(a, balance)
            }
        }

    fun observeBudgets(month: YearMonth): Flow<List<BudgetStatus>> =
        combine(db.budgetDao().observeForMonth(month.monthKey()), observeCategoryTotals(month)) { budgets, totals ->
            val spent = totals.associate { it.category to it.totalPaise }
            budgets.map { BudgetStatus(it, spent[it.category] ?: 0L) }
        }

    fun observeSavingsGoals(): Flow<List<SavingsGoalEntity>> = db.savingsGoalDao().observeAll()

    suspend fun saveTransaction(t: TransactionEntity) {
        require(t.amountPaise > 0) { "Amount must be positive" }
        if (t.id == 0L) tx.insert(t) else tx.update(t)
    }

    suspend fun deleteTransaction(t: TransactionEntity) = tx.delete(t)

    suspend fun saveAccount(a: AccountEntity) {
        if (a.id == 0L) db.accountDao().insert(a) else db.accountDao().update(a)
    }

    /** Deleting an account keeps its transactions (they become unlinked). */
    suspend fun deleteAccount(a: AccountEntity) = db.accountDao().delete(a)

    suspend fun setBudget(category: TransactionCategory, month: YearMonth, limitPaise: Long) {
        db.budgetDao().upsert(BudgetEntity(category = category, monthKey = month.monthKey(), limitPaise = limitPaise))
    }

    suspend fun deleteBudget(b: BudgetEntity) = db.budgetDao().delete(b)

    /** Savings goals are mirrored into the Goals module so they appear in goal progress. */
    suspend fun saveSavingsGoal(g: SavingsGoalEntity): Long = db.withTransaction {
        val id = if (g.id == 0L) db.savingsGoalDao().insert(g) else g.id.also { db.savingsGoalDao().update(g) }
        val linked = db.goalDao().getAll().firstOrNull { it.savingsGoalId == id }
        val mirror = GoalEntity(
            id = linked?.id ?: 0,
            title = g.title,
            type = GoalType.SAVINGS,
            targetValue = g.targetPaise / 100.0,
            currentValue = g.savedPaise / 100.0,
            unit = "₹",
            targetDate = g.targetDate,
            savingsGoalId = id,
            isCompleted = g.savedPaise >= g.targetPaise,
            createdAt = linked?.createdAt ?: g.createdAt,
        )
        if (linked == null) db.goalDao().insert(mirror) else db.goalDao().update(mirror)
        id
    }

    suspend fun addToSavings(goal: SavingsGoalEntity, paise: Long) {
        saveSavingsGoal(goal.copy(savedPaise = (goal.savedPaise + paise).coerceAtLeast(0)))
    }

    suspend fun deleteSavingsGoal(g: SavingsGoalEntity) = db.withTransaction {
        db.goalDao().getAll().filter { it.savingsGoalId == g.id }.forEach { db.goalDao().delete(it) }
        db.savingsGoalDao().delete(g)
    }

    suspend fun totalFor(type: TransactionType, start: java.time.Instant, end: java.time.Instant): Long = tx.total(type.name, start, end)

    suspend fun categoryTotals(type: TransactionType, start: java.time.Instant, end: java.time.Instant) = tx.categoryTotals(type.name, start, end)

    /**
     * Recurring transactions: the newest instance of a series carries the recurrence flag.
     * When its next date arrives, a new instance is posted and becomes the series head.
     */
    suspend fun processRecurring(): Int {
        val now = time.now()
        val zone = time.zone()
        var posted = 0
        for (head in tx.getRecurringHeads()) {
            var current = head
            var guard = 0
            while (guard++ < 400) {
                val next = RecurrenceMath.next(current.occurredAt, current.recurrence, zone) ?: break
                if (next.isAfter(now)) break
                db.withTransaction {
                    tx.update(current.copy(recurrence = Recurrence.NONE))
                    val newId = tx.insert(current.copy(id = 0, occurredAt = next, createdAt = now))
                    current = current.copy(id = newId, occurredAt = next)
                }
                posted++
            }
        }
        return posted
    }
}
