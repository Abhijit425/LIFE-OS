package com.lifeos.app.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Time is stored as epoch millis (UTC instants) so records survive time-zone changes;
 * calendar dates (birthdays, due-dates without time) are stored as epoch days.
 * Enums use Room's built-in name-based converter. Money is stored as Long paise.
 */
class Converters {
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun dateToLong(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun longToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
}
