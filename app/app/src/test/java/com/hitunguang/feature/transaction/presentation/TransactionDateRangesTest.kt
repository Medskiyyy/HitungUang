package com.hitunguang.feature.transaction.presentation

import com.hitunguang.feature.transaction.domain.usecase.SearchTransactionsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class TransactionDateRangesTest {

    private val jakarta = ZoneId.of("Asia/Jakarta")

    private fun local(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(jakarta).toInstant().toEpochMilli()

    @Test
    fun weekRange_onSunday_isCurrentWeek() {
        // 2026-11-08 is a Sunday
        val (start, end) = TransactionDateRanges.weekRange(LocalDate.of(2026, 11, 8), jakarta)
        assertEquals(local(2026, 11, 2), start)
        assertEquals(local(2026, 11, 9) - 1, end)
    }

    @Test
    fun weekRange_onMonday_startsToday() {
        val (start, end) = TransactionDateRanges.weekRange(LocalDate.of(2026, 11, 9), jakarta)
        assertEquals(local(2026, 11, 9), start)
        assertEquals(local(2026, 11, 16) - 1, end)
    }

    @Test
    fun pickerRange_coversWholeLocalDays() {
        val startUtc = LocalDate.of(2026, 11, 3).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endUtc = LocalDate.of(2026, 11, 5).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val (start, end) = TransactionDateRanges.pickerRangeToLocal(startUtc, endUtc, jakarta)
        assertEquals(local(2026, 11, 3), start)
        assertEquals(local(2026, 11, 6) - 1, end)
        // 01:30 local on the start day must be inside the range
        val earlyMorning = local(2026, 11, 3, 1, 30)
        assertEquals(true, earlyMorning in start..end)
    }

    @Test
    fun ftsQuery_stripsSpecialCharacters() {
        assertEquals("makan* bakso*", SearchTransactionsUseCase.toFtsQuery("  Makan \"bakso*"))
        assertEquals("rp* 50* 000*", SearchTransactionsUseCase.toFtsQuery("rp.50,000"))
        assertEquals("kopi* susu*", SearchTransactionsUseCase.toFtsQuery("-kopi (susu)"))
        assertEquals("café* 東京*", SearchTransactionsUseCase.toFtsQuery("café 東京"))
        assertNull(SearchTransactionsUseCase.toFtsQuery("\"*-():^"))
        assertNull(SearchTransactionsUseCase.toFtsQuery("   "))
    }
}
