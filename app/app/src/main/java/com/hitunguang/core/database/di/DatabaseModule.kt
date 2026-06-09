package com.hitunguang.core.database.di

import android.content.Context
import androidx.room.Room
import com.hitunguang.core.database.HitungUangDatabase
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
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_receipt_id` ON `transactions` (`receipt_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_start_date` ON `budgets` (`start_date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recycle_bin_entity_id` ON `recycle_bin` (`entity_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recycle_bin_deleted_at` ON `recycle_bin` (`deleted_at`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recycle_bin_expire_at` ON `recycle_bin` (`expire_at`)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `categories` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `categories` ADD COLUMN `deleted_at` INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `accounts` ADD COLUMN `deleted_at` INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE `budgets` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `budgets` ADD COLUMN `deleted_at` INTEGER DEFAULT NULL")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): HitungUangDatabase {
        return Room.databaseBuilder(
            context,
            HitungUangDatabase::class.java,
            "hitunguang.db"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                val defaultCategories = listOf(
                    Triple("default_expense_makanan", "Makanan", "EXPENSE" to "restaurant"),
                    Triple("default_expense_transportasi", "Transportasi", "EXPENSE" to "directions_car"),
                    Triple("default_expense_belanja", "Belanja", "EXPENSE" to "shopping_cart"),
                    Triple("default_expense_hiburan", "Hiburan", "EXPENSE" to "movie"),
                    Triple("default_expense_tagihan", "Tagihan", "EXPENSE" to "receipt"),
                    Triple("default_expense_lain_lain", "Lain-lain", "EXPENSE" to "category"),
                    
                    Triple("default_income_gaji", "Gaji", "INCOME" to "payments"),
                    Triple("default_income_investasi", "Investasi", "INCOME" to "trending_up"),
                    Triple("default_income_bonus", "Bonus", "INCOME" to "redeem"),
                    Triple("default_income_lain_lain", "Lain-lain", "INCOME" to "category")
                )
                for (cat in defaultCategories) {
                    db.execSQL(
                        "INSERT INTO categories (id, name, category_type, icon, is_default, is_pinned, created_at, updated_at, is_deleted, deleted_at) " +
                        "VALUES ('${cat.first}', '${cat.second}', '${cat.third.first}', '${cat.third.second}', 1, 0, $now, $now, 0, NULL)"
                    )
                }
            }
        })
        .build()
    }

    @Provides
    fun provideUserProfileDao(database: HitungUangDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    fun provideAccountDao(database: HitungUangDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: HitungUangDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: HitungUangDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideTransferDao(database: HitungUangDatabase): TransferDao = database.transferDao()

    @Provides
    fun provideBudgetDao(database: HitungUangDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideReceiptDao(database: HitungUangDatabase): ReceiptDao = database.receiptDao()

    @Provides
    fun provideSettingsDao(database: HitungUangDatabase): SettingsDao = database.settingsDao()

    @Provides
    fun provideRecycleBinDao(database: HitungUangDatabase): RecycleBinDao = database.recycleBinDao()

    @Provides
    fun provideAttachmentDao(database: HitungUangDatabase): AttachmentDao = database.attachmentDao()
}
