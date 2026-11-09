package com.hitunguang.core.notification

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Calendar bounds (local dates, inclusive) for the weekly and monthly review notifications. */
object ReviewPeriods {

    fun previousWeek(today: LocalDate): Pair<LocalDate, LocalDate> {
        val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return thisMonday.minusWeeks(1) to thisMonday.minusDays(1)
    }

    fun previousMonth(today: LocalDate): Pair<LocalDate, LocalDate> {
        val first = today.withDayOfMonth(1).minusMonths(1)
        return first to first.with(TemporalAdjusters.lastDayOfMonth())
    }

    fun monthName(date: LocalDate): String =
        date.month.getDisplayName(TextStyle.FULL, Locale("in", "ID"))
}
