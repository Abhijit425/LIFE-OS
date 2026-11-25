package com.lifeos.app.domain.assistant

import com.lifeos.app.domain.logic.MoneyParser
import com.lifeos.app.domain.model.TransactionCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.temporal.TemporalAdjusters

enum class Period(val label: String) { TODAY("today"), THIS_WEEK("this week"), THIS_MONTH("this month"), LAST_MONTH("last month") }

/** Structured intents understood by LIFE AI's offline command engine (rule-based, not generative). */
sealed interface Command {
    data class SpendingQuery(val period: Period) : Command
    data class IncomeQuery(val period: Period) : Command
    data class ExpensesByCategory(val period: Period) : Command
    data class AddExpense(val amountPaise: Long, val category: TransactionCategory, val note: String?) : Command
    data class AddIncome(val amountPaise: Long, val note: String?) : Command
    data class CreateReminder(val title: String, val date: LocalDate, val time: LocalTime?) : Command
    data object ShowPendingTasks : Command
    data class AddTask(val title: String, val due: LocalDate?, val time: LocalTime?) : Command
    data class CompleteTask(val query: String) : Command
    data class DeleteTask(val query: String) : Command
    data class CreateSavingsGoal(val amountPaise: Long, val title: String) : Command
    data class AppointmentsQuery(val date: LocalDate) : Command
    data class ScheduleQuery(val date: LocalDate) : Command
    data object SummarizeWeek : Command
    data class AddNote(val text: String) : Command
    data object BudgetStatus : Command
    data object Help : Command
    data class Unknown(val text: String) : Command
}

data class DateTimeExtraction(val date: LocalDate?, val time: LocalTime?, val rest: String)

object CommandParser {

