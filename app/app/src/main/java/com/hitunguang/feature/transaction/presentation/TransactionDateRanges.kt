package com.hitunguang.feature.transaction.presentation

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

object TransactionDateRanges {

    // Monday 00:00:00.000 .. Sunday 23:59:59.999 of the week containing [today], in [zone].
    fun weekRange(today: LocalDate, zone: ZoneId): Pair<Long, Long> {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val startMs = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = weekStart.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return startMs to endMs
    }

    // DateRangePicker returns UTC-midnight millis; map the picked calendar days to local day bounds.
    fun pickerRangeToLocal(startUtcMillis: Long, endUtcMillis: Long, zone: ZoneId): Pair<Long, Long> {
        val startDate = Instant.ofEpochMilli(startUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val endDate = Instant.ofEpochMilli(endUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val startMs = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return startMs to endMs
    }
}
