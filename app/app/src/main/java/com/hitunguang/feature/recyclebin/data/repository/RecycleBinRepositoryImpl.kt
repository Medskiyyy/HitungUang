package com.hitunguang.feature.recyclebin.data.repository

import androidx.room.withTransaction
import com.hitunguang.core.balance.BalanceCalculator
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.dao.AttachmentDao
import com.hitunguang.core.database.dao.RecycleBinDao
import com.hitunguang.core.database.dao.TransactionDao
import com.hitunguang.core.filemanager.AttachmentFileManager
import com.hitunguang.feature.recyclebin.data.mapper.RecycleBinMapper
import com.hitunguang.feature.recyclebin.domain.model.RecycleBinItem
import com.hitunguang.feature.recyclebin.domain.repository.RecycleBinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecycleBinRepositoryImpl @Inject constructor(
    private val database: HitungUangDatabase,
    private val recycleBinDao: RecycleBinDao,
    private val transactionDao: TransactionDao,
    private val attachmentDao: AttachmentDao,
    private val fileManager: AttachmentFileManager
) : RecycleBinRepository {

    override fun getDeletedItems(): Flow<List<RecycleBinItem>> {
        return recycleBinDao.getDeletedItemsWithDetails().map { list ->
            list.map { RecycleBinMapper.toDomain(it) }
        }
    }

    override suspend fun restoreTransaction(transactionId: String) {
        database.withTransaction {
            val transactionEntity = transactionDao.getTransactionEntityById(transactionId) ?: return@withTransaction
            val balanceDifference = BalanceCalculator.calculateDifference(
                transactionEntity.transactionType,
                transactionEntity.amount
            )
            val now = System.currentTimeMillis()

            // 1. Update transaction state (is_deleted = 0, deleted_at = null)
            transactionDao.restoreTransactionState(transactionId, now)

            // 2. Adjust account balance
            transactionDao.adjustAccountBalance(transactionEntity.accountId, balanceDifference, now)

            // 3. Re-index in FTS
            transactionDao.indexTransaction(
                transactionEntity.id,
                transactionEntity.title,
                transactionEntity.note,
                transactionEntity.categoryId,
                transactionEntity.receiptId
            )

            // 4. Delete recycle bin entry
            val recycleBinEntry = recycleBinDao.getAllDeletedItems().first().find { it.entityId == transactionId }
            if (recycleBinEntry != null) {
                recycleBinDao.removeFromRecycleBin(recycleBinEntry)
            }
        }
    }

    override suspend fun permanentDeleteTransaction(transactionId: String) {
        // Fetch attachments first
        val attachments = attachmentDao.getAttachmentsByTransactionIdDirect(transactionId)
        // Delete physical files from disk
        attachments.forEach { attachment ->
            fileManager.deleteFile(attachment.filePath)
        }

        database.withTransaction {
            // Delete FTS search index
            transactionDao.deindexTransaction(transactionId)

            // Hard delete transaction (cascades database attachments)
            transactionDao.hardDeleteTransaction(transactionId)

            // Remove recycle bin entry
            val recycleBinEntry = recycleBinDao.getAllDeletedItems().first().find { it.entityId == transactionId }
            if (recycleBinEntry != null) {
                recycleBinDao.removeFromRecycleBin(recycleBinEntry)
            }
        }
    }

    override suspend fun restoreItem(entityId: String, entityType: String) {
        if (entityType == "TRANSACTION") {
            restoreTransaction(entityId)
            return
        }
        database.withTransaction {
            val now = System.currentTimeMillis()
            when (entityType) {
                "CATEGORY" -> database.categoryDao().restoreCategoryState(entityId, now)
                "WALLET" -> database.accountDao().restoreAccountState(entityId, now)
                "BUDGET" -> database.budgetDao().restoreBudgetState(entityId, now)
            }

            val recycleBinEntry = recycleBinDao.getAllDeletedItems().first().find {
                it.entityId == entityId && it.entityType == entityType
            }
            if (recycleBinEntry != null) {
                recycleBinDao.removeFromRecycleBin(recycleBinEntry)
            }
        }
    }

    override suspend fun permanentDeleteItem(entityId: String, entityType: String) {
        if (entityType == "TRANSACTION") {
            permanentDeleteTransaction(entityId)
            return
        }
        database.withTransaction {
            when (entityType) {
                "CATEGORY" -> {
                    val category = database.categoryDao().getCategoryByIdDirect(entityId)
                    if (category == null || !category.isDefault) {
                        database.categoryDao().hardDeleteCategory(entityId)
                    }
                }
                "WALLET" -> database.accountDao().hardDeleteAccount(entityId)
                "BUDGET" -> database.budgetDao().hardDeleteBudget(entityId)
            }

            val recycleBinEntry = recycleBinDao.getAllDeletedItems().first().find {
                it.entityId == entityId && it.entityType == entityType
            }
            if (recycleBinEntry != null) {
                recycleBinDao.removeFromRecycleBin(recycleBinEntry)
            }
        }
    }

    override suspend fun cleanupExpiredItems(currentTime: Long) {
        val expiredEntries = recycleBinDao.getExpiredItems(currentTime)
        expiredEntries.forEach { entry ->
            permanentDeleteItem(entry.entityId, entry.entityType)
        }
    }
}
