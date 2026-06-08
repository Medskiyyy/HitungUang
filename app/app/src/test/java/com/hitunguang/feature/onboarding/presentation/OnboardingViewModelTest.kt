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
            settingsRepository = settingsRepository
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

        // BUDGET -> NOTIFICATION
        viewModel.nextStep()
        assertEquals(OnboardingStep.NOTIFICATION, viewModel.uiState.value.currentStep)

        // NOTIFICATION -> SECURITY
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

        // Match PINs, proceed to BACKUP
        viewModel.updateConfirmPin("1234")
        viewModel.nextStep()
        assertEquals(OnboardingStep.BACKUP, viewModel.uiState.value.currentStep)

        // BACKUP -> TUTORIAL
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
}
