package com.hitunguang.feature.recurring.domain.usecase

import com.hitunguang.core.notification.NotificationHelper
import com.hitunguang.core.notification.RecurringFailureStore
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.domain.repository.RecurringTransactionRepository
import com.hitunguang.feature.recurring.domain.util.RecurrenceSchedule
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject

/**
 * Posts every due occurrence of every active rule, catching up missed ones. Each occurrence is
 * posted and its next_run_at advanced in one DB transaction, so a crash can't double-post or skip.
 */
class ProcessDueRecurringUseCase @Inject constructor(
    private val repository: RecurringTransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val failureStore: RecurringFailureStore,
    private val notificationHelper: NotificationHelper
) {
    companion object {
        const val MAX_OCCURRENCES_PER_RUN = 60
    }

    /** Returns how many transactions were posted. */
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): Int {
        var posted = 0
        for (rule in repository.getDue(now)) {
            posted += processRule(rule, now)
        }
        return posted
    }

    private suspend fun processRule(rule: RecurringTransaction, now: Long): Int {
        var occurrence = rule.nextRunAt
        var posted = 0
        while (occurrence <= now && posted < MAX_OCCURRENCES_PER_RUN) {
            val next = try {
                missingTargetReason(rule)?.let { throw IllegalStateException(it) }
                postOccurrence(rule.id, occurrence, now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportFailure(rule, occurrence, e.message ?: "Ada yang gagal.")
                return posted
            }
            // Null: the rule was changed, paused or already posted by a concurrent run
            if (next == null) return posted
            occurrence = next
            posted++
        }
        failureStore.clear(rule.id)
        return posted
    }

    private suspend fun postOccurrence(ruleId: String, occurrence: Long, now: Long): Long? =
        repository.runInTransaction {
            val current = repository.getById(ruleId)
            if (current == null || !current.isActive || current.nextRunAt != occurrence) {
                return@runInTransaction null
            }
            createTransactionUseCase(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    accountId = current.accountId,
                    categoryId = current.categoryId,
                    receiptId = null,
                    transactionType = current.transactionType,
                    title = current.title,
                    note = current.note,
                    amount = current.amount,
                    transactionDate = occurrence,
                    isDeleted = false,
                    deletedAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
            val next = RecurrenceSchedule.nextRunAt(current.recurrenceRule, occurrence)
            repository.updateNextRunAt(ruleId, next, now)
            next
        }

    private suspend fun missingTargetReason(rule: RecurringTransaction): String? {
        if (accountRepository.getAccountById(rule.accountId).firstOrNull() == null) {
            return "dompetnya udah dihapus."
        }
        val categoryId = rule.categoryId ?: return null
        val category = categoryRepository.getCategoryByIdDirect(categoryId)
        if (category == null || category.isDeleted) {
            return "kategorinya udah dihapus."
        }
        return null
    }

    private fun reportFailure(rule: RecurringTransaction, occurrence: Long, reason: String) {
        if (!failureStore.shouldNotify(rule.id, occurrence)) return
        notificationHelper.showNotification(
            channelId = NotificationHelper.CHANNEL_RECURRING,
            notificationId = rule.id.hashCode(),
            title = "Transaksi berulang gagal dicatat",
            message = "Transaksi berulang \"${rule.title}\" gagal dicatat: $reason"
        )
        failureStore.markNotified(rule.id, occurrence)
    }
}
