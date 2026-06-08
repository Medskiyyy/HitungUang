package com.hitunguang.core.balance

/**
 * Single source of truth untuk kalkulasi perubahan saldo akun.
 * Object murni tanpa dependency — dapat digunakan di mana saja.
 */
object BalanceCalculator {

    /**
     * Menghitung perubahan saldo (delta) berdasarkan tipe dan nominal transaksi.
     * INCOME → +amount, EXPENSE/TRANSFER_FEE → -amount.
     */
    fun calculateDifference(transactionType: String, amount: Long): Long {
        return when (transactionType) {
            "INCOME" -> amount
            "EXPENSE", "TRANSFER_FEE" -> -amount
            else -> 0L
        }
    }

    /**
     * Menghitung pembalikan saldo (untuk pembatalan/soft-delete transaksi).
     * Kebalikan dari [calculateDifference].
     */
    fun calculateReversal(transactionType: String, amount: Long): Long {
        return -calculateDifference(transactionType, amount)
    }
}
