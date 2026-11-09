package com.hitunguang.feature.recurring.presentation

import com.hitunguang.feature.recurring.domain.model.RecurrenceRule
import com.hitunguang.feature.recurring.domain.util.RecurrenceSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurrenceTextTest {

    @Test
    fun describesRulesInWords() {
        val monday = RecurrenceSchedule.runAt(LocalDate.of(2026, 11, 9))
        assertEquals("Tiap hari", RecurrenceText.describe(RecurrenceRule.Daily, monday))
        assertEquals("Tiap Senin", RecurrenceText.describe(RecurrenceRule.Weekly, monday))
        assertEquals("Tiap bulan tgl 25", RecurrenceText.describe(RecurrenceRule.Monthly(25), monday))
        assertEquals("Tiap Minggu", RecurrenceText.describe(RecurrenceRule.Weekly, RecurrenceSchedule.runAt(LocalDate.of(2026, 11, 15))))
    }
}
