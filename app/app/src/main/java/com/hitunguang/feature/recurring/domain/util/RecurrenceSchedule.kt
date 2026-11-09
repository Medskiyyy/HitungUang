package com.hitunguang.feature.recurring.domain.util

import com.hitunguang.feature.recurring.domain.model.RecurrenceRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/** Occurrence math for recurring transactions. Every occurrence is posted at 07:00 local time. */
object RecurrenceSchedule {
    val POST_TIME: LocalTime = LocalTime.of(7, 0)

    fun runAt(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atTime(POST_TIME).atZone(zone).toInstant().toEpochMilli()

    fun dateOf(runAt: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(runAt).atZone(zone).toLocalDate()

    /** First occurrence on or after [startDate]. */
    fun firstDate(rule: RecurrenceRule, startDate: LocalDate): LocalDate = when (rule) {
        RecurrenceRule.Daily, RecurrenceRule.Weekly -> startDate
        is RecurrenceRule.Monthly -> {
            val candidate = monthlyDate(YearMonth.from(startDate), rule.dayOfMonth)
            if (candidate.isBefore(startDate)) {
                monthlyDate(YearMonth.from(startDate).plusMonths(1), rule.dayOfMonth)
            } else {
                candidate
            }
        }
    }

    fun nextDate(rule: RecurrenceRule, current: LocalDate): LocalDate = when (rule) {
        RecurrenceRule.Daily -> current.plusDays(1)
        RecurrenceRule.Weekly -> current.plusWeeks(1)
        is RecurrenceRule.Monthly -> monthlyDate(YearMonth.from(current).plusMonths(1), rule.dayOfMonth)
    }

    fun firstRunAt(rule: RecurrenceRule, startDate: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        runAt(firstDate(rule, startDate), zone)

    fun nextRunAt(rule: RecurrenceRule, currentRunAt: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        runAt(nextDate(rule, dateOf(currentRunAt, zone)), zone)

    /** Skips occurrences dated before [today], e.g. the ones missed while a rule was paused. */
    fun skipPast(rule: RecurrenceRule, runAt: Long, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long {
        var date = dateOf(runAt, zone)
        while (date.isBefore(today)) {
            date = nextDate(rule, date)
        }
        return runAt(date, zone)
    }

    private fun monthlyDate(month: YearMonth, dayOfMonth: Int): LocalDate =
        month.atDay(minOf(dayOfMonth, month.lengthOfMonth()))
}
