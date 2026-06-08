package com.hitunguang.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hitunguang.core.database.entity.RecycleBinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecycleBinDao {
    @Query("SELECT * FROM recycle_bin ORDER BY deleted_at DESC")
    fun getAllDeletedItems(): Flow<List<RecycleBinEntity>>

    @Query("""
        SELECT 
            r.id AS recycle_bin_id,
            r.entity_type AS recycle_bin_entity_type,
            r.entity_id AS recycle_bin_entity_id,
            r.deleted_at AS recycle_bin_deleted_at,
            r.expire_at AS recycle_bin_expire_at,
            t.id AS id,
            t.account_id AS account_id,
            t.category_id AS category_id,
            t.receipt_id AS receipt_id,
            t.transaction_type AS transaction_type,
            t.title AS title,
            t.note AS note,
            t.amount AS amount,
            t.transaction_date AS transaction_date,
            t.is_deleted AS is_deleted,
            t.deleted_at AS deleted_at,
            t.created_at AS created_at,
            t.updated_at AS updated_at,
            a.name AS account_name,
            c.name AS category_name
        FROM recycle_bin r
        LEFT JOIN transactions t ON r.entity_id = t.id AND r.entity_type = 'TRANSACTION'
        LEFT JOIN accounts a ON t.account_id = a.id
        LEFT JOIN categories c ON t.category_id = c.id
        ORDER BY r.deleted_at DESC
    """)
    fun getDeletedItemsWithDetails(): Flow<List<com.hitunguang.core.database.entity.DeletedItemWithDetailsEntity>>

    @Query("SELECT * FROM recycle_bin WHERE expire_at <= :currentTime")
    suspend fun getExpiredItems(currentTime: Long): List<RecycleBinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToRecycleBin(item: RecycleBinEntity)

    @Delete
    suspend fun removeFromRecycleBin(item: RecycleBinEntity)

    @Query("DELETE FROM recycle_bin WHERE expire_at <= :currentTime")
    suspend fun cleanupExpiredItems(currentTime: Long)
}