    fun parse(input: String, today: LocalDate): Command {
        val raw = input.trim().trimEnd('?', '.', '!')
        val t = raw.lowercase().replace(Regex("\\s+"), " ")
        if (t.isEmpty()) return Command.Unknown(input)

        if (Regex("""^(help|what can you do|commands|how do i use.*)$""").matches(t)) return Command.Help

        // ---- Queries
        if (Regex("""(expenses?|spending|spent).*\bby categor""").containsMatchIn(t) || t.contains("category breakdown")) {
            return Command.ExpensesByCategory(periodOf(t) ?: Period.THIS_MONTH)
        }
        if (Regex("""^(how much|what).*(did i|have i|i)\s*(spend|spent)|^(my )?(total )?(spending|expenses)( (today|this week|this month|last month))?$""").containsMatchIn(t)) {
            return Command.SpendingQuery(periodOf(t) ?: Period.THIS_MONTH)
        }
        if (Regex("""^(how much).*(earn|earned|income|receive|received)|^(my )?income( (today|this week|this month|last month))?$""").containsMatchIn(t)) {
            return Command.IncomeQuery(periodOf(t) ?: Period.THIS_MONTH)
        }
        if (Regex("""budget (status|left|remaining)|how('?s| is) my budget|remaining budget""").containsMatchIn(t)) return Command.BudgetStatus
        if (Regex("""(show|list|what are|what's|whats)?.*\b(pending|open|incomplete|remaining) tasks|^my tasks$|^tasks$""").containsMatchIn(t)) {
            return Command.ShowPendingTasks
        }
        if (Regex("""summari[sz]e (my )?week|weekly (summary|report)|how was my week""").containsMatchIn(t)) return Command.SummarizeWeek
        if (t.contains("appointment")) {
            val dt = extractDateTime(t, today)
            if (Regex("""^(what|any|show|list|do i have)""").containsMatchIn(t) || t.endsWith("appointments")) {
                return Command.AppointmentsQuery(dt.date ?: today)
            }
        }
        if (Regex("""^(what('?s| is)|show)( on)? (my )?(schedule|calendar|agenda)|what do i have (today|tomorrow|on)""").containsMatchIn(t)) {
            return Command.ScheduleQuery(extractDateTime(t, today).date ?: today)
        }

        // ---- Actions
        Regex("""^remind me (to |about |that )?(.+)$""").find(t)?.let { m ->
            val dt = extractDateTime(rawTail(raw, m.groups[2]!!.range.first), today)
            val title = cleanTitle(dt.rest)
            if (title.isNotEmpty()) return Command.CreateReminder(title.cap(), dt.date ?: today, dt.time)
        }
        Regex("""^(add|create|new|make)( a| an)?( new)? (task|todo|to-do)( to| for| called|:)?\s*(.+)$""").find(t)?.let { m ->
            val dt = extractDateTime(rawTail(raw, m.groups[6]!!.range.first), today)
            val title = cleanTitle(dt.rest)
            if (title.isNotEmpty()) return Command.AddTask(title.cap(), dt.date, dt.time)
        }
        Regex("""^(complete|finish|mark|done with|tick off)( the)?( task)? (.+?)( as (done|complete|completed))?$""").find(t)?.let { m ->
            val q = m.groupValues[4].trim()
            if (q.isNotEmpty()) return Command.CompleteTask(q)
        }
        Regex("""^(delete|remove)( the)? task (.+)$""").find(t)?.let { m ->
            return Command.DeleteTask(m.groupValues[3].trim())
        }
        Regex("""^(create|add|set|start|new)( a| an)?( new)? sav(ings|ing) goal( of| for)? (.+)$""").find(t)?.let { m ->
            val rest = m.groupValues[6]
            val amount = MoneyParser.parsePaise(rest)
            if (amount != null) {
                val title = Regex("""\b(for|to|called|named)\s+(.+)$""").find(rest)?.groupValues?.get(2)?.trim()
                return Command.CreateSavingsGoal(amount, title?.takeIf { it.isNotEmpty() }?.cap() ?: "Savings goal")
            }
        }
        Regex("""^(add|log|record)( an?)? expense (of )?(.+)$|^(i )?(spent|paid) (.+)$""").find(t)?.let { m ->
            val body = m.groups[4]?.value ?: m.groups[7]?.value ?: ""
            val amount = MoneyParser.parsePaise(body)
            if (amount != null) {
                val note = Regex("""\b(on|for|at|to)\s+(.+)$""").find(body)?.groupValues?.get(2)?.trim()
                return Command.AddExpense(amount, categorize(body), note?.cap())
            }
        }
        Regex("""^(add|log|record)( an?)? income (of )?(.+)$|^(i )?(received|got|earned) (.+)$""").find(t)?.let { m ->
            val body = m.groups[4]?.value ?: m.groups[7]?.value ?: ""
            val amount = MoneyParser.parsePaise(body)
            if (amount != null) {
                val note = Regex("""\b(from|for|as)\s+(.+)$""").find(body)?.groupValues?.get(2)?.trim()
                return Command.AddIncome(amount, note?.cap())
            }
        }
        Regex("""^(note|add (a )?note|take (a )?note|write (a )?note)[:\s]+(.+)$""").find(t)?.let { m ->
            val start = m.groups[5]!!.range.first
            return Command.AddNote(raw.substring(start).trim())
        }
        return Command.Unknown(input)
    }

    /** Maps the original-case tail of the input (keeps user capitalisation in titles). */
    private fun rawTail(raw: String, lowerIndex: Int): String {
        // Lower-casing and whitespace collapsing can shift indices; fall back to lower-case text if so.
        val collapsed = raw.replace(Regex("\\s+"), " ")
        return if (lowerIndex <= collapsed.length) collapsed.substring(lowerIndex) else collapsed
    }

    private fun cleanTitle(s: String): String =
        s.trim().trim(',', '.', ';', ':').replace(Regex("\\s+"), " ").removeSuffix(" by").removeSuffix(" on").removeSuffix(" at").trim()

    private fun String.cap(): String = replaceFirstChar { it.uppercase() }

    fun periodOf(t: String): Period? = when {
        t.contains("last month") -> Period.LAST_MONTH
        t.contains("this week") || t.contains("past week") || t.contains("last 7 days") -> Period.THIS_WEEK
        Regex("""\btoday\b""").containsMatchIn(t) -> Period.TODAY
        t.contains("this month") || t.contains("month") -> Period.THIS_MONTH
        else -> null
    }

