package com.hitunguang.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.feature.settings.data.repository.SettingsRepositoryImpl
import com.hitunguang.feature.settings.domain.model.AppSettings
import com.hitunguang.feature.settings.domain.model.BackupSettings
import com.hitunguang.feature.settings.domain.model.NotificationSettings
import com.hitunguang.feature.settings.domain.model.SecuritySettings
import com.hitunguang.feature.transaction.domain.model.TransactionDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsDataStoreTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var settingsRepository: SettingsRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val datastoreFile = File(tmpFolder.newFolder(), "test_preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { datastoreFile }
        )
        settingsDataStore = SettingsDataStore(dataStore)
        settingsRepository = SettingsRepositoryImpl(settingsDataStore)
    }

    @Test
    fun testAppSettingsSaveAndRead() = runBlocking {
        val original = AppSettings(
            id = "app-1",
            themeMode = "DARK",
            hideBalance = true,
            receiptAutoDeleteDays = 15,
            dashboardPeriod = "WEEKLY",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        settingsRepository.saveAppSettings(original)

        val read = settingsRepository.getAppSettings().first()
        assertNotNull(read)
        assertEquals(original.themeMode, read?.themeMode)
        assertEquals(original.hideBalance, read?.hideBalance)
        assertEquals(original.receiptAutoDeleteDays, read?.receiptAutoDeleteDays)
        assertEquals(original.dashboardPeriod, read?.dashboardPeriod)
    }

    @Test
    fun testBackupSettingsSaveAndRead() = runBlocking {
        val original = BackupSettings(
            id = "backup-1",
            backupFolderUri = "content://com.android.externalstorage.documents/tree/primary%3ABackup",
            backupFrequency = "DAILY",
            autoBackupEnabled = true,
            lastBackupAt = 12345L,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        settingsRepository.saveBackupSettings(original)

        val read = settingsRepository.getBackupSettings().first()
        assertNotNull(read)
        assertEquals(original.backupFolderUri, read?.backupFolderUri)
        assertEquals(original.backupFrequency, read?.backupFrequency)
        assertEquals(original.autoBackupEnabled, read?.autoBackupEnabled)
        assertEquals(original.lastBackupAt, read?.lastBackupAt)
    }

    @Test
    fun testNotificationSettingsSaveAndRead() = runBlocking {
        val original = NotificationSettings(
            id = "notif-1",
            dailyReminderEnabled = true,
            dailyReminderTime = "08:00",
            weeklyReviewEnabled = true,
            monthlyReviewEnabled = false,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        settingsRepository.saveNotificationSettings(original)

        val read = settingsRepository.getNotificationSettings().first()
        assertNotNull(read)
        assertEquals(original.dailyReminderEnabled, read?.dailyReminderEnabled)
        assertEquals(original.dailyReminderTime, read?.dailyReminderTime)
        assertEquals(original.weeklyReviewEnabled, read?.weeklyReviewEnabled)
        assertEquals(original.monthlyReviewEnabled, read?.monthlyReviewEnabled)
    }

    @Test
    fun testSecuritySettingsSaveAndRead() = runBlocking {
        val original = SecuritySettings(
            id = "sec-1",
            pinHash = "dummyhash123",
            biometricEnabled = true,
            recoveryCodeHash = "dummyrecovery123",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        settingsRepository.saveSecuritySettings(original)

        val read = settingsRepository.getSecuritySettings().first()
        assertNotNull(read)
        assertEquals(original.pinHash, read?.pinHash)
        assertEquals(original.biometricEnabled, read?.biometricEnabled)
        assertEquals(original.recoveryCodeHash, read?.recoveryCodeHash)
    }

    @Test
    fun testTransactionDraftSaveAndRead() = runBlocking {
        val draft = TransactionDraft(
            transactionType = "EXPENSE",
            accountId = "acc-1",
            categoryId = "cat-1",
            title = "Lunch",
            note = "Burger",
            amount = 12000L,
            updatedAt = 5000L
        )

        settingsDataStore.saveTransactionDraft(draft)

        val read = settingsDataStore.transactionDraft.first()
        assertNotNull(read)
        assertEquals("Lunch", read?.title)
        assertEquals(12000L, read?.amount)

        settingsDataStore.clearTransactionDraft()
        val cleared = settingsDataStore.transactionDraft.first()
        assertNull(cleared)
    }
}
