package com.lifeos.app.feature.finance

import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.dao.CategoryTotal
import com.lifeos.app.data.local.entity.AccountEntity
import com.lifeos.app.data.local.entity.BudgetEntity
import com.lifeos.app.data.local.entity.ReminderEntity
import com.lifeos.app.data.local.entity.SavingsGoalEntity
import com.lifeos.app.data.local.entity.TransactionEntity
import com.lifeos.app.data.repository.AccountWithBalance
import com.lifeos.app.data.repository.BudgetStatus
import com.lifeos.app.data.repository.FinanceRepository
import com.lifeos.app.data.repository.MonthFlow
import com.lifeos.app.data.repository.ReminderEngine
import com.lifeos.app.domain.model.AccountType
import com.lifeos.app.domain.model.Recurrence
import com.lifeos.app.domain.model.ReminderType
import com.lifeos.app.domain.model.TransactionCategory
import com.lifeos.app.feature.common.LifeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repo: FinanceRepository,
    private val reminders: ReminderEngine,
    private val time: TimeProvider,
) : LifeViewModel() {

    private val _month = MutableStateFlow(repo.currentMonth())
    val month: StateFlow<YearMonth> = _month

    val transactions: StateFlow<List<TransactionEntity>> = _month.flatMapLatest { repo.observeMonth(it) }.stateWhileSubscribed(emptyList())
    val categoryTotals: StateFlow<List<CategoryTotal>> = _month.flatMapLatest { repo.observeCategoryTotals(it) }.stateWhileSubscribed(emptyList())
    val sixMonths: StateFlow<List<MonthFlow>> = _month.flatMapLatest { repo.observeSixMonths(it) }.stateWhileSubscribed(emptyList())
    val budgets: StateFlow<List<BudgetStatus>> = _month.flatMapLatest { repo.observeBudgets(it) }.stateWhileSubscribed(emptyList())
    val accounts: StateFlow<List<AccountWithBalance>> = repo.observeAccounts().stateWhileSubscribed(emptyList())
    val savings: StateFlow<List<SavingsGoalEntity>> = repo.observeSavingsGoals().stateWhileSubscribed(emptyList())
    val isCurrentMonth: StateFlow<Boolean> = _month.map { it == repo.currentMonth() }.stateWhileSubscribed(true)

    init {
        act { repo.processRecurring() }
    }

    fun shiftMonth(delta: Long) {
        val next = _month.value.plusMonths(delta)
        if (!next.isAfter(repo.currentMonth())) _month.value = next
    }

    fun now() = time.now()
    fun zone() = time.zone()

    fun saveTransaction(t: TransactionEntity) = act("Saved.") { repo.saveTransaction(t) }
    fun deleteTransaction(t: TransactionEntity) = act("Transaction deleted.") { repo.deleteTransaction(t) }

    fun setBudget(category: TransactionCategory, paise: Long) = act("Budget saved.") { repo.setBudget(category, _month.value, paise) }
    fun deleteBudget(b: BudgetEntity) = act("Budget removed.") { repo.deleteBudget(b) }

    /** Saves the account; for loans with an EMI, optionally creates a monthly bill reminder. */
    fun saveAccount(a: AccountEntity, emiReminder: Boolean) = act("Account saved.") {
        repo.saveAccount(a)
        val emi = a.emiPaise
        val day = a.emiDayOfMonth
        if (emiReminder && a.type == AccountType.LOAN && emi != null && day != null) {
            val zone = time.zone()
            val today = time.now().atZone(zone).toLocalDate()
            var date = today.withDayOfMonth(day.coerceIn(1, today.lengthOfMonth()))
            if (!date.isAfter(today)) date = date.plusMonths(1).let { it.withDayOfMonth(day.coerceIn(1, it.lengthOfMonth())) }
            reminders.save(
                ReminderEntity(
                    title = "EMI due: ${a.name}",
                    message = "EMI of ${com.lifeos.app.core.format.IndianFormat.rupees(emi)} is due today.",
                    type = ReminderType.BILL,
                    triggerAt = date.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant(),
                    recurrence = Recurrence.MONTHLY,
                    createdAt = time.now(),
                ),
            )
        }
    }

    fun deleteAccount(a: AccountEntity) = act("Account deleted. Its transactions were kept.") { repo.deleteAccount(a) }

    fun saveSavings(g: SavingsGoalEntity) = act("Savings goal saved.") { repo.saveSavingsGoal(g) }
    fun addToSavings(g: SavingsGoalEntity, paise: Long) = act("Added to ${g.title}.") { repo.addToSavings(g, paise) }
    fun deleteSavings(g: SavingsGoalEntity) = act("Savings goal deleted.") { repo.deleteSavingsGoal(g) }
}