    private val categoryWords: List<Pair<TransactionCategory, List<String>>> = listOf(
        TransactionCategory.FOOD to listOf("food", "lunch", "dinner", "breakfast", "snack", "grocer", "restaurant", "swiggy", "zomato", "tea", "coffee", "vegetable", "milk"),
        TransactionCategory.TRANSPORT to listOf("transport", "petrol", "diesel", "fuel", "uber", "ola", "auto", "taxi", "bus", "metro", "parking", "toll"),
        TransactionCategory.TRAVEL to listOf("travel", "flight", "train", "hotel", "trip", "holiday", "ticket"),
        TransactionCategory.BILLS to listOf("bill", "electricity", "water bill", "gas", "rent", "recharge", "internet", "wifi", "broadband", "emi", "insurance", "phone"),
        TransactionCategory.MEDICAL to listOf("medical", "medicine", "doctor", "hospital", "pharmacy", "clinic", "test", "health"),
        TransactionCategory.EDUCATION to listOf("education", "school", "fees", "tuition", "course", "book", "college"),
        TransactionCategory.SHOPPING to listOf("shopping", "clothes", "amazon", "flipkart", "shoes", "gift", "gadget"),
        TransactionCategory.ENTERTAINMENT to listOf("movie", "netflix", "entertainment", "game", "concert", "subscription", "spotify"),
        TransactionCategory.FAMILY to listOf("family", "kids", "child", "baby", "parents", "mother", "father"),
    )

    fun categorize(text: String): TransactionCategory {
        val t = text.lowercase()
        TransactionCategory.entries.firstOrNull { Regex("""\b${it.label.lowercase()}\b""").containsMatchIn(t) }?.let { return it }
        return categoryWords.firstOrNull { (_, words) -> words.any { t.contains(it) } }?.first ?: TransactionCategory.OTHER
    }

    private val months = mapOf(
        "jan" to Month.JANUARY, "feb" to Month.FEBRUARY, "mar" to Month.MARCH, "apr" to Month.APRIL,
        "may" to Month.MAY, "jun" to Month.JUNE, "jul" to Month.JULY, "aug" to Month.AUGUST,
        "sep" to Month.SEPTEMBER, "oct" to Month.OCTOBER, "nov" to Month.NOVEMBER, "dec" to Month.DECEMBER,
    )
    private const val MONTH_RX = "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"
    private const val DAY_RX = "(monday|tuesday|wednesday|thursday|friday|saturday|sunday)"

