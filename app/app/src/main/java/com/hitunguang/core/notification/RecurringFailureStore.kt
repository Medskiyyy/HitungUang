package com.hitunguang.core.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Remembers, per recurring rule, the occurrence whose failure was already notified,
 * so a rule stuck on the same occurrence is reported once instead of on every run.
 */
class RecurringFailureStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("recurring_failures", Context.MODE_PRIVATE)

    fun shouldNotify(ruleId: String, occurrence: Long): Boolean =
        prefs.getLong(ruleId, Long.MIN_VALUE) != occurrence

    fun markNotified(ruleId: String, occurrence: Long) {
        prefs.edit().putLong(ruleId, occurrence).apply()
    }

    fun clear(ruleId: String) {
        if (prefs.contains(ruleId)) prefs.edit().remove(ruleId).apply()
    }
}
