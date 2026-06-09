package com.hitunguang.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class DeletedItemWithDetailsEntity(
    @ColumnInfo(name = "recycle_bin_id") val recycleBinId: String,
    @ColumnInfo(name = "recycle_bin_entity_type") val entityType: String,
    @ColumnInfo(name = "recycle_bin_entity_id") val entityId: String,
    @ColumnInfo(name = "recycle_bin_deleted_at") val deletedAt: Long,
    @ColumnInfo(name = "recycle_bin_expire_at") val expireAt: Long,
    @Embedded val transaction: TransactionEntity?,
    @ColumnInfo(name = "account_name") val accountName: String?,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @Embedded(prefix = "joined_category_") val category: CategoryEntity?,
    @Embedded(prefix = "joined_account_") val account: AccountEntity?,
    @Embedded(prefix = "joined_budget_") val budget: BudgetEntity?,
    @ColumnInfo(name = "budget_category_name") val budgetCategoryName: String?
)
