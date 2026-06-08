package com.hitunguang.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hitunguang.core.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE transaction_id = :transactionId ORDER BY created_at ASC")
    fun getAttachmentsByTransactionId(transactionId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT COUNT(*) FROM attachments WHERE transaction_id = :transactionId")
    suspend fun getAttachmentCountForTransaction(transactionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE transaction_id = :transactionId")
    suspend fun deleteAttachmentsByTransactionId(transactionId: String)

    @Query("SELECT * FROM attachments WHERE transaction_id = :transactionId")
    suspend fun getAttachmentsByTransactionIdDirect(transactionId: String): List<AttachmentEntity>
}
