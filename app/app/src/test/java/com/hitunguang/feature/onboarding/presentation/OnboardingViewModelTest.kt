package com.hitunguang.feature.onboarding.presentation

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.budget.data.repository.BudgetRepositoryImpl
import com.hitunguang.feature.budget.domain.repository.BudgetRepository
import com.hitunguang.core.security.PinHasher
import com.hitunguang.feature.onboarding.data.repository.UserProfileRepositoryImpl
import com.hitunguang.feature.onboarding.domain.repository.UserProfileRepository
import com.hitunguang.feature.settings.data.repository.SettingsRepositoryImpl
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var db: HitungUangDatabase
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var settingsDataStore: SettingsDataStore

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Setup in-memory DB
        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        userProfileRepository = UserProfileRepositoryImpl(db.userProfileDao())
        accountRepository = AccountRepositoryImpl(db.accountDao())
        budgetRepository = BudgetRepositoryImpl(db.budgetDao())

        // Setup DataStore
        val datastoreFile = File(tmpFolder.newFolder(), "test_onboarding_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { datastoreFile }
        )
        settingsDataStore = SettingsDataStore(dataStore)
        settingsRepository = SettingsRepositoryImpl(settingsDataStore)

        viewModel = OnboardingViewModel(
            context = context,
            userProfileRepository = userProfileRepository,
            accountRepository = accountRepository,
            budgetRepository = budgetRepository,
            settingsRepository = settingsRepository,
            database = db
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testOnboardingStepsAndValidation() = runBlocking {
        // Starts at WELCOME
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.currentStep)

        // WELCOME -> PROFILE
        viewModel.nextStep()
        assertEquals(OnboardingStep.PROFILE, viewModel.uiState.value.currentStep)

        // Try to proceed with empty name (should trigger error)
        viewModel.nextStep()
        assertEquals(OnboardingStep.PROFILE, viewModel.uiState.value.currentStep)
        assertNotNull(viewModel.uiState.value.nameError)

        // Set name, proceed to ACCOUNT
        viewModel.updateName("Ahmad")
        assertNull(viewModel.uiState.value.nameError)
        viewModel.nextStep()
        assertEquals(OnboardingStep.ACCOUNT, viewModel.uiState.value.currentStep)

        // ACCOUNT -> BUDGET
        viewModel.nextStep()
        assertEquals(OnboardingStep.BUDGET, viewModel.uiState.value.currentStep)

        // BUDGET -> SECURITY (NOTIFICATION and BACKUP were removed from onboarding)
        viewModel.nextStep()
        assertEquals(OnboardingStep.SECURITY, viewModel.uiState.value.currentStep)

        // Set PIN enabled but invalid PIN
        viewModel.setPinEnabled(true)
        viewModel.updatePin("12")
        viewModel.nextStep()
        assertEquals(OnboardingStep.SECURITY, viewModel.uiState.value.currentStep)
        assertNotNull(viewModel.uiState.value.pinError)

        // Enter valid PIN but non-matching confirmation PIN
        viewModel.updatePin("1234")
        viewModel.updateConfirmPin("1235")
        viewModel.nextStep()
        assertEquals(OnboardingStep.SECURITY, viewModel.uiState.value.currentStep)
        assertNotNull(viewModel.uiState.value.confirmPinError)

        // Match PINs, proceed to TUTORIAL
        viewModel.updateConfirmPin("1234")
        viewModel.nextStep()
        assertEquals(OnboardingStep.TUTORIAL, viewModel.uiState.value.currentStep)

        // TUTORIAL -> COMPLETE
        viewModel.nextStep()
        
        // Wait for asynchronous completeOnboarding coroutine to finish
        kotlinx.coroutines.delay(500)
        
        // 1. User Profile saved
        val profile = userProfileRepository.getUserProfile().first()
        assertNotNull(profile)
        assertEquals("Ahmad", profile?.name)

        // 2. Default accounts saved
        val savedAccounts = accountRepository.getAllAccounts().first()
        assertEquals(1, savedAccounts.size)
        assertEquals("Dompet Utama", savedAccounts[0].name)

        // 3. Security Settings saved
        val secSettings = settingsRepository.getSecuritySettings().first()
        assertNotNull(secSettings)
        assertTrue(secSettings?.pinHash?.isNotEmpty() == true)
        assertTrue(secSettings?.recoveryCodeHash?.isNotEmpty() == true)
    }

    @Test
    fun testOnboardingPreviousStep() = runBlocking {
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.currentStep)
        viewModel.nextStep() // To PROFILE
        assertEquals(OnboardingStep.PROFILE, viewModel.uiState.value.currentStep)
        viewModel.previousStep() // Back to WELCOME
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.currentStep)
    }

    private fun countRows(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
            it.moveToFirst()
            it.getInt(0)
        }

    private suspend fun goToTutorial(pin: String? = null, budget: String = "") {
        viewModel.nextStep()
        viewModel.updateName("Ahmad")
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.updateBudgetAmount(budget)
        viewModel.nextStep()
        if (pin != null) {
            viewModel.setPinEnabled(true)
            viewModel.updatePin(pin)
            viewModel.updateConfirmPin(pin)
        }
        viewModel.nextStep()
        assertEquals(OnboardingStep.TUTORIAL, viewModel.uiState.value.currentStep)
    }

    private suspend fun awaitProfile() {
        val start = System.currentTimeMillis()
        while (userProfileRepository.getUserProfile().first() == null) {
            if (System.currentTimeMillis() - start > 10_000) throw AssertionError("Profile not saved")
            kotlinx.coroutines.delay(20)
        }
    }

    @Test
    fun testDoubleTapFinishCreatesSingleProfileAndAccount() = runBlocking {
        goToTutorial(pin = "123456", budget = "500000")
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep()
        awaitProfile()
        kotlinx.coroutines.delay(300)

        assertEquals(1, countRows("user_profiles"))
        assertEquals(1, countRows("accounts"))
        assertEquals(1, countRows("budgets"))
        val sec = settingsRepository.getSecuritySettings().first()!!
        assertEquals(6, sec.pinLength)
        assertTrue(PinHasher.verify("123456", sec.pinHash!!))
        assertFalse(PinHasher.isLegacy(sec.pinHash!!))
    }

    @Test
    fun testNonDigitPinRejected() = runBlocking {
        viewModel.nextStep()
        viewModel.updateName("Ahmad")
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.setPinEnabled(true)
        viewModel.updatePin("12ab")
        viewModel.updateConfirmPin("12ab")
        viewModel.nextStep()
        assertEquals(OnboardingStep.SECURITY, viewModel.uiState.value.currentStep)
        assertNotNull(viewModel.uiState.value.pinError)
    }

    @Test
    fun testOnboardingBudgetCoversCurrentCalendarMonth() = runBlocking {
        goToTutorial(budget = "1000000")
        viewModel.nextStep()
        awaitProfile()

        val budget = budgetRepository.getAllBudgets().first().single()
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(budget.startDate).atZone(zone)
        val end = Instant.ofEpochMilli(budget.endDate).atZone(zone)
        val today = LocalDate.now(zone)
        assertEquals(today.withDayOfMonth(1), start.toLocalDate())
        assertEquals(0, start.toLocalTime().toNanoOfDay())
        assertEquals(today.withDayOfMonth(today.lengthOfMonth()), end.toLocalDate())
        assertEquals(java.time.LocalTime.of(23, 59, 59, 999_000_000), end.toLocalTime())
    }

    @Test
    fun testMonthRangeMillisHandlesLeapFebruary() {
        val zone = ZoneId.of("Asia/Jakarta")
        val (start, end) = monthRangeMillis(LocalDate.of(2028, 2, 17), zone)
        assertEquals(LocalDate.of(2028, 2, 1).atStartOfDay(zone).toInstant().toEpochMilli(), start)
        assertEquals(LocalDate.of(2028, 3, 1).atStartOfDay(zone).toInstant().toEpochMilli() - 1, end)
        assertEquals(LocalDate.of(2028, 2, 29), Instant.ofEpochMilli(end).atZone(zone).toLocalDate())
    }
}
