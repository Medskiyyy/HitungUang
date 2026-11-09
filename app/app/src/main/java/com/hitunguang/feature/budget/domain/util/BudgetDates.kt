package com.hitunguang.feature.budget.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Budget periods are stored as local-time millis: start = 00:00:00.000 of the first day,
 * end = 23:59:59.999 of the last day, both in the device zone.
 * Material DatePicker works with UTC-midnight millis, so picker values must be converted.
 */
object BudgetDates {

    fun toLocalDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun startOfDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L

    fun startOfDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        startOfDay(toLocalDate(millis, zone), zone)

    fun endOfDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        endOfDay(toLocalDate(millis, zone), zone)

    /** DatePicker selection (UTC midnight) -> local start of that calendar day. */
    fun pickerToLocalStart(pickerMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        startOfDay(toLocalDate(pickerMillis, ZoneOffset.UTC), zone)

    /** DatePicker selection (UTC midnight) -> local 23:59:59.999 of that calendar day. */
    fun pickerToLocalEnd(pickerMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        endOfDay(toLocalDate(pickerMillis, ZoneOffset.UTC), zone)

    /** Stored local millis -> UTC midnight of the same local date, for DatePicker initialization. */
    fun localToPicker(localMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        toLocalDate(localMillis, zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun millisUntilNextMidnight(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        startOfDay(toLocalDate(nowMillis, zone).plusDays(1), zone) - nowMillis

    /**
     * The period right after [startMillis]..[endMillis]: a whole calendar month rolls to the next
     * whole calendar month, anything else keeps its length in days and starts the day after it ends.
     */
    fun nextPeriod(startMillis: Long, endMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val start = toLocalDate(startMillis, zone)
        val end = toLocalDate(endMillis, zone)
        val (nextStart, nextEnd) = if (start.dayOfMonth == 1 && end == start.with(TemporalAdjusters.lastDayOfMonth())) {
            val first = start.plusMonths(1)
            first to first.with(TemporalAdjusters.lastDayOfMonth())
        } else {
            val days = (ChronoUnit.DAYS.between(start, end) + 1).coerceAtLeast(1L)
            val first = end.plusDays(1)
            first to first.plusDays(days - 1)
        }
        return startOfDay(nextStart, zone) to endOfDay(nextEnd, zone)
    }

    /** Rolls forward period by period until the period ends at or after [nowMillis]. */
    fun rollForward(startMillis: Long, endMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        var period = nextPeriod(startMillis, endMillis, zone)
        while (period.second < nowMillis) {
            period = nextPeriod(period.first, period.second, zone)
        }
        return period
    }
}
