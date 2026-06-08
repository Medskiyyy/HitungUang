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
        .addMigrations(MIGRATION_1_2)
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