    /**
     * Pulls a date and/or time out of free text and returns the text with those phrases removed.
     * Dates without a year roll to next year if already past.
     */
    fun extractDateTime(text: String, today: LocalDate): DateTimeExtraction {
        var s = " $text "
        var date: LocalDate? = null
        var time: LocalTime? = null

        fun cut(rx: Regex, onMatch: (MatchResult) -> Unit) {
            val m = rx.find(s) ?: return
            onMatch(m)
            s = s.removeRange(m.range).let { " " + it.trim() + " " }
        }

        val ci = RegexOption.IGNORE_CASE
        // Time first so "5 pm" isn't mistaken for a date.
        cut(Regex("""\b(at|by|@)?\s*(\d{1,2})(?::|\.)(\d{2})\s*(am|pm|a\.m\.|p\.m\.)?\b""", ci)) { m ->
            var h = m.groupValues[2].toInt()
            val min = m.groupValues[3].toInt()
            val ap = m.groupValues[4].lowercase()
            if (ap.startsWith("p") && h < 12) h += 12
            if (ap.startsWith("a") && h == 12) h = 0
            if (h in 0..23 && min in 0..59) time = LocalTime.of(h, min)
        }
        if (time == null) {
            cut(Regex("""\b(at|by|@)?\s*(\d{1,2})\s*(am|pm|a\.m\.|p\.m\.)(?=\W)""", ci)) { m ->
                var h = m.groupValues[2].toInt()
                val ap = m.groupValues[3].lowercase()
                if (ap.startsWith("p") && h < 12) h += 12
                if (ap.startsWith("a") && h == 12) h = 0
                if (h in 0..23) time = LocalTime.of(h, 0)
            }
        }
        if (time == null) {
            cut(Regex("""\b(at|by)\s+(\d{1,2})(?=\W)(?!\s*(?:$MONTH_RX|/|-))""", ci)) { m ->
                val h = m.groupValues[2].toInt()
                // "at 5" → 5 PM if 1–7 (people rarely mean 5 AM), otherwise as written.
                if (h in 0..23) time = LocalTime.of(if (h in 1..7) h + 12 else h, 0)
            }
        }
        if (time == null) cut(Regex("""\b(in the |this )?morning\b""", ci)) { time = LocalTime.of(9, 0) }
        if (time == null) cut(Regex("""\b(in the |this )?afternoon\b""", ci)) { time = LocalTime.of(14, 0) }
        if (time == null) cut(Regex("""\b(in the |this )?evening\b""", ci)) { time = LocalTime.of(18, 0) }
        if (time == null) cut(Regex("""\btonight\b""", ci)) { time = LocalTime.of(20, 0); date = today }
        if (time == null) cut(Regex("""\bnoon\b""", ci)) { time = LocalTime.NOON }

        cut(Regex("""\b(on |by )?(the )?day after tomorrow\b""", ci)) { date = today.plusDays(2) }
        if (date == null) cut(Regex("""\b(on |by )?tomorrow\b""", ci)) { date = today.plusDays(1) }
        if (date == null) cut(Regex("""\b(on |by )?today\b""", ci)) { date = today }
        if (date == null) cut(Regex("""\bin (\d{1,3}) days?\b""", ci)) { m -> date = today.plusDays(m.groupValues[1].toLong()) }
        if (date == null) cut(Regex("""\bnext week\b""", ci)) { date = today.plusWeeks(1).with(DayOfWeek.MONDAY) }
        if (date == null) cut(Regex("""\b(on |by )?(next |this |coming )?$DAY_RX\b""", ci)) { m ->
            val dow = DayOfWeek.valueOf(m.groupValues[3].uppercase())
            val base = if (m.groupValues[2].trim() == "next") today.with(TemporalAdjusters.next(dow))
            else today.with(TemporalAdjusters.nextOrSame(dow))
            date = base
        }
        if (date == null) cut(Regex("""\b(on |by )?(\d{1,2})(?:st|nd|rd|th)?\s+(of\s+)?$MONTH_RX(\s+(\d{4}))?\b""", ci)) { m ->
            date = buildDate(m.groupValues[2].toInt(), m.groupValues[4], m.groupValues[6], today)
        }
        if (date == null) cut(Regex("""\b(on |by )?$MONTH_RX\s+(\d{1,2})(?:st|nd|rd|th)?(,?\s+(\d{4}))?\b""", ci)) { m ->
            date = buildDate(m.groupValues[3].toInt(), m.groupValues[2], m.groupValues[5], today)
        }
        if (date == null) cut(Regex("""\b(on |by )?(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2,4}))?\b""", ci)) { m ->
            // Indian convention: day/month.
            val d = m.groupValues[2].toInt()
            val mo = m.groupValues[3].toInt()
            val yRaw = m.groupValues[4]
            val y = when {
                yRaw.isEmpty() -> null
                yRaw.length == 2 -> 2000 + yRaw.toInt()
                else -> yRaw.toInt()
            }
            date = runCatching {
                val candidate = LocalDate.of(y ?: today.year, mo, d)
                if (y == null && candidate.isBefore(today)) candidate.plusYears(1) else candidate
            }.getOrNull()
        }
        return DateTimeExtraction(date, time, s.trim().replace(Regex("\\s+"), " "))
    }

    private fun buildDate(day: Int, monthText: String, yearText: String, today: LocalDate): LocalDate? {
        val month = months[monthText.lowercase().take(3)] ?: return null
        val year = yearText.toIntOrNull()
        return runCatching {
            val candidate = LocalDate.of(year ?: today.year, month, day)
            if (year == null && candidate.isBefore(today)) candidate.plusYears(1) else candidate
        }.getOrNull()
    }
}
