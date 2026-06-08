package com.hitunguang.feature.receipt.data.repository

import com.hitunguang.core.database.dao.ReceiptDao
import com.hitunguang.core.filemanager.ReceiptFileManager
import com.hitunguang.feature.receipt.data.mapper.ReceiptMapper
import com.hitunguang.feature.receipt.domain.model.Receipt
import com.hitunguang.feature.receipt.domain.model.ReceiptItem
import com.hitunguang.feature.receipt.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val receiptFileManager: ReceiptFileManager
) : ReceiptRepository {

    override fun getAllReceipts(): Flow<List<Receipt>> {
        return receiptDao.getAllReceipts().map { ReceiptMapper.toDomainList(it) }
    }

    override fun getReceiptById(id: String): Flow<Receipt?> {
        return receiptDao.getReceiptById(id).map { it?.let { ReceiptMapper.toDomain(it) } }
    }

    override fun getReceiptItems(receiptId: String): Flow<List<ReceiptItem>> {
        return receiptDao.getReceiptItems(receiptId).map { ReceiptMapper.toDomainItemList(it) }
    }

    override suspend fun saveReceipt(receipt: Receipt, items: List<ReceiptItem>) {
        val receiptEntity = ReceiptMapper.toEntity(receipt)
        val itemEntities = ReceiptMapper.toEntityItemList(items)
        receiptDao.insertReceiptWithItems(receiptEntity, itemEntities)
    }

    override suspend fun deleteReceipt(receipt: Receipt) {
        receiptFileManager.deleteFile(receipt.imagePath)
        val receiptEntity = ReceiptMapper.toEntity(receipt)
        receiptDao.deleteReceipt(receiptEntity)
    }
}
