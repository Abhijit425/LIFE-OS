package com.lifeos.app.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source of "now" and the current zone. The zone is read on every call (not cached) so the app
 * follows time-zone changes immediately. Tests substitute a fixed implementation.
 */
interface TimeProvider {
    fun now(): Instant
    fun zone(): ZoneId
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/** Start (inclusive) and end (exclusive) of a local day, as instants. */
data class InstantRange(val start: Instant, val end: Instant)

fun LocalDate.dayRange(zone: ZoneId): InstantRange =
    InstantRange(atStartOfDay(zone).toInstant(), plusDays(1).atStartOfDay(zone).toInstant())

fun YearMonth.monthRange(zone: ZoneId): InstantRange =
    InstantRange(atDay(1).atStartOfDay(zone).toInstant(), plusMonths(1).atDay(1).atStartOfDay(zone).toInstant())

/** Budget month key, e.g. November 2026 -> 202611. */
fun YearMonth.monthKey(): Int = year * 100 + monthValue

enum class DayPart(val greeting: String) {
    MORNING("Good morning"), AFTERNOON("Good afternoon"), EVENING("Good evening"), NIGHT("Good night")
}

/** 05–11 morning, 12–16 afternoon, 17–21 evening, otherwise night. */
fun dayPartFor(hour: Int): DayPart = when (hour) {
    in 5..11 -> DayPart.MORNING
    in 12..16 -> DayPart.AFTERNOON
    in 17..21 -> DayPart.EVENING
    else -> DayPart.NIGHT
}
