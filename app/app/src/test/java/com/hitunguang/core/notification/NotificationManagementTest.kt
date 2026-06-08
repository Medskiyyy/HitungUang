package com.hitunguang.core.notification

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.core.database.entity.CategoryEntity
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.core.notification.worker.BudgetWorker
import com.hitunguang.core.notification.worker.ReminderWorker
import com.hitunguang.core.notification.worker.ReviewWorker
import com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.feature.budget.domain.usecase.CheckBudgetThresholdUseCase
import com.hitunguang.feature.category.data.repository.CategoryRepositoryImpl
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.settings.data.repository.SettingsRepositoryImpl
import com.hitunguang.feature.settings.domain.model.NotificationSettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import com.hitunguang.feature.settings.presentation.NotificationViewModel
import com.hitunguang.feature.transaction.data.repository.TransactionRepositoryImpl
import com.hitunguang.feature.transaction.domain.model.Transaction
import com.hitunguang.feature.transaction.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

class TestApplication : Application(),
    dagger.hilt.internal.GeneratedComponent,
    BudgetWorker.BudgetWorkerEntryPoint,
    ReminderWorker.ReminderWorkerEntryPoint,
    ReviewWorker.ReviewWorkerEntryPoint {

    companion object {
        lateinit var realNotificationHelper: NotificationHelper
        lateinit var realBudgetRepository: BudgetRepository
        lateinit var realTransactionRepository: TransactionRepository
        lateinit var realCategoryRepository: CategoryRepository
        lateinit var realSettingsDataStore: SettingsDataStore
        lateinit var realNotificationScheduler: NotificationScheduler
    }

    override fun notificationHelper() = realNotificationHelper
    override fun budgetRepository() = realBudgetRepository
    override fun transactionRepository() = realTransactionRepository
    override fun categoryRepository() = realCategoryRepository
    override fun settingsDataStore() = realSettingsDataStore
    override fun notificationScheduler() = realNotificationScheduler
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class NotificationManagementTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var db: HitungUangDatabase
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var checkBudgetThresholdUseCase: CheckBudgetThresholdUseCase
    private lateinit var viewModel: NotificationViewModel

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext<Context>()

        // Initialize WorkManager Test helper
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        // Initialize Database
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        budgetRepository = BudgetRepositoryImpl(db.budgetDao())
        transactionRepository = TransactionRepositoryImpl(db.transactionDao())
        categoryRepository = CategoryRepositoryImpl(db.categoryDao())

        // Initialize Settings DataStore
        val datastoreFile = File(tmpFolder.newFolder(), "test_settings_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { datastoreFile }
        )
        settingsDataStore = SettingsDataStore(dataStore)
        settingsRepository = SettingsRepositoryImpl(settingsDataStore)

        // Initialize Notification Helper & Scheduler
        notificationHelper = NotificationHelper(context)
        notificationScheduler = NotificationScheduler(context)

        // Setup usecases
        checkBudgetThresholdUseCase = CheckBudgetThresholdUseCase(budgetRepository, context)

        // Inject objects into static Application context for Workers
        TestApplication.realNotificationHelper = notificationHelper
        TestApplication.realBudgetRepository = budgetRepository
        TestApplication.realTransactionRepository = transactionRepository
        TestApplication.realCategoryRepository = categoryRepository
        TestApplication.realSettingsDataStore = settingsDataStore
        TestApplication.realNotificationScheduler = notificationScheduler

        // Grant notification permission for tests
        Shadows.shadowOf(context as Application).grantPermissions(
            android.Manifest.permission.POST_NOTIFICATIONS
        )

        // Set up default category and account
        db.accountDao().insertAccount(
            AccountEntity("acc-1", "Dompet Utama", "CASH", null, 1000000L, 1000000L, 1000L, 1000L)
        )
        db.categoryDao().insertCategory(
            CategoryEntity("cat-1", "Makanan", "EXPENSE", null, isDefault = false, isPinned = false, createdAt = 1000L, updatedAt = 1000L)
        )

        // Save initial default settings to DataStore so ViewModel is immediately initialized
        val defaultSettings = NotificationSettings(
            id = "default",
            dailyReminderEnabled = false,
            dailyReminderTime = "20:00",
            weeklyReviewEnabled = false,
            monthlyReviewEnabled = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        settingsRepository.saveNotificationSettings(defaultSettings)

        // Initialize ViewModel
        viewModel = NotificationViewModel(settingsRepository, notificationScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun idleMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun testScheduleUpdatesOnSettingsMutation() = runBlocking {
        val workManager = WorkManager.getInstance(context)

        // Wait for ViewModel initialization
        delay(100)
        idleMainLooper()

        // 1. Toggle daily reminder
        viewModel.toggleDailyReminder(true)
        delay(100)
        idleMainLooper()

        viewModel.updateDailyReminderTime("19:30")
        delay(100)
        idleMainLooper()

        var settings = settingsRepository.getNotificationSettings().first()
        assertNotNull(settings)
        assertTrue(settings!!.dailyReminderEnabled)
        assertEquals("19:30", settings.dailyReminderTime)

        var workInfos = workManager.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_REMINDER).get()
        assertFalse(workInfos.isEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, workInfos[0].state)

        // 2. Disable daily reminder
        viewModel.toggleDailyReminder(false)
        delay(100)
        idleMainLooper()

        settings = settingsRepository.getNotificationSettings().first()
        assertFalse(settings!!.dailyReminderEnabled)

        workInfos = workManager.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_REMINDER).get()
        assertTrue(workInfos.isEmpty() || workInfos[0].state == WorkInfo.State.CANCELLED)

        // 3. Weekly & Monthly review reviews
        viewModel.toggleWeeklyReview(true)
        delay(100)
        idleMainLooper()

        viewModel.toggleMonthlyReview(true)
        delay(100)
        idleMainLooper()

        settings = settingsRepository.getNotificationSettings().first()
        assertTrue(settings!!.weeklyReviewEnabled)
        assertTrue(settings.monthlyReviewEnabled)

        val weeklyInfos = workManager.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_WEEKLY).get()
        assertFalse(weeklyInfos.isEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, weeklyInfos[0].state)

        val monthlyInfos = workManager.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_MONTHLY).get()
        assertFalse(monthlyInfos.isEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, monthlyInfos[0].state)
    }

    @Test
    fun testBudgetWorkerThresholdAndLimitTriggers() = runBlocking {
        // Setup budget: limit 100,000, threshold 80% (80,000), duration from now-1h to now+1h
        val now = System.currentTimeMillis()
        val budget = Budget(
            id = "budget-1",
            categoryId = "cat-1",
            budgetType = "CATEGORY",
            amount = 100000L,
            thresholdPercent = 80,
            startDate = now - 3600000L,
            endDate = now + 3600000L,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        budgetRepository.insertBudget(budget)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = Shadows.shadowOf(notificationManager)

        // Clear existing notifications
        notificationManager.cancelAll()

        // 1. Transaction total under threshold: 50,000
        val tx1 = Transaction(
            id = "tx-1",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Makan Siang",
            note = null,
            amount = 50000L,
            transactionDate = now,
            isDeleted = false,
            deletedAt = null,
            createdAt = now,
            updatedAt = now
        )
        transactionRepository.insertTransaction(tx1, -50000L)

        // Trigger budget check
        checkBudgetThresholdUseCase("cat-1")

        // Execute budget worker
        val worker = TestListenableWorkerBuilder<BudgetWorker>(context)
            .setInputData(workDataOf("budget_id" to "budget-1"))
            .build()

        val result1 = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result1)

        // No notification should be enqueued becausespent is 50,000 (under 80,000 threshold)
        assertTrue(shadowNotificationManager.allNotifications.isEmpty())

        // 2. Transaction total crosses 80% threshold:spent 85,000
        val tx2 = Transaction(
            id = "tx-2",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Kopi Sore",
            note = null,
            amount = 35000L,
            transactionDate = now,
            isDeleted = false,
            deletedAt = null,
            createdAt = now,
            updatedAt = now
        )
        transactionRepository.insertTransaction(tx2, -35000L)

        val result2 = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result2)

        // Check if "Anggaran Hampir Habis!" notification is triggered
        val notifications = shadowNotificationManager.allNotifications
        assertEquals(1, notifications.size)
        assertEquals("Anggaran Hampir Habis!", Shadows.shadowOf(notifications[0]).contentTitle)

        // 3. Transaction total crosses 100% limit:spent 105,000
        notificationManager.cancelAll()
        val tx3 = Transaction(
            id = "tx-3",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Makan Malam",
            note = null,
            amount = 20000L,
            transactionDate = now,
            isDeleted = false,
            deletedAt = null,
            createdAt = now,
            updatedAt = now
        )
        transactionRepository.insertTransaction(tx3, -20000L)

        val result3 = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result3)

        // Check if "Anggaran Habis!" notification is triggered
        val limitNotifications = shadowNotificationManager.allNotifications
        assertEquals(1, limitNotifications.size)
        assertEquals("Anggaran Habis!", Shadows.shadowOf(limitNotifications[0]).contentTitle)
    }

    @Test
    fun testReminderWorkerTrigger() = runBlocking {
        // Enable daily reminder
        val defaultSettings = NotificationSettings(
            id = "default",
            dailyReminderEnabled = true,
            dailyReminderTime = "20:00",
            weeklyReviewEnabled = false,
            monthlyReviewEnabled = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        settingsRepository.saveNotificationSettings(defaultSettings)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = Shadows.shadowOf(notificationManager)
        notificationManager.cancelAll()

        val worker = TestListenableWorkerBuilder<ReminderWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, shadowNotificationManager.allNotifications.size)
        assertEquals("Catat Keuangan Anda", Shadows.shadowOf(shadowNotificationManager.allNotifications[0]).contentTitle)
    }

    @Test
    fun testReviewWorkerTrigger() = runBlocking {
        // Enable weekly review
        val defaultSettings = NotificationSettings(
            id = "default",
            dailyReminderEnabled = false,
            dailyReminderTime = "20:00",
            weeklyReviewEnabled = true,
            monthlyReviewEnabled = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        settingsRepository.saveNotificationSettings(defaultSettings)

        val now = System.currentTimeMillis()
        val tx = Transaction(
            id = "tx-weekly",
            accountId = "acc-1",
            categoryId = "cat-1",
            receiptId = null,
            transactionType = "EXPENSE",
            title = "Belanja Mingguan",
            note = null,
            amount = 150000L,
            transactionDate = now - 10000L, // inside last 7 days
            isDeleted = false,
            deletedAt = null,
            createdAt = now,
            updatedAt = now
        )
        transactionRepository.insertTransaction(tx, -150000L)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = Shadows.shadowOf(notificationManager)
        notificationManager.cancelAll()

        // Run Weekly Review Worker
        val weeklyWorker = TestListenableWorkerBuilder<ReviewWorker>(context)
            .setTags(listOf("weekly_review"))
            .build()
        val weeklyResult = weeklyWorker.doWork()

        assertEquals(ListenableWorker.Result.success(), weeklyResult)
        assertEquals(1, shadowNotificationManager.allNotifications.size)
        val weeklyNotif = shadowNotificationManager.allNotifications[0]
        assertEquals("Review Keuangan Mingguan", Shadows.shadowOf(weeklyNotif).contentTitle)
        assertTrue(Shadows.shadowOf(weeklyNotif).contentText.toString().contains("150000"))

        // Run Monthly Review Worker
        notificationManager.cancelAll()
        val monthlyWorker = TestListenableWorkerBuilder<ReviewWorker>(context)
            .setTags(listOf("monthly_review"))
            .build()
        val monthlyResult = monthlyWorker.doWork()

        assertEquals(ListenableWorker.Result.success(), monthlyResult)
        assertEquals(1, shadowNotificationManager.allNotifications.size)
        val monthlyNotif = shadowNotificationManager.allNotifications[0]
        assertEquals("Review Keuangan Bulanan", Shadows.shadowOf(monthlyNotif).contentTitle)
        assertTrue(Shadows.shadowOf(monthlyNotif).contentText.toString().contains("150000"))
    }
}
