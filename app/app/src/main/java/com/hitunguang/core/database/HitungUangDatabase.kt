package com.hitunguang.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hitunguang.core.database.dao.AccountDao
import com.hitunguang.core.database.dao.AttachmentDao
import com.hitunguang.core.database.dao.BudgetDao
import com.hitunguang.core.database.dao.CategoryDao
import com.hitunguang.core.database.dao.ReceiptDao
import com.hitunguang.core.database.dao.RecycleBinDao
import com.hitunguang.core.database.dao.SettingsDao
import com.hitunguang.core.database.dao.TransactionDao
import com.hitunguang.core.database.dao.TransferDao
import com.hitunguang.core.database.dao.UserProfileDao
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.AppSettingEntity
import com.hitunguang.core.database.entity.AttachmentEntity
import com.hitunguang.core.database.entity.BackupSettingEntity
import com.hitunguang.core.database.entity.BudgetEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.database.entity.NotificationSettingEntity
import com.hitunguang.core.database.entity.ReceiptEntity
import com.hitunguang.core.database.entity.ReceiptItemEntity
import com.hitunguang.core.database.entity.RecycleBinEntity
import com.hitunguang.core.database.entity.RecurringTransactionEntity
import com.hitunguang.core.database.entity.SecuritySettingEntity
import com.hitunguang.core.database.entity.TransactionDraftEntity
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.core.database.entity.TransactionSearchEntity
import com.hitunguang.core.database.entity.TransferEntity
import com.hitunguang.core.database.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransferEntity::class,
        AttachmentEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        TransactionDraftEntity::class,
        SecuritySettingEntity::class,
        NotificationSettingEntity::class,
        BackupSettingEntity::class,
        AppSettingEntity::class,
        RecycleBinEntity::class,
        TransactionSearchEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HitungUangDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun budgetDao(): BudgetDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun settingsDao(): SettingsDao
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun attachmentDao(): AttachmentDao
}
