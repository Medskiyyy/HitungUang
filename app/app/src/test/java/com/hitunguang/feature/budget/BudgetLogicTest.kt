package com.hitunguang.feature.budget

import com.hitunguang.core.notification.BudgetAlertLevel
import com.hitunguang.core.notification.ReviewPeriods
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.model.TransferFee
import com.hitunguang.feature.budget.domain.util.BudgetDates
import com.hitunguang.feature.budget.domain.util.BudgetSpending
import com.hitunguang.feature.budget.presentation.BudgetSummary
import com.hitunguang.feature.budget.presentation.BudgetWithProgress
import com.hitunguang.feature.onboarding.presentation.components.formatThousands
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class BudgetLogicTest {

    private val jakarta = ZoneId.of("Asia/Jakarta")

    private fun millis(iso: String): Long = Instant.parse(iso).toEpochMilli()

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(jakarta).toInstant().toEpochMilli()

    private fun day(date: String) = LocalDate.parse(date)

    private fun period(start: String, end: String) =
        BudgetDates.startOfDay(day(start), jakarta) to BudgetDates.endOfDay(day(end), jakarta)

    // --- date conversion ---

    @Test
    fun pickerSelection_mapsToWholeLocalDay() {
        val picked = millis("2026-11-01T00:00:00Z") // DatePicker: UTC midnight of 1 Nov
        assertEquals(millis("2026-10-31T17:00:00Z"), BudgetDates.pickerToLocalStart(picked, jakarta))
        assertEquals(millis("2026-11-01T16:59:59.999Z"), BudgetDates.pickerToLocalEnd(picked, jakarta))
    }

    @Test
    fun storedLocalMillis_mapsBackToSamePickerDay() {
        val start = BudgetDates.pickerToLocalStart(millis("2026-11-01T00:00:00Z"), jakarta)
        val end = BudgetDates.pickerToLocalEnd(millis("2026-11-30T00:00:00Z"), jakarta)
        assertEquals(millis("2026-11-01T00:00:00Z"), BudgetDates.localToPicker(start, jakarta))
        assertEquals(millis("2026-11-30T00:00:00Z"), BudgetDates.localToPicker(end, jakarta))
    }

    @Test
    fun millisUntilNextMidnight_isLocal() {
        assertEquals(60_000L, BudgetDates.millisUntilNextMidnight(local("2026-11-09T23:59:00"), jakarta))
        assertEquals(24 * 3_600_000L, BudgetDates.millisUntilNextMidnight(local("2026-11-09T00:00:00"), jakarta))
    }

    // --- period math ---

    @Test
    fun nextPeriod_wholeMonthRollsToNextWholeMonth() {
        val (start, end) = period("2026-10-01", "2026-10-31")
        assertEquals(period("2026-11-01", "2026-11-30"), BudgetDates.nextPeriod(start, end, jakarta))

        val (janStart, janEnd) = period("2027-01-01", "2027-01-31")
        assertEquals(period("2027-02-01", "2027-02-28"), BudgetDates.nextPeriod(janStart, janEnd, jakarta))
    }

    @Test
    fun nextPeriod_legacyUtcMidnightMonthIsStillWholeMonth() {
        // Old builds stored the raw picker value (07:00 local) for both ends
        val start = millis("2026-11-01T00:00:00Z")
        val end = millis("2026-11-30T00:00:00Z")
        assertEquals(period("2026-12-01", "2026-12-31"), BudgetDates.nextPeriod(start, end, jakarta))
    }

    @Test
    fun nextPeriod_customLengthKeepsDayCount() {
        val (start, end) = period("2026-11-09", "2026-12-09") // 31 days
        assertEquals(period("2026-12-10", "2027-01-09"), BudgetDates.nextPeriod(start, end, jakarta))

        val (wStart, wEnd) = period("2026-11-02", "2026-11-08") // 7 days
        assertEquals(period("2026-11-09", "2026-11-15"), BudgetDates.nextPeriod(wStart, wEnd, jakarta))
    }

    @Test
    fun rollForward_skipsMissedPeriodsUntilNowIsInside() {
        val (start, end) = period("2026-07-01", "2026-07-31")
        val now = local("2026-11-09T10:00:00")
        assertEquals(period("2026-11-01", "2026-11-30"), BudgetDates.rollForward(start, end, now, jakarta))
    }

    @Test
    fun rollForward_endOfLastDayIsStillSamePeriod() {
        val (start, end) = period("2026-10-01", "2026-10-31")
        val now = local("2026-11-30T23:59:59")
        assertEquals(period("2026-11-01", "2026-11-30"), BudgetDates.rollForward(start, end, now, jakarta))
    }

    // --- spent computation ---

    private fun tx(
        id: String,
        type: String,
        amount: Long,
        date: Long,
        categoryId: String? = "cat-food",
        isDeleted: Boolean = false
    ) = TransactionWithDetails(
        id = id, accountId = "acc", accountName = "Dompet", categoryId = categoryId,
        categoryName = null, categoryIcon = null, receiptId = null, transactionType = type,
        title = id, note = null, amount = amount, transactionDate = date, isDeleted = isDeleted,
        deletedAt = null, createdAt = date, updatedAt = date
    )

    private fun budget(type: String, categoryId: String?, start: Long, end: Long, amount: Long = 100_000L) = Budget(
        id = "b-$type", categoryId = categoryId, budgetType = type, amount = amount, thresholdPercent = 80,
        startDate = start, endDate = end, isActive = true, createdAt = 0L, updatedAt = 0L
    )

    @Test
    fun spentAmount_usesExpensesOnlyAndAddsFeesToGlobal() {
        val (start, end) = period("2026-11-01", "2026-11-30")
        val transactions = listOf(
            tx("food", "EXPENSE", 30_000L, local("2026-11-30T23:30:00")),
            tx("transport", "EXPENSE", 10_000L, local("2026-11-05T08:00:00"), categoryId = "cat-transport"),
            tx("deleted", "EXPENSE", 99_000L, local("2026-11-05T08:00:00"), isDeleted = true),
            tx("legacy-fee", "TRANSFER_FEE", 5_000L, local("2026-11-05T08:00:00")),
            tx("income", "INCOME", 50_000L, local("2026-11-05T08:00:00")),
            tx("before", "EXPENSE", 7_000L, local("2026-10-31T23:59:59"))
        )
        val fees = listOf(
            TransferFee(local("2026-11-10T12:00:00"), 2_500L),
            TransferFee(local("2026-12-01T00:00:00"), 9_000L)
        )

        assertEquals(42_500L, BudgetSpending.spentAmount(budget("GLOBAL", null, start, end), transactions, fees))
        assertEquals(30_000L, BudgetSpending.spentAmount(budget("CATEGORY", "cat-food", start, end), transactions, fees))
        assertEquals(42_500L, BudgetSpending.totalExpense(transactions, fees, start, end))
    }

    // --- summary ---

    private fun progress(type: String, amount: Long, spent: Long) = BudgetWithProgress(
        budget = budget(type, if (type == "GLOBAL") null else "cat-$amount", 0L, 1L, amount),
        categoryName = null,
        spentAmount = spent,
        remainingAmount = (amount - spent).coerceAtLeast(0L),
        progressPercent = spent * 100f / amount,
        isOverBudget = spent > amount,
        isThresholdReached = spent * 100f / amount >= 80f
    )

    @Test
    fun summary_globalBudgetIsNotSummedWithCategoryBudgets() {
        val summary = BudgetSummary.of(
            listOf(progress("GLOBAL", 2_000_000L, 500_000L), progress("CATEGORY", 600_000L, 300_000L))
        )
        assertEquals(BudgetSummary(2_000_000L, 500_000L, 1_500_000L), summary)
    }

    @Test
    fun summary_withoutGlobalSumsCategoryBudgets() {
        val summary = BudgetSummary.of(
            listOf(progress("CATEGORY", 600_000L, 300_000L), progress("CATEGORY", 400_000L, 100_000L))
        )
        assertEquals(BudgetSummary(1_000_000L, 400_000L, 600_000L), summary)
    }

    // --- alert levels ---

    @Test
    fun alertLevel_fromSpentAmount() {
        assertEquals(BudgetAlertLevel.NONE, BudgetAlertLevel.of(0L, 100_000L, 80))
        assertEquals(BudgetAlertLevel.NONE, BudgetAlertLevel.of(79_999L, 100_000L, 80))
        assertEquals(BudgetAlertLevel.THRESHOLD, BudgetAlertLevel.of(80_000L, 100_000L, 80))
        assertEquals(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.of(100_000L, 100_000L, 80))
    }

    @Test
    fun alertLevel_notifiesOnlyWhenLevelIncreases() {
        assertTrue(BudgetAlertLevel.shouldNotify(BudgetAlertLevel.NONE, BudgetAlertLevel.THRESHOLD))
        assertTrue(BudgetAlertLevel.shouldNotify(BudgetAlertLevel.THRESHOLD, BudgetAlertLevel.EXCEEDED))
        assertTrue(BudgetAlertLevel.shouldNotify(BudgetAlertLevel.NONE, BudgetAlertLevel.EXCEEDED))
        assertFalse(BudgetAlertLevel.shouldNotify(BudgetAlertLevel.THRESHOLD, BudgetAlertLevel.THRESHOLD))
        assertFalse(BudgetAlertLevel.shouldNotify(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.EXCEEDED))
        assertFalse(BudgetAlertLevel.shouldNotify(BudgetAlertLevel.EXCEEDED, BudgetAlertLevel.THRESHOLD))
        assertFalse(BudgetAlertLevel.shouldNotify(BudgetAlertLevel.NONE, BudgetAlertLevel.NONE))
    }

    // --- review periods ---

    @Test
    fun reviewPeriods_useCalendarBounds() {
        assertEquals(day("2026-11-02") to day("2026-11-08"), ReviewPeriods.previousWeek(day("2026-11-09")))
        assertEquals(day("2026-11-02") to day("2026-11-08"), ReviewPeriods.previousWeek(day("2026-11-15")))
        assertEquals(day("2026-10-01") to day("2026-10-31"), ReviewPeriods.previousMonth(day("2026-11-01")))
        assertEquals(day("2025-12-01") to day("2025-12-31"), ReviewPeriods.previousMonth(day("2026-01-01")))
        assertEquals("Oktober", ReviewPeriods.monthName(day("2026-10-01")))
    }

    // --- onboarding amount ---

    @Test
    fun formatThousands_groupsDigits() {
        assertEquals("", formatThousands(""))
        assertEquals("500", formatThousands("500"))
        assertEquals("2.000.000", formatThousands("2000000"))
        assertEquals("15.000", formatThousands("15000"))
    }
}
