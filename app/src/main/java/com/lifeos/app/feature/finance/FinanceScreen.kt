package com.lifeos.app.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.designsystem.component.CardHeader
import com.lifeos.app.core.designsystem.component.ChoiceChips
import com.lifeos.app.core.designsystem.component.DateField
import com.lifeos.app.core.designsystem.component.DonutChart
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.FieldLabel
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.GlowProgressBar
import com.lifeos.app.core.designsystem.component.GroupedBarChart
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeSheet
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.ListRow
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.component.SearchField
import com.lifeos.app.core.designsystem.component.SegmentedTabs
import com.lifeos.app.core.designsystem.component.Slice
import com.lifeos.app.core.designsystem.component.StatBlock
import com.lifeos.app.core.designsystem.component.SwitchRow
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.designsystem.theme.WidthClass
import com.lifeos.app.core.format.IndianFormat
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.data.local.entity.AccountEntity
import com.lifeos.app.data.local.entity.SavingsGoalEntity
import com.lifeos.app.data.local.entity.TransactionEntity
import com.lifeos.app.domain.logic.MoneyParser
import com.lifeos.app.domain.model.AccountType
import com.lifeos.app.domain.model.Recurrence
import com.lifeos.app.domain.model.TransactionCategory
import com.lifeos.app.domain.model.TransactionType
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.navigation.screenContentPadding
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class FinanceTab(val label: String) { OVERVIEW("Overview"), TRANSACTIONS("Transactions"), BUDGETS("Budgets"), ACCOUNTS("Accounts"), SAVINGS("Savings") }

/** Wrapper so "new" (item = null) and "edit" share one sheet. */
private data class Edit<T>(val item: T?)

