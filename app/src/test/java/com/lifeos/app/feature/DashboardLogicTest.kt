package com.lifeos.app.feature

import com.lifeos.app.data.repository.ProfileRepositoryImpl
import com.lifeos.app.domain.model.DashboardSnapshot
import com.lifeos.app.domain.model.FamilySummary
import com.lifeos.app.domain.model.FinanceSummary
import com.lifeos.app.domain.model.TaskSummary
import com.lifeos.app.domain.usecase.DashboardMath
import com.lifeos.app.feature.dashboard.overviewSentence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardLogicTest {

    private fun snapshot(due: Int = 0, overdue: Int = 0, spent: Long = 0) = DashboardSnapshot(
        displayName = null,
        widgets = DashboardMath.defaultWidgets(),
        schedule = emptyList(),
        upcomingEventsCount = 0,
        finance = FinanceSummary(0, spent, null, emptyList()),
        tasks = TaskSummary(due, 0, overdue, due + overdue),
        reminders = emptyList(),
        goals = emptyList(),
        family = FamilySummary(0, null, null, null),
        notes = emptyList(),
        documents = emptyList(),
        productivityScore = null,
    )

    @Test fun overviewIsHonestWhenEmpty() {
        assertTrue(overviewSentence(snapshot()).startsWith("Nothing is recorded"))
    }

    @Test fun overviewReportsRealCounts() {
        assertEquals(
            "2 tasks due today, 1 overdue, ₹1,250 spent this month.",
            overviewSentence(snapshot(due = 2, overdue = 1, spent = 1_250_00)),
        )
        assertEquals("1 task due today.", overviewSentence(snapshot(due = 1)))
    }

    @Test fun financeDerivedValues() {
        val f = FinanceSummary(incomePaise = 50_000_00, expensePaise = 12_000_00, budgetPaise = 20_000_00, lastSixMonthsExpense = emptyList())
        assertEquals(38_000_00L, f.netPaise)
        assertEquals(8_000_00L, f.remainingBudgetPaise)
        assertNull(f.copy(budgetPaise = null).remainingBudgetPaise)
    }

    @Test fun nameValidation() {
        assertEquals("Abhijit", ProfileRepositoryImpl.validateName("  Abhijit  "))
        assertEquals("Abhijit Das", ProfileRepositoryImpl.validateName("Abhijit   Das"))
        assertNull(ProfileRepositoryImpl.validateName("   "))
        assertNull(ProfileRepositoryImpl.validateName("x".repeat(41)))
        assertNull(ProfileRepositoryImpl.validateName("bad\u0000name"))
    }
}
