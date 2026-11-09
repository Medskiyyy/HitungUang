package com.hitunguang.feature.recurring.presentation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.feature.recurring.domain.model.RecurrenceRule
import com.hitunguang.feature.recurring.domain.util.RecurrenceSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RecurrenceTextTest {

    @Test
    fun describesRulesInWords() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val monday = RecurrenceSchedule.runAt(LocalDate.of(2026, 11, 9))
        assertEquals("Tiap hari", RecurrenceText.describe(context, RecurrenceRule.Daily, monday))
        assertEquals("Tiap Senin", RecurrenceText.describe(context, RecurrenceRule.Weekly, monday))
        assertEquals("Tiap bulan tgl 25", RecurrenceText.describe(context, RecurrenceRule.Monthly(25), monday))
        assertEquals("Tiap Minggu", RecurrenceText.describe(context, RecurrenceRule.Weekly, RecurrenceSchedule.runAt(LocalDate.of(2026, 11, 15))))
    }
}