@Composable
fun FinanceScreen(vm: FinanceViewModel = hiltViewModel()) {
    val month by vm.month.collectAsStateWithLifecycle()
    val isCurrent by vm.isCurrentMonth.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val totals by vm.categoryTotals.collectAsStateWithLifecycle()
    val six by vm.sixMonths.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val savings by vm.savings.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(FinanceTab.OVERVIEW) }
    var txEdit by remember { mutableStateOf<Edit<TransactionEntity>?>(null) }
    var budgetOpen by remember { mutableStateOf(false) }
    var accountEdit by remember { mutableStateOf<Edit<AccountEntity>?>(null) }
    var savingsEdit by remember { mutableStateOf<Edit<SavingsGoalEntity>?>(null) }
    var addMoneyTo by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf<TransactionType?>(null) }
    val colors = LifeTheme.colors
    val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))

    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
    val budgetTotal = budgets.sumOf { it.budget.limitPaise }

    MessageBox(vm) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = screenContentPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            item {
                ScreenHeader("Finance", "Manual tracker in ₹ — LIFE OS never connects to your bank.") {
                    LifePrimaryButton("Add", onClick = { txEdit = Edit(null) }, icon = Icons.Outlined.Add)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.shiftMonth(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous month", tint = colors.textPrimary) }
                    Text(monthLabel, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    IconButton(onClick = { vm.shiftMonth(1) }, enabled = !isCurrent) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next month", tint = if (isCurrent) colors.textTertiary else colors.textPrimary) }
                }
                Spacer(Modifier.height(Spacing.xs))
                SegmentedTabs(FinanceTab.entries, tab, { it.label }, { tab = it })
            }
            when (tab) {
                FinanceTab.OVERVIEW -> {
                    item {
                        GlassCard {
                            val compact = LifeTheme.layout.widthClass == WidthClass.Compact
                            val stats = listOf(
                                Triple("Income", IndianFormat.rupeesCompact(income), colors.success),
                                Triple("Expenses", IndianFormat.rupeesCompact(expense), colors.textPrimary),
                                Triple(if (budgetTotal > 0) "Budget left" else "Net", IndianFormat.rupeesCompact(if (budgetTotal > 0) budgetTotal - expense else income - expense),
                                    if ((if (budgetTotal > 0) budgetTotal - expense else income - expense) < 0) colors.danger else colors.textPrimary),
                                Triple("Saved (goals)", IndianFormat.rupeesCompact(savings.sumOf { it.savedPaise }), colors.accentCyan),
                            )
                            stats.chunked(if (compact) 2 else 4).forEach { row ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    row.forEach { (l, v, c) -> StatBlock(l, v, Modifier.weight(1f), c) }
                                }
                            }
                        }
                    }
                    item {
                        GlassCard {
                            CardHeader(Icons.Outlined.ArrowDownward, "Spending by category")
                            Spacer(Modifier.height(Spacing.md))
                            if (totals.isEmpty()) {
                                EmptyState(Icons.Outlined.ArrowDownward, "No expenses in $monthLabel", "Tap Add to record your first expense.")
                            } else {
                                DonutChart(
                                    slices = totals.map { Slice(it.category.label, it.totalPaise.toFloat(), IndianFormat.rupeesCompact(it.totalPaise)) },
                                    centerLabel = "spent", centerValue = IndianFormat.rupeesCompact(expense),
                                )
                            }
                        }
                    }
                    item {
                        GlassCard {
                            CardHeader(Icons.Outlined.ArrowUpward, "Income vs expenses · 6 months")
                            Spacer(Modifier.height(Spacing.md))
                            GroupedBarChart(
                                labels = six.map { TimeFormat.monthShort(it.month) },
                                a = six.map { it.incomePaise.toFloat() },
                                b = six.map { it.expensePaise.toFloat() },
                                aName = "Income", bName = "Expenses",
                                accessibilitySummary = six.joinToString { "${TimeFormat.monthShort(it.month)}: income ${IndianFormat.rupees(it.incomePaise, false)}, expenses ${IndianFormat.rupees(it.expensePaise, false)}" },
                            )
                        }
                    }
                    if (savings.isNotEmpty()) item {
                        GlassCard {
                            CardHeader(Icons.Outlined.Savings, "Savings progress")
                            savings.forEach { g -> SavingsRow(g) { savingsEdit = Edit(g) } }
                        }
                    }
                }
                FinanceTab.TRANSACTIONS -> {
                    item {
                        SearchField(query, { query = it }, "Search notes or categories")
                        Spacer(Modifier.height(Spacing.xs))
                        ChoiceChips(listOf<TransactionType?>(null, TransactionType.EXPENSE, TransactionType.INCOME), typeFilter,
                            { it?.name?.lowercase()?.replaceFirstChar { c -> c.uppercase() } ?: "All" }, { typeFilter = it })
                    }
                    val filtered = transactions.filter { t ->
                        (typeFilter == null || t.type == typeFilter) &&
                            (query.isBlank() || t.note.orEmpty().contains(query, true) || t.category.label.contains(query, true))
                    }
                    if (filtered.isEmpty()) item {
                        GlassCard { EmptyState(Icons.Outlined.ArrowDownward, "No transactions", "Nothing matches in $monthLabel.") }
                    }
                    val zone = vm.zone()
                    filtered.groupBy { it.occurredAt.atZone(zone).toLocalDate() }.forEach { (date, list) ->
                        item(key = "d$date") {
                            GlassCard {
                                Text(TimeFormat.shortDate(date), style = MaterialTheme.typography.labelLarge, color = colors.accentCyan)
                                list.forEach { t ->
                                    ListRow(
                                        title = t.note?.takeIf { it.isNotBlank() } ?: t.category.label,
                                        subtitle = t.category.label + (if (t.recurrence != Recurrence.NONE) " · repeats ${t.recurrence.name.lowercase()}" else ""),
                                        trailing = {
                                            Text(
                                                (if (t.type == TransactionType.INCOME) "+" else "−") + IndianFormat.rupees(t.amountPaise),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (t.type == TransactionType.INCOME) colors.success else colors.textPrimary,
                                            )
                                        },
                                        onClick = { txEdit = Edit(t) },
                                    )
                                }
                            }
                        }
                    }
                }
                FinanceTab.BUDGETS -> {
                    item {
                        GlassCard {
                            CardHeader(Icons.Outlined.AccountBalance, "Budgets for $monthLabel") {
                                LifePrimaryButton("Set", onClick = { budgetOpen = true })
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            if (budgets.isEmpty()) {
                                EmptyState(Icons.Outlined.AccountBalance, "No budgets yet", "Set a monthly limit per category to track remaining budget.")
                            } else {
                                budgets.forEach { b ->
                                    Column(Modifier.padding(vertical = Spacing.xs)) {
                                        Row {
                                            Text(b.budget.category.label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                            Text("${IndianFormat.rupeesCompact(b.spentPaise)} / ${IndianFormat.rupeesCompact(b.budget.limitPaise)}", style = MaterialTheme.typography.labelLarge,
                                                color = if (b.progress > 1f) colors.danger else colors.textSecondary)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        GlowProgressBar(b.progress, color = if (b.progress > 1f) colors.danger else null)
                                        Row {
                                            Spacer(Modifier.weight(1f))
                                            androidx.compose.material3.TextButton(onClick = { vm.deleteBudget(b.budget) }) { Text("Remove", color = colors.textTertiary) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                FinanceTab.ACCOUNTS -> {
                    item {
                        GlassCard {
                            CardHeader(Icons.Outlined.AccountBalance, "Accounts, cards & loans") {
                                LifePrimaryButton("Add", onClick = { accountEdit = Edit(null) })
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            if (accounts.isEmpty()) {
                                EmptyState(Icons.Outlined.AccountBalance, "No accounts", "Add cash, bank, credit-card or loan accounts. Balances come from what you record.")
                            }
                            accounts.forEach { a ->
                                val owed = a.account.type == AccountType.CREDIT_CARD || a.account.type == AccountType.LOAN
                                ListRow(
                                    title = a.account.name,
                                    subtitle = a.account.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } +
                                        (a.account.emiPaise?.let { " · EMI ${IndianFormat.rupees(it, false)}" + (a.account.emiDayOfMonth?.let { d -> " on day $d" } ?: "") } ?: "") +
                                        (a.account.creditLimitPaise?.let { " · limit ${IndianFormat.rupeesCompact(it)}" } ?: ""),
                                    trailing = {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(IndianFormat.rupees(a.balancePaise), style = MaterialTheme.typography.titleSmall, color = if (owed) colors.warning else colors.textPrimary)
                                            Text(if (owed) "owed" else "balance", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
                                        }
                                    },
                                    onClick = { accountEdit = Edit(a.account) },
                                )
                            }
                        }
                    }
                }
                FinanceTab.SAVINGS -> {
                    item {
                        GlassCard {
                            CardHeader(Icons.Outlined.Savings, "Savings goals") {
                                LifePrimaryButton("New", onClick = { savingsEdit = Edit(null) })
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            if (savings.isEmpty()) EmptyState(Icons.Outlined.Savings, "No savings goals", "Example: save ₹1,00,000 for an emergency fund.")
                            savings.forEach { g ->
                                SavingsRow(g) { savingsEdit = Edit(g) }
                                Row {
                                    Spacer(Modifier.weight(1f))
                                    androidx.compose.material3.TextButton(onClick = { addMoneyTo = g }) { Text("Add money", color = colors.accentCyan) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    txEdit?.let { e -> TransactionSheet(e.item, accounts.map { it.account }, vm, onDismiss = { txEdit = null }) }
    if (budgetOpen) BudgetSheet(onDismiss = { budgetOpen = false }, onSave = { c, p -> vm.setBudget(c, p); budgetOpen = false })
    accountEdit?.let { e -> AccountSheet(e.item, vm, onDismiss = { accountEdit = null }) }
    savingsEdit?.let { e -> SavingsSheet(e.item, vm, onDismiss = { savingsEdit = null }) }
    addMoneyTo?.let { g -> AddMoneySheet(g, onDismiss = { addMoneyTo = null }, onSave = { p -> vm.addToSavings(g, p); addMoneyTo = null }) }
}

@Composable
private fun SavingsRow(g: SavingsGoalEntity, onClick: () -> Unit) {
    val colors = LifeTheme.colors
    val progress = if (g.targetPaise > 0) g.savedPaise.toFloat() / g.targetPaise else 0f
    Column(Modifier.padding(vertical = Spacing.xs)) {
        ListRow(
            title = g.title,
            subtitle = "${IndianFormat.rupees(g.savedPaise, false)} of ${IndianFormat.rupees(g.targetPaise, false)}" + (g.targetDate?.let { " · by ${TimeFormat.shortDate(it)} ${it.year}" } ?: ""),
            trailing = { Text("${Math.round(progress.coerceAtMost(1f) * 100)}%", style = MaterialTheme.typography.labelLarge, color = colors.accentCyan) },
            onClick = onClick,
        )
        GlowProgressBar(progress)
    }
}

@Composable
private fun TransactionSheet(item: TransactionEntity?, accounts: List<AccountEntity>, vm: FinanceViewModel, onDismiss: () -> Unit) {
    val zone = vm.zone()
    var type by remember { mutableStateOf(item?.type ?: TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf(item?.let { MoneyParser.toFieldText(it.amountPaise) } ?: "") }
    var category by remember { mutableStateOf(item?.category ?: TransactionCategory.FOOD) }
    var date by remember { mutableStateOf(item?.occurredAt?.atZone(zone)?.toLocalDate() ?: vm.now().atZone(zone).toLocalDate()) }
    var note by remember { mutableStateOf(item?.note.orEmpty()) }
    var accountId by remember { mutableStateOf(item?.accountId) }
    var recurrence by remember { mutableStateOf(item?.recurrence ?: Recurrence.NONE) }
    val paise = MoneyParser.parseField(amount)

    LifeSheet(
        title = if (item == null) "Add transaction" else "Edit transaction",
        onDismiss = onDismiss,
        saveEnabled = paise != null,
        onSave = {
            val time = item?.occurredAt?.atZone(zone)?.toLocalTime() ?: if (date == vm.now().atZone(zone).toLocalDate()) vm.now().atZone(zone).toLocalTime() else LocalTime.NOON
            vm.saveTransaction(
                (item ?: TransactionEntity(type = type, amountPaise = 1, category = category, occurredAt = vm.now(), createdAt = vm.now())).copy(
                    type = type, amountPaise = paise ?: 0, category = category, note = note.trim().ifBlank { null },
                    occurredAt = date.atTime(time).atZone(zone).toInstant(), accountId = accountId, recurrence = recurrence,
                ),
            )
            onDismiss()
        },
        onDelete = item?.let { { vm.deleteTransaction(it); onDismiss() } },
    ) {
        ChoiceChips(listOf(TransactionType.EXPENSE, TransactionType.INCOME), type, { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }, { type = it })
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(amount, { amount = it }, "Amount (₹)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal,
            isError = amount.isNotBlank() && paise == null, supportingText = if (amount.isNotBlank() && paise == null) "Enter an amount like 1250 or 1,250.50" else null)
        FieldLabel("Category")
        ChoiceChips(TransactionCategory.entries, category, { it.label }, { category = it })
        DateField("Date", date, { d -> if (d != null) date = d }, allowClear = false)
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(note, { note = it.take(120) }, "Note (optional)", Modifier.fillMaxWidth())
        if (accounts.isNotEmpty()) {
            FieldLabel("Account")
            ChoiceChips(listOf<AccountEntity?>(null) + accounts, accounts.firstOrNull { it.id == accountId }, { it?.name ?: "None" }, { accountId = it?.id })
        }
        FieldLabel("Repeats")
        ChoiceChips(listOf(Recurrence.NONE, Recurrence.WEEKLY, Recurrence.MONTHLY, Recurrence.YEARLY), recurrence,
            { if (it == Recurrence.NONE) "Never" else it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }, { recurrence = it })
    }
}

@Composable
private fun BudgetSheet(onDismiss: () -> Unit, onSave: (TransactionCategory, Long) -> Unit) {
    var category by remember { mutableStateOf(TransactionCategory.FOOD) }
    var amount by remember { mutableStateOf("") }
    val paise = MoneyParser.parseField(amount)
    LifeSheet("Set monthly budget", onDismiss, onSave = { paise?.let { onSave(category, it) } }, saveEnabled = paise != null) {
        FieldLabel("Category")
        ChoiceChips(TransactionCategory.entries.filter { it != TransactionCategory.SALARY }, category, { it.label }, { category = it })
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(amount, { amount = it }, "Monthly limit (₹)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal)
        Text("Setting a budget again for the same category replaces it.", style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textTertiary)
    }
}

@Composable
private fun AccountSheet(item: AccountEntity?, vm: FinanceViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var type by remember { mutableStateOf(item?.type ?: AccountType.BANK) }
    var opening by remember { mutableStateOf(item?.openingBalancePaise?.let { MoneyParser.toFieldText(it) } ?: "") }
    var limit by remember { mutableStateOf(item?.creditLimitPaise?.let { MoneyParser.toFieldText(it) } ?: "") }
    var emi by remember { mutableStateOf(item?.emiPaise?.let { MoneyParser.toFieldText(it) } ?: "") }
    var emiDay by remember { mutableStateOf(item?.emiDayOfMonth?.toString() ?: "") }
    var emiReminder by remember { mutableStateOf(item == null) }
    val openingPaise = if (opening.isBlank()) 0L else MoneyParser.parseField(opening)
    val dayValid = emiDay.isBlank() || emiDay.toIntOrNull() in 1..31
    LifeSheet(
        if (item == null) "Add account" else "Edit account", onDismiss,
        saveEnabled = name.isNotBlank() && openingPaise != null && dayValid,
        onSave = {
            vm.saveAccount(
                (item ?: AccountEntity(name = name, type = type, createdAt = vm.now())).copy(
                    name = name.trim(), type = type, openingBalancePaise = openingPaise ?: 0,
                    creditLimitPaise = if (type == AccountType.CREDIT_CARD) MoneyParser.parseField(limit) else null,
                    emiPaise = if (type == AccountType.LOAN) MoneyParser.parseField(emi) else null,
                    emiDayOfMonth = if (type == AccountType.LOAN) emiDay.toIntOrNull() else null,
                ),
                emiReminder,
            )
            onDismiss()
        },
        onDelete = item?.let { { vm.deleteAccount(it); onDismiss() } },
        deleteMessage = "The account will be removed. Transactions linked to it are kept.",
    ) {
        LifeTextField(name, { name = it.take(40) }, "Name (e.g. SBI Savings)", Modifier.fillMaxWidth())
        FieldLabel("Type")
        ChoiceChips(AccountType.entries, type, { it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() } }, { type = it })
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(opening, { opening = it }, if (type == AccountType.LOAN || type == AccountType.CREDIT_CARD) "Amount currently owed (₹)" else "Opening balance (₹)",
            Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal)
        if (type == AccountType.CREDIT_CARD) {
            Spacer(Modifier.height(Spacing.sm))
            LifeTextField(limit, { limit = it }, "Credit limit (₹, optional)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal)
        }
        if (type == AccountType.LOAN) {
            Spacer(Modifier.height(Spacing.sm))
            LifeTextField(emi, { emi = it }, "EMI amount (₹)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(Spacing.sm))
            LifeTextField(emiDay, { emiDay = it.filter(Char::isDigit).take(2) }, "EMI day of month (1–31)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number,
                isError = !dayValid)
            SwitchRow("Monthly EMI reminder", emiReminder, { emiReminder = it }, "Creates a repeating bill reminder at 9 AM on that day")
        }
        Text("Record payments and spending as transactions linked to this account to keep its balance current.",
            style = MaterialTheme.typography.bodySmall, color = LifeTheme.colors.textTertiary)
    }
}

@Composable
private fun SavingsSheet(item: SavingsGoalEntity?, vm: FinanceViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(item?.title.orEmpty()) }
    var target by remember { mutableStateOf(item?.let { MoneyParser.toFieldText(it.targetPaise) } ?: "") }
    var saved by remember { mutableStateOf(item?.let { MoneyParser.toFieldText(it.savedPaise) } ?: "") }
    var date by remember { mutableStateOf(item?.targetDate) }
    val targetPaise = MoneyParser.parseField(target)
    val savedPaise = if (saved.isBlank()) 0L else MoneyParser.parseField(saved)
    LifeSheet(
        if (item == null) "New savings goal" else "Edit savings goal", onDismiss,
        saveEnabled = title.isNotBlank() && targetPaise != null && savedPaise != null,
        onSave = {
            vm.saveSavings((item ?: SavingsGoalEntity(title = title, targetPaise = 1, createdAt = vm.now())).copy(
                title = title.trim(), targetPaise = targetPaise ?: 1, savedPaise = savedPaise ?: 0, targetDate = date))
            onDismiss()
        },
        onDelete = item?.let { { vm.deleteSavings(it); onDismiss() } },
    ) {
        LifeTextField(title, { title = it.take(50) }, "Goal (e.g. Emergency fund)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(target, { target = it }, "Target (₹)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(saved, { saved = it }, "Saved so far (₹)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal)
        DateField("Target date", date, { date = it })
    }
}

@Composable
private fun AddMoneySheet(goal: SavingsGoalEntity, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    val paise = MoneyParser.parseField(amount)
    LifeSheet("Add to ${goal.title}", onDismiss, onSave = { paise?.let(onSave) }, saveEnabled = paise != null, saveLabel = "Add") {
        LifeTextField(amount, { amount = it }, "Amount (₹)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal)
    }
}
