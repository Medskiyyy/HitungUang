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
            c.name AS category_name,
            
            cat.id AS joined_category_id,
            cat.name AS joined_category_name,
            cat.category_type AS joined_category_category_type,
            cat.icon AS joined_category_icon,
            cat.is_default AS joined_category_is_default,
            cat.is_pinned AS joined_category_is_pinned,
            cat.is_deleted AS joined_category_is_deleted,
            cat.deleted_at AS joined_category_deleted_at,
            cat.created_at AS joined_category_created_at,
            cat.updated_at AS joined_category_updated_at,
            
            acc.id AS joined_account_id,
            acc.name AS joined_account_name,
            acc.account_type AS joined_account_account_type,
            acc.icon AS joined_account_icon,
            acc.initial_balance AS joined_account_initial_balance,
            acc.current_balance AS joined_account_current_balance,
            acc.is_deleted AS joined_account_is_deleted,
            acc.deleted_at AS joined_account_deleted_at,
            acc.created_at AS joined_account_created_at,
            acc.updated_at AS joined_account_updated_at,
            
            b.id AS joined_budget_id,
            b.category_id AS joined_budget_category_id,
            b.budget_type AS joined_budget_budget_type,
            b.amount AS joined_budget_amount,
            b.threshold_percent AS joined_budget_threshold_percent,
            b.start_date AS joined_budget_start_date,
            b.end_date AS joined_budget_end_date,
            b.is_active AS joined_budget_is_active,
            b.is_deleted AS joined_budget_is_deleted,
            b.deleted_at AS joined_budget_deleted_at,
            b.created_at AS joined_budget_created_at,
            b.updated_at AS joined_budget_updated_at,
            
            c_b.name AS budget_category_name
        FROM recycle_bin r
        LEFT JOIN transactions t ON r.entity_id = t.id AND r.entity_type = 'TRANSACTION'
        LEFT JOIN accounts a ON t.account_id = a.id
        LEFT JOIN categories c ON t.category_id = c.id
        LEFT JOIN categories cat ON r.entity_id = cat.id AND r.entity_type = 'CATEGORY'
        LEFT JOIN accounts acc ON r.entity_id = acc.id AND r.entity_type = 'WALLET'
        LEFT JOIN budgets b ON r.entity_id = b.id AND r.entity_type = 'BUDGET'
        LEFT JOIN categories c_b ON b.category_id = c_b.id
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
