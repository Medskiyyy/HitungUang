package com.hitunguang.feature.recurring.presentation

import com.hitunguang.feature.recurring.domain.model.RecurrenceRule
import com.hitunguang.feature.recurring.domain.util.RecurrenceSchedule
import java.time.DayOfWeek

object RecurrenceText {
    private val weekdays = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    fun weekdayName(dayOfWeek: DayOfWeek): String = weekdays[dayOfWeek.value - 1]

    /** "Tiap hari", "Tiap Senin" (weekday of the next occurrence) or "Tiap bulan tgl 25". */
    fun describe(rule: RecurrenceRule, nextRunAt: Long): String = when (rule) {
        RecurrenceRule.Daily -> "Tiap hari"
        RecurrenceRule.Weekly -> "Tiap ${weekdayName(RecurrenceSchedule.dateOf(nextRunAt).dayOfWeek)}"
        is RecurrenceRule.Monthly -> "Tiap bulan tgl ${rule.dayOfMonth}"
    }
}
