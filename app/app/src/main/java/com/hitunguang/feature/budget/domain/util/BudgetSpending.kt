package com.hitunguang.feature.budget.domain.util

import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.model.TransferFee
import com.hitunguang.feature.transaction.domain.model.TransactionWithDetails

/**
 * Single definition of "spent": non-deleted EXPENSE transactions in the period, plus transfer
 * admin fees (transfers.admin_fee is the source of truth; fees have no category, so they only
 * count toward GLOBAL budgets and overall totals).
 */
object BudgetSpending {

    fun expenseSum(
        transactions: List<TransactionWithDetails>,
        startMillis: Long,
        endMillis: Long,
        categoryId: String? = null
    ): Long = transactions
        .filter {
            !it.isDeleted &&
            it.transactionType == "EXPENSE" &&
            it.transactionDate in startMillis..endMillis &&
            (categoryId == null || it.categoryId == categoryId)
        }
        .sumOf { it.amount }

    fun transferFeeSum(transferFees: List<TransferFee>, startMillis: Long, endMillis: Long): Long =
        transferFees.filter { it.transferDate in startMillis..endMillis }.sumOf { it.adminFee }

    fun totalExpense(
        transactions: List<TransactionWithDetails>,
        transferFees: List<TransferFee>,
        startMillis: Long,
        endMillis: Long
    ): Long = expenseSum(transactions, startMillis, endMillis) +
        transferFeeSum(transferFees, startMillis, endMillis)

    fun spentAmount(
        budget: Budget,
        transactions: List<TransactionWithDetails>,
        transferFees: List<TransferFee>
    ): Long {
        val expenses = expenseSum(transactions, budget.startDate, budget.endDate, budget.categoryId)
        val fees = if (budget.budgetType == "GLOBAL") {
            transferFeeSum(transferFees, budget.startDate, budget.endDate)
        } else {
            0L
        }
        return expenses + fees
    }
}
