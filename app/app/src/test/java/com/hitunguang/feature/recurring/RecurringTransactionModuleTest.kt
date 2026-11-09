package com.hitunguang.feature.recurring

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.database.entity.TransactionEntity
import com.hitunguang.core.filemanager.AttachmentFileManager
import com.hitunguang.core.notification.NotificationHelper
import com.hitunguang.core.notification.RecurringFailureStore
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.account.domain.usecase.DeleteAccountUseCase
import com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl
import com.hitunguang.feature.budget.domain.usecase.CheckBudgetThresholdUseCase
import com.hitunguang.feature.category.data.repository.CategoryRepositoryImpl
import com.hitunguang.feature.recurring.data.repository.RecurringTransactionRepositoryImpl
import com.hitunguang.feature.recurring.domain.model.RecurrenceRule
import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.domain.usecase.CreateRecurringTransactionUseCase
import com.hitunguang.feature.recurring.domain.usecase.ProcessDueRecurringUseCase
import com.hitunguang.feature.recurring.domain.usecase.SetRecurringTransactionActiveUseCase
import com.hitunguang.feature.recurring.domain.util.RecurrenceSchedule
import com.hitunguang.feature.recyclebin.data.repository.RecycleBinRepositoryImpl
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.usecase.CreateTransactionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class RecurringTransactionModuleTest {

    private lateinit var context: Context
    private lateinit var db: HitungUangDatabase
    private lateinit var repository: RecurringTransactionRepositoryImpl
    private lateinit var createRecurring: CreateRecurringTransactionUseCase
    private lateinit var setActive: SetRecurringTransactionActiveUseCase
    private lateinit var processDue: ProcessDueRecurringUseCase

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 9)
    private val now = RecurrenceSchedule.runAt(today, zone) + 60_000L

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        context.getSharedPreferences("recurring_failures", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = RecurringTransactionRepositoryImpl(db, db.recurringTransactionDao())
        createRecurring = CreateRecurringTransactionUseCase(repository)
        setActive = SetRecurringTransactionActiveUseCase(repository)
        val transactionRepository = TransactionRepositoryImpl(db.transactionDao())
        val budgetRepository = BudgetRepositoryImpl(db.budgetDao())
        processDue = ProcessDueRecurringUseCase(
            repository = repository,
            accountRepository = AccountRepositoryImpl(db.accountDao()),
            categoryRepository = CategoryRepositoryImpl(db.categoryDao()),
            createTransactionUseCase = CreateTransactionUseCase(
                transactionRepository,
                db.accountDao(),
                CheckBudgetThresholdUseCase(budgetRepository, context)
            ),
            failureStore = RecurringFailureStore(context),
            notificationHelper = NotificationHelper(context)
        )

        db.accountDao().insertAccount(AccountEntity("acc-1", "Dompet Tunai", "CASH", null, 100_000L, 100_000L, 1000L, 1000L))
        db.accountDao().insertAccount(AccountEntity("acc-2", "Tabungan Bank", "BANK", null, 0L, 0L, 1000L, 1000L))
        db.categoryDao().insertCategory(
            CategoryEntity("cat-1", "Tagihan", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun rule(
        type: String = "EXPENSE",
        amount: Long = 10_000L,
        recurrence: RecurrenceRule = RecurrenceRule.Daily,
        start: LocalDate = today,
        accountId: String = "acc-1"
    ) = RecurringTransaction(
        id = "rec-1",
        accountId = accountId,
        categoryId = "cat-1",
        transactionType = type,
        title = "Bayar kos",
        note = "Kamar 3",
        amount = amount,
        recurrenceRule = recurrence,
        nextRunAt = RecurrenceSchedule.firstRunAt(recurrence, start, zone),
        isActive = true,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private fun oldTransaction() = TransactionEntity(
        id = "tx-1",
        accountId = "acc-1",
        categoryId = "cat-1",
        receiptId = null,
        transactionType = "EXPENSE",
        title = "Lama",
        note = null,
        amount = 1_000L,
        transactionDate = 1000L,
        isDeleted = false,
        deletedAt = null,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private fun recycleBin() = RecycleBinRepositoryImpl(
        db, db.recycleBinDao(), db.transactionDao(), db.attachmentDao(), AttachmentFileManager(context)
    )

    private suspend fun transactions() = db.transactionDao().getAllTransactions().first()
    private suspend fun balance(id: String = "acc-1") = db.accountDao().getAccountBalance(id)
    private suspend fun nextRunAt() = repository.getById("rec-1")!!.nextRunAt
    private fun notificationCount() =
        shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).allNotifications.size

    @Test
    fun catchUpPostsEveryMissedOccurrenceAndAdvances() = runBlocking {
        createRecurring(rule(start = today.minusDays(4)))

        val posted = withTimeout(10_000) { processDue(now) }

        assertEquals(5, posted)
        val txs = transactions()
        assertEquals(5, txs.size)
        assertEquals(
            (4 downTo 0).map { RecurrenceSchedule.runAt(today.minusDays(it.toLong()), zone) }.toSet(),
            txs.map { it.transactionDate }.toSet()
        )
        assertTrue(txs.all { it.title == "Bayar kos" && it.note == "Kamar 3" && it.categoryId == "cat-1" })
        assertEquals(50_000L, balance())
        assertEquals(RecurrenceSchedule.runAt(today.plusDays(1), zone), nextRunAt())
    }

    @Test
    fun runningTwiceDoesNotDoublePost() = runBlocking {
        createRecurring(rule(type = "INCOME", recurrence = RecurrenceRule.Monthly(9), start = today.minusMonths(2)))

        assertEquals(3, processDue(now))
        assertEquals(0, processDue(now))

        assertEquals(3, transactions().size)
        assertEquals(130_000L, balance())
        assertEquals(RecurrenceSchedule.runAt(LocalDate.of(2026, 12, 9), zone), nextRunAt())
    }

    @Test
    fun notDueYetPostsNothing() = runBlocking {
        createRecurring(rule(start = today.plusDays(1)))
        assertEquals(0, processDue(now))
        assertTrue(transactions().isEmpty())
    }

    @Test
    fun catchUpIsCappedPerRun() = runBlocking {
        createRecurring(rule(type = "INCOME", start = today.minusDays(99)))

        assertEquals(ProcessDueRecurringUseCase.MAX_OCCURRENCES_PER_RUN, processDue(now))
        assertEquals(RecurrenceSchedule.runAt(today.minusDays(39), zone), nextRunAt())
        assertEquals(40, processDue(now))
        assertEquals(100, transactions().size)
    }

    @Test
    fun insufficientBalanceStopsKeepsNextRunAndNotifiesOnce() = runBlocking {
        // 100.000 covers two 40.000 occurrences; the third fails
        createRecurring(rule(amount = 40_000L, start = today.minusDays(3)))

        assertEquals(2, processDue(now))
        val failedAt = RecurrenceSchedule.runAt(today.minusDays(1), zone)
        assertEquals(failedAt, nextRunAt())
        assertEquals(20_000L, balance())
        assertEquals(1, notificationCount())

        // Same failure on the next run: nothing posted, no second notification
        assertEquals(0, processDue(now))
        assertEquals(failedAt, nextRunAt())
        assertEquals(1, notificationCount())

        // Topping up lets the stuck occurrence through
        db.accountDao().adjustBalance("acc-1", 100_000L, now)
        assertEquals(2, processDue(now))
        assertEquals(4, transactions().size)
    }

    @Test
    fun deletedWalletFailsWithoutPosting() = runBlocking {
        createRecurring(rule())
        db.accountDao().softDeleteAccount("acc-1", now)

        assertEquals(0, processDue(now))
        assertTrue(transactions().isEmpty())
        assertEquals(RecurrenceSchedule.runAt(today, zone), nextRunAt())
        assertEquals(1, notificationCount())
    }

    @Test
    fun pausedRuleIsSkippedAndResumeDoesNotBackPost() = runBlocking {
        createRecurring(rule(type = "INCOME", start = today.minusDays(5)))
        setActive(repository.getById("rec-1")!!, active = false, now = now)
        assertEquals(0, processDue(now))

        setActive(repository.getById("rec-1")!!, active = true, now = now)
        assertEquals(RecurrenceSchedule.runAt(today, zone), nextRunAt())
        assertEquals(1, processDue(now))
    }

    @Test
    fun categoryHardDeleteKeepsRuleUncategorized() = runBlocking {
        createRecurring(rule())
        db.transactionDao().insertTransaction(oldTransaction())

        recycleBin().permanentDeleteItem("cat-1", "CATEGORY")

        assertNull(db.categoryDao().getCategoryByIdDirect("cat-1"))
        val kept = repository.getById("rec-1")
        assertNotNull(kept)
        assertNull(kept!!.categoryId)
    }

    @Test
    fun walletDeleteMovesRulesToReplacement() = runBlocking {
        createRecurring(rule())
        db.transactionDao().insertTransaction(oldTransaction())
        val accountRepository = AccountRepositoryImpl(db.accountDao())
        val account = accountRepository.getAccountById("acc-1").first()!!

        DeleteAccountUseCase(accountRepository)(account, replacementAccountId = "acc-2")

        val moved = repository.getById("rec-1")!!
        assertEquals("acc-2", moved.accountId)
        assertTrue(moved.isActive)
    }

    @Test
    fun walletWithoutHistoryDeactivatesRulesAndCanBePermanentlyDeleted() = runBlocking {
        createRecurring(rule(accountId = "acc-2"))
        val accountRepository = AccountRepositoryImpl(db.accountDao())
        val account = accountRepository.getAccountById("acc-2").first()!!

        DeleteAccountUseCase(accountRepository)(account)
        assertFalse(repository.getById("rec-1")!!.isActive)

        recycleBin().permanentDeleteItem("acc-2", "WALLET")

        assertNull(repository.getById("rec-1"))
        assertEquals(0, db.query("SELECT * FROM accounts WHERE id = 'acc-2'", null).use { it.count })
    }
}
