package com.hitunguang.core.notification

import android.content.Context
import com.hitunguang.feature.budget.domain.model.Budget

enum class BudgetAlertLevel {
    NONE, THRESHOLD, EXCEEDED;

    companion object {
        fun of(spentAmount: Long, budgetAmount: Long, thresholdPercent: Int): BudgetAlertLevel = when {
            spentAmount <= 0L -> NONE
            spentAmount >= budgetAmount -> EXCEEDED
            spentAmount >= budgetAmount * (thresholdPercent / 100.0) -> THRESHOLD
            else -> NONE
        }

        fun shouldNotify(previous: BudgetAlertLevel, current: BudgetAlertLevel): Boolean =
            current.ordinal > previous.ordinal
    }
}

/**
 * Last alert level per budget id. The level is tied to the budget's updatedAt, so editing a
 * budget resets it; a rolled-over budget has a new id and starts from NONE.
 */
class BudgetAlertStore(context: Context) {
    private val prefs = context.getSharedPreferences("budget_alert_levels", Context.MODE_PRIVATE)

    fun lastLevel(budget: Budget): BudgetAlertLevel {
        val stored = prefs.getString(budget.id, null) ?: return BudgetAlertLevel.NONE
        val updatedAt = stored.substringBefore('|').toLongOrNull()
        if (updatedAt != budget.updatedAt) return BudgetAlertLevel.NONE
        return BudgetAlertLevel.entries.find { it.name == stored.substringAfter('|') } ?: BudgetAlertLevel.NONE
    }

    fun saveLevel(budget: Budget, level: BudgetAlertLevel) {
        prefs.edit().putString(budget.id, "${budget.updatedAt}|${level.name}").apply()
    }
}
