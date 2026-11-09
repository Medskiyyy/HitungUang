package com.hitunguang.feature.recurring

import com.hitunguang.feature.recurring.domain.model.RecurrenceRule
import com.hitunguang.feature.recurring.domain.util.RecurrenceSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class RecurrenceScheduleTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    private fun date(text: String) = LocalDate.parse(text)

    @Test
    fun ruleEncodesAndParses() {
        listOf(RecurrenceRule.Daily, RecurrenceRule.Weekly, RecurrenceRule.Monthly(25)).forEach {
            assertEquals(it, RecurrenceRule.parse(it.encode()))
        }
        assertEquals("MONTHLY:31", RecurrenceRule.Monthly(31).encode())
    }

    @Test(expected = IllegalArgumentException::class)
    fun monthlyRejectsDayOutOfRange() {
        RecurrenceRule.parse("MONTHLY:32")
    }

    @Test
    fun dailyAdvancesOneDayAt7() {
        val runAt = RecurrenceSchedule.firstRunAt(RecurrenceRule.Daily, date("2026-12-31"), zone)
        val local = Instant.ofEpochMilli(runAt).atZone(zone)
        assertEquals(date("2026-12-31"), local.toLocalDate())
        assertEquals(LocalTime.of(7, 0), local.toLocalTime())

        val next = RecurrenceSchedule.nextRunAt(RecurrenceRule.Daily, runAt, zone)
        assertEquals(RecurrenceSchedule.runAt(date("2027-01-01"), zone), next)
    }

    @Test
    fun weeklyKeepsTheStartWeekday() {
        val start = date("2026-11-09")
        assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)
        assertEquals(start, RecurrenceSchedule.firstDate(RecurrenceRule.Weekly, start))
        val next = RecurrenceSchedule.nextDate(RecurrenceRule.Weekly, start)
        assertEquals(date("2026-11-16"), next)
        assertEquals(DayOfWeek.MONDAY, next.dayOfWeek)
    }

    @Test
    fun monthlyOn31UsesLastDayButKeepsTheOriginalDay() {
        val rule = RecurrenceRule.Monthly(31)
        var d = RecurrenceSchedule.firstDate(rule, date("2026-10-01"))
        val dates = mutableListOf(d)
        repeat(5) {
            d = RecurrenceSchedule.nextDate(rule, d)
            dates.add(d)
        }
        assertEquals(
            listOf("2026-10-31", "2026-11-30", "2026-12-31", "2027-01-31", "2027-02-28", "2027-03-31").map(::date),
            dates
        )
    }

    @Test
    fun monthlyFebruaryInLeapYear() {
        val rule = RecurrenceRule.Monthly(30)
        val feb = RecurrenceSchedule.nextDate(rule, date("2028-01-30"))
        assertEquals(date("2028-02-29"), feb)
        assertEquals(date("2028-03-30"), RecurrenceSchedule.nextDate(rule, feb))

        assertEquals(date("2027-02-28"), RecurrenceSchedule.nextDate(RecurrenceRule.Monthly(29), date("2027-01-29")))
    }

    @Test
    fun monthlyFirstOccurrenceIsOnOrAfterStart() {
        val rule = RecurrenceRule.Monthly(25)
        assertEquals(date("2026-11-25"), RecurrenceSchedule.firstDate(rule, date("2026-11-09")))
        assertEquals(date("2026-11-25"), RecurrenceSchedule.firstDate(rule, date("2026-11-25")))
        assertEquals(date("2026-12-25"), RecurrenceSchedule.firstDate(rule, date("2026-11-26")))
        assertEquals(date("2027-02-28"), RecurrenceSchedule.firstDate(RecurrenceRule.Monthly(31), date("2027-02-01")))
    }

    @Test
    fun skipPastLandsOnTodayOrLater() {
        val runAt = RecurrenceSchedule.runAt(date("2026-11-01"), zone)
        assertEquals(
            RecurrenceSchedule.runAt(date("2026-11-09"), zone),
            RecurrenceSchedule.skipPast(RecurrenceRule.Daily, runAt, date("2026-11-09"), zone)
        )
        assertEquals(
            RecurrenceSchedule.runAt(date("2026-11-15"), zone),
            RecurrenceSchedule.skipPast(RecurrenceRule.Weekly, runAt, date("2026-11-09"), zone)
        )
        val future = RecurrenceSchedule.runAt(date("2026-12-01"), zone)
        assertEquals(future, RecurrenceSchedule.skipPast(RecurrenceRule.Daily, future, date("2026-11-09"), zone))
    }
}
