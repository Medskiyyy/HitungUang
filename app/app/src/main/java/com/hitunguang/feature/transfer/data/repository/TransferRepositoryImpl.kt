package com.hitunguang.feature.transfer.data.repository

import com.hitunguang.core.database.dao.TransferDao
import com.hitunguang.feature.transfer.data.mapper.TransferMapper
import com.hitunguang.feature.transfer.domain.model.Transfer
import com.hitunguang.feature.transfer.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransferRepositoryImpl @Inject constructor(
    private val transferDao: TransferDao
) : TransferRepository {
    override fun getAllTransfers(): Flow<List<Transfer>> {
        return transferDao.getAllTransfers().map { list ->
            list.map { TransferMapper.toDomain(it) }
        }
    }

    override fun getTransferById(id: String): Flow<Transfer?> {
        return transferDao.getTransferById(id).map { entity ->
            entity?.let { TransferMapper.toDomain(it) }
        }
    }

    override suspend fun executeTransfer(transfer: Transfer) {
        transferDao.executeTransfer(TransferMapper.toEntity(transfer))
    }

    override suspend fun revertTransfer(transfer: Transfer) {
        transferDao.revertTransfer(TransferMapper.toEntity(transfer))
    }
}
