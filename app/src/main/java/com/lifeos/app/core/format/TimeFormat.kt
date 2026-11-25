package com.lifeos.app.core.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormat {
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val longDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
    private val shortDate = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val monthShort = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

    fun time(at: Instant, zone: ZoneId): String = time.format(at.atZone(zone))

    fun longDate(date: LocalDate): String = longDate.format(date)

    fun shortDate(date: LocalDate): String = shortDate.format(date)

    fun monthShort(month: java.time.YearMonth): String = monthShort.format(month)

    /** "Today 5:00 PM", "Tomorrow 9:00 AM", or "Fri, 28 Nov 9:00 AM". */
    fun relative(at: Instant, today: LocalDate, zone: ZoneId): String {
        val date = at.atZone(zone).toLocalDate()
        val day = when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> shortDate.format(date)
        }
        return "$day ${time(at, zone)}"
    }
}
