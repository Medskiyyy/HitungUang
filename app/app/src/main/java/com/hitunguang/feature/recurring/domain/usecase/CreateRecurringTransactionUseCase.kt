package com.hitunguang.feature.recurring.domain.usecase

import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.domain.repository.RecurringTransactionRepository
import javax.inject.Inject

class CreateRecurringTransactionUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository
) {
    suspend operator fun invoke(recurring: RecurringTransaction) {
        validateRecurring(recurring)
        repository.insert(recurring)
    }
}

internal fun validateRecurring(recurring: RecurringTransaction) {
    if (recurring.title.isBlank()) {
        throw IllegalArgumentException("Judulnya belum diisi.")
    }
    if (recurring.amount <= 0L) {
        throw IllegalArgumentException("Nominal transaksi harus lebih dari 0.")
    }
    if (recurring.transactionType != "INCOME" && recurring.transactionType != "EXPENSE") {
        throw IllegalArgumentException("Jenis transaksi harus pemasukan atau pengeluaran.")
    }
    if (recurring.accountId.isBlank()) {
        throw IllegalArgumentException("Pilih dompetnya dulu.")
    }
}
