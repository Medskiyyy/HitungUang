package com.hitunguang.core.balance

/**
 * Hasil validasi integritas saldo sebuah akun.
 */
data class BalanceValidationResult(
    val accountId: String,
    val storedBalance: Long,
    val calculatedBalance: Long,
    val isConsistent: Boolean
)
