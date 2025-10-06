package com.example.strive.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object DateUtils {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

    fun nowDateString(zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.now(zoneId).format(dateFormatter)

    fun millisToDateString(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().format(dateFormatter)

    fun calendarToDateString(calendar: Calendar): String {
        val localDate = LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        return localDate.format(dateFormatter)
    }
}

