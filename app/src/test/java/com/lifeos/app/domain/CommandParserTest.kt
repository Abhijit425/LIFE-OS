package com.lifeos.app.domain

import com.lifeos.app.domain.assistant.Command
import com.lifeos.app.domain.assistant.CommandParser
import com.lifeos.app.domain.assistant.Period
import com.lifeos.app.domain.model.TransactionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CommandParserTest {
    private val today = LocalDate.of(2026, 11, 25) // Wednesday

    private fun p(s: String) = CommandParser.parse(s, today)

    @Test fun spendingQueries() {
        assertEquals(Command.SpendingQuery(Period.THIS_MONTH), p("How much did I spend this month?"))
        assertEquals(Command.SpendingQuery(Period.TODAY), p("how much have I spent today"))
        assertEquals(Command.SpendingQuery(Period.LAST_MONTH), p("How much did I spend last month"))
        assertEquals(Command.ExpensesByCategory(Period.THIS_MONTH), p("Show my expenses by category."))
        assertEquals(Command.IncomeQuery(Period.THIS_MONTH), p("how much did I earn this month"))
    }

    @Test fun reminders() {
        assertEquals(
            Command.CreateReminder("Pay electricity bill", today.plusDays(1), null),
            p("Remind me to pay electricity bill tomorrow."),
        )
        assertEquals(
            Command.CreateReminder("Call Ma", today, LocalTime.of(18, 30)),
            p("remind me to call Ma at 6:30 pm"),
        )
        assertEquals(
            Command.CreateReminder("Renew insurance", LocalDate.of(2026, 12, 5), LocalTime.of(10, 0)),
            p("Remind me to renew insurance on 5 Dec at 10am"),
        )
        val fri = p("remind me about the meeting on friday at 5") as Command.CreateReminder
        assertEquals(LocalDate.of(2026, 11, 27), fri.date)
        assertEquals(LocalTime.of(17, 0), fri.time)
        assertEquals("The meeting", fri.title)
    }

    @Test fun tasks() {
        assertEquals(Command.ShowPendingTasks, p("Show my pending tasks."))
        assertEquals(Command.AddTask("Complete my project", null, null), p("Add a task to complete my project."))
        assertEquals(Command.AddTask("Submit report", today.plusDays(2), null), p("create task submit report day after tomorrow"))
        assertEquals(Command.CompleteTask("submit report"), p("mark submit report as done"))
        assertEquals(Command.DeleteTask("old stuff"), p("delete task old stuff"))
    }

    @Test fun moneyActions() {
        assertEquals(Command.CreateSavingsGoal(50_000_00, "Savings goal"), p("Create a savings goal of ₹50,000."))
        assertEquals(Command.CreateSavingsGoal(1_00_000_00, "New laptop"), p("create savings goal of 1 lakh for new laptop"))
        val e = p("I spent 450 on lunch") as Command.AddExpense
        assertEquals(450_00L, e.amountPaise)
        assertEquals(TransactionCategory.FOOD, e.category)
        assertEquals("Lunch", e.note)
        val e2 = p("add expense 1,200 for petrol") as Command.AddExpense
        assertEquals(TransactionCategory.TRANSPORT, e2.category)
        val i = p("received 52k salary") as Command.AddIncome
        assertEquals(52_000_00L, i.amountPaise)
    }

    @Test fun otherQueries() {
        assertEquals(Command.AppointmentsQuery(today.plusDays(1)), p("What appointments do I have tomorrow?"))
        assertEquals(Command.SummarizeWeek, p("Summarize my week."))
        assertEquals(Command.AddNote("Buy Diwali gifts for kids"), p("note: Buy Diwali gifts for kids"))
        assertEquals(Command.Help, p("help"))
        assertTrue(p("sing me a song") is Command.Unknown)
    }
}
