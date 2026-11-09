package com.hitunguang.feature.recurring.presentation

import android.content.Context
import com.hitunguang.R
import com.hitunguang.feature.recurring.domain.model.RecurrenceRule
import com.hitunguang.feature.recurring.domain.util.RecurrenceSchedule
import java.time.DayOfWeek

object RecurrenceText {
    private val weekdays = listOf(
        R.string.recurring_weekday_1,
        R.string.recurring_weekday_2,
        R.string.recurring_weekday_3,
        R.string.recurring_weekday_4,
        R.string.recurring_weekday_5,
        R.string.recurring_weekday_6,
        R.string.recurring_weekday_7
    )

    fun weekdayName(context: Context, dayOfWeek: DayOfWeek): String = context.getString(weekdays[dayOfWeek.value - 1])

    /** "Tiap hari", "Tiap Senin" (weekday of the next occurrence) or "Tiap bulan tgl 25". */
    fun describe(context: Context, rule: RecurrenceRule, nextRunAt: Long): String = when (rule) {
        RecurrenceRule.Daily -> context.getString(R.string.recurring_every_day)
        RecurrenceRule.Weekly -> context.getString(R.string.recurring_every_weekday, weekdayName(context, RecurrenceSchedule.dateOf(nextRunAt).dayOfWeek))
        is RecurrenceRule.Monthly -> context.getString(R.string.recurring_every_month_day, rule.dayOfMonth)
    }
}
