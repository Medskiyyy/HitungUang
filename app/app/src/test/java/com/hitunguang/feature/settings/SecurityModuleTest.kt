package com.hitunguang.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.core.security.PinHasher
import com.hitunguang.feature.settings.data.repository.SettingsRepositoryImpl
import com.hitunguang.feature.settings.domain.model.SecuritySettings
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import com.hitunguang.feature.settings.domain.usecase.CheckSecurityStatusUseCase
import com.hitunguang.feature.settings.domain.usecase.DisablePinSecurityUseCase
import com.hitunguang.feature.settings.domain.usecase.SavePinUseCase
import com.hitunguang.feature.settings.domain.usecase.ValidatePinUseCase
import com.hitunguang.feature.settings.domain.usecase.ValidateRecoveryCodeUseCase
import com.hitunguang.feature.settings.presentation.PendingSecurityAction
import com.hitunguang.feature.settings.presentation.SecurityViewModel
import com.hitunguang.feature.settings.presentation.SetupStep
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
class SecurityModuleTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var checkSecurityStatusUseCase: CheckSecurityStatusUseCase
    private lateinit var validatePinUseCase: ValidatePinUseCase
    private lateinit var savePinUseCase: SavePinUseCase
    private lateinit var disablePinSecurityUseCase: DisablePinSecurityUseCase
    private lateinit var validateRecoveryCodeUseCase: ValidateRecoveryCodeUseCase

    private lateinit var viewModel: SecurityViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Setup DataStore
        val datastoreFile = File(tmpFolder.newFolder(), "test_settings_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { datastoreFile }
        )
        settingsDataStore = SettingsDataStore(dataStore)
        settingsRepository = SettingsRepositoryImpl(settingsDataStore)

        // Setup Use Cases
        checkSecurityStatusUseCase = CheckSecurityStatusUseCase(settingsRepository)
        validatePinUseCase = ValidatePinUseCase(settingsRepository)
        savePinUseCase = SavePinUseCase(settingsRepository)
        disablePinSecurityUseCase = DisablePinSecurityUseCase(settingsRepository)
        validateRecoveryCodeUseCase = ValidateRecoveryCodeUseCase(settingsRepository, disablePinSecurityUseCase)

        viewModel = SecurityViewModel(
            settingsRepository = settingsRepository,
            checkSecurityStatusUseCase = checkSecurityStatusUseCase,
            validatePinUseCase = validatePinUseCase,
            savePinUseCase = savePinUseCase,
            disablePinSecurityUseCase = disablePinSecurityUseCase,
            validateRecoveryCodeUseCase = validateRecoveryCodeUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPinHasher() {
        val pin = "123456"
        val hash = PinHasher.hash(pin)
        // SHA-256 hash length in hex is 64 characters
        assertEquals(64, hash.length)
        // Verify deterministic output
        assertEquals(hash, PinHasher.hash(pin))
    }

    @Test
    fun testSecurityUseCases() = runBlocking {
        // 1. Initial State: no PIN
        assertFalse(checkSecurityStatusUseCase().first())
        val initialSettings = settingsRepository.getSecuritySettings().first()
        assertNull(initialSettings?.pinHash)

        // 2. Save PIN
        val pin = "1234"
        val rawRecoveryCode = savePinUseCase(pin)
        
        // Check recovery code format: 16 characters uppercase alphanumeric
        assertEquals(16, rawRecoveryCode.length)
        assertEquals(rawRecoveryCode.uppercase(), rawRecoveryCode)

        // Check settings saved
        assertTrue(checkSecurityStatusUseCase().first())
        val savedSettings = settingsRepository.getSecuritySettings().first()
        assertNotNull(savedSettings)
        assertEquals(PinHasher.hash(pin), savedSettings?.pinHash)
        assertEquals(PinHasher.hash(rawRecoveryCode), savedSettings?.recoveryCodeHash)

        // 3. Validate PIN
        assertTrue(validatePinUseCase(pin))
        assertFalse(validatePinUseCase("5678"))

        // 4. Validate Recovery Code (valid)
        // This should validate and also disable PIN security
        assertTrue(validateRecoveryCodeUseCase(rawRecoveryCode))
        assertFalse(checkSecurityStatusUseCase().first()) // now disabled

        // 5. Save PIN again, then test Validate Recovery Code (invalid)
        val newRecovery = savePinUseCase("4321")
        assertFalse(validateRecoveryCodeUseCase("WRONGRECOVERY123"))
        assertTrue(checkSecurityStatusUseCase().first()) // still active

        // 6. Disable PIN Security Use Case directly
        disablePinSecurityUseCase()
        assertFalse(checkSecurityStatusUseCase().first())
        val disabledSettings = settingsRepository.getSecuritySettings().first()
        assertNull(disabledSettings?.pinHash)
        assertNull(disabledSettings?.recoveryCodeHash)
        assertFalse(disabledSettings?.biometricEnabled ?: true)
    }

    @Test
    fun testViewModelInitializationAndAppLock() = runBlocking {
        // App is unlocked when no security settings exist or PIN is null
        assertFalse(viewModel.uiState.value.isAppLocked)

        // Save a PIN
        savePinUseCase("123456")

        // Create a new ViewModel to simulate app launch (cold start) with PIN configured
        val newViewModel = SecurityViewModel(
            settingsRepository = settingsRepository,
            checkSecurityStatusUseCase = checkSecurityStatusUseCase,
            validatePinUseCase = validatePinUseCase,
            savePinUseCase = savePinUseCase,
            disablePinSecurityUseCase = disablePinSecurityUseCase,
            validateRecoveryCodeUseCase = validateRecoveryCodeUseCase
        )
        delay(100)

        // App should lock initially because PIN is configured
        assertTrue(newViewModel.uiState.value.isAppLocked)
    }

    @Test
    fun testViewModelPinEntryAndLockout() = runBlocking {
        savePinUseCase("1234")

        // Re-init VM to trigger initial locking
        val vm = SecurityViewModel(
            settingsRepository = settingsRepository,
            checkSecurityStatusUseCase = checkSecurityStatusUseCase,
            validatePinUseCase = validatePinUseCase,
            savePinUseCase = savePinUseCase,
            disablePinSecurityUseCase = disablePinSecurityUseCase,
            validateRecoveryCodeUseCase = validateRecoveryCodeUseCase
        )
        delay(100)
        assertTrue(vm.uiState.value.isAppLocked)
        assertEquals(5, vm.uiState.value.remainingAttempts)
        assertFalse(vm.uiState.value.isLockedOut)

        // Enter correct PIN (digits 1, 2, 3, 4)
        vm.onPinDigitEntered("1")
        vm.onPinDigitEntered("2")
        vm.onPinDigitEntered("3")
        vm.onPinDigitEntered("4") // Should auto-verify

        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)
        assertFalse(vm.uiState.value.isAppLocked)
        assertEquals(5, vm.uiState.value.remainingAttempts)

        // Relock by mimicking state or re-init
        val vm2 = SecurityViewModel(
            settingsRepository = settingsRepository,
            checkSecurityStatusUseCase = checkSecurityStatusUseCase,
            validatePinUseCase = validatePinUseCase,
            savePinUseCase = savePinUseCase,
            disablePinSecurityUseCase = disablePinSecurityUseCase,
            validateRecoveryCodeUseCase = validateRecoveryCodeUseCase
        )
        delay(100)
        assertTrue(vm2.uiState.value.isAppLocked)

        // Enter wrong digits 4 times
        repeat(4) {
            vm2.onPinDigitEntered("9")
            vm2.onPinDigitEntered("9")
            vm2.onPinDigitEntered("9")
            vm2.onPinDigitEntered("9")
            kotlinx.coroutines.delay(100)
        }
        assertEquals(1, vm2.uiState.value.remainingAttempts)
        assertFalse(vm2.uiState.value.isLockedOut)

        // Enter wrong digits the 5th time -> lockout
        vm2.onPinDigitEntered("9")
        vm2.onPinDigitEntered("9")
        vm2.onPinDigitEntered("9")
        vm2.onPinDigitEntered("9")
        kotlinx.coroutines.delay(100)

        assertEquals(0, vm2.uiState.value.remainingAttempts)
        assertTrue(vm2.uiState.value.isLockedOut)
        
        // Verify digit entry is blocked when locked out
        vm2.onPinDigitEntered("1")
        assertEquals("", vm2.uiState.value.pinInput)
    }

    @Test
    fun testViewModelRecoveryFlow() = runBlocking {
        val recoveryCode = savePinUseCase("1234")

        val vm = SecurityViewModel(
            settingsRepository = settingsRepository,
            checkSecurityStatusUseCase = checkSecurityStatusUseCase,
            validatePinUseCase = validatePinUseCase,
            savePinUseCase = savePinUseCase,
            disablePinSecurityUseCase = disablePinSecurityUseCase,
            validateRecoveryCodeUseCase = validateRecoveryCodeUseCase
        )
        delay(100)

        // Lockout the app
        repeat(5) {
            vm.onPinDigitEntered("9")
            vm.onPinDigitEntered("9")
            vm.onPinDigitEntered("9")
            vm.onPinDigitEntered("9")
            kotlinx.coroutines.delay(100)
        }
        assertTrue(vm.uiState.value.isLockedOut)

        // Toggle recovery mode
        vm.enterRecoveryMode(true)
        assertTrue(vm.uiState.value.isRecoveryMode)

        // Enter invalid recovery code
        vm.onRecoveryCodeChanged("WRONG")
        vm.verifyRecoveryCode()
        kotlinx.coroutines.delay(100)
        assertNotNull(vm.uiState.value.recoveryCodeError)
        assertTrue(vm.uiState.value.isAppLocked)

        // Enter correct recovery code
        vm.onRecoveryCodeChanged(recoveryCode)
        vm.verifyRecoveryCode()
        kotlinx.coroutines.delay(100)

        // App should unlock, recovery mode should close, and lockout reset
        assertFalse(vm.uiState.value.isAppLocked)
        assertFalse(vm.uiState.value.isRecoveryMode)
        assertFalse(vm.uiState.value.isLockedOut)
        assertEquals(5, vm.uiState.value.remainingAttempts)
    }

    @Test
    fun testViewModelPinSetupAndBiometrics() = runBlocking {
        // Starts with SetupStep.INACTIVE
        assertEquals(SetupStep.INACTIVE, viewModel.uiState.value.setupStep)

        // Start PIN setup
        viewModel.startPinSetup()
        assertEquals(SetupStep.ENTER_NEW_PIN, viewModel.uiState.value.setupStep)

        // Mismatched or too short PIN handling
        viewModel.onSetupPinInputChanged("123")
        viewModel.submitSetupPin()
        assertEquals(SetupStep.ENTER_NEW_PIN, viewModel.uiState.value.setupStep)
        assertNotNull(viewModel.uiState.value.setupError)

        // Valid PIN entry -> proceed to confirmation
        viewModel.onSetupPinInputChanged("1234")
        viewModel.submitSetupPin()
        assertEquals(SetupStep.CONFIRM_NEW_PIN, viewModel.uiState.value.setupStep)
        assertNull(viewModel.uiState.value.setupError)

        // Confirmation mismatch handling
        viewModel.onSetupPinInputChanged("5678")
        viewModel.submitSetupPin()
        assertEquals(SetupStep.CONFIRM_NEW_PIN, viewModel.uiState.value.setupStep)
        assertNotNull(viewModel.uiState.value.setupError)

        // Valid confirmation -> saves and shows recovery code
        viewModel.onSetupPinInputChanged("1234")
        viewModel.submitSetupPin()
        kotlinx.coroutines.delay(100)

        assertEquals(SetupStep.SHOW_RECOVERY_CODE, viewModel.uiState.value.setupStep)
        val recoveryCode = viewModel.uiState.value.generatedRecoveryCode
        assertNotNull(recoveryCode)
        assertEquals(16, recoveryCode?.length)

        // Finish PIN setup
        viewModel.finishPinSetup()
        assertEquals(SetupStep.INACTIVE, viewModel.uiState.value.setupStep)
        assertNull(viewModel.uiState.value.generatedRecoveryCode)

        // Toggle biometrics
        viewModel.setBiometricEnabled(true)
        kotlinx.coroutines.delay(100)
        assertTrue(settingsRepository.getSecuritySettings().first()?.biometricEnabled == true)

        viewModel.setBiometricEnabled(false)
        kotlinx.coroutines.delay(100)
        assertFalse(settingsRepository.getSecuritySettings().first()?.biometricEnabled == true)
    }

    @Test
    fun testViewModelVerifyCurrentPinAndActions() = runBlocking {
        savePinUseCase("1234")

        // Trigger action that requires verification (e.g. DISABLE_PIN)
        viewModel.openVerifyCurrentPin(PendingSecurityAction.DISABLE_PIN)
        assertTrue(viewModel.uiState.value.verifyCurrentPinOpen)
        assertEquals(PendingSecurityAction.DISABLE_PIN, viewModel.uiState.value.pendingAction)

        // Enter wrong current PIN
        viewModel.onVerifyCurrentPinInputChanged("9999")
        viewModel.submitVerifyCurrentPin()
        kotlinx.coroutines.delay(100)
        assertNotNull(viewModel.uiState.value.verifyCurrentPinError)
        assertTrue(checkSecurityStatusUseCase().first()) // Security still active

        // Enter correct current PIN
        viewModel.onVerifyCurrentPinInputChanged("1234")
        viewModel.submitVerifyCurrentPin()
        kotlinx.coroutines.delay(100)

        // Verification modal closes and action disables security
        assertFalse(viewModel.uiState.value.verifyCurrentPinOpen)
        assertFalse(checkSecurityStatusUseCase().first()) // Security disabled!
    }

    @Test
    fun testViewModelVerifyCurrentPinToViewRecovery() = runBlocking {
        savePinUseCase("1234")

        // Trigger viewing recovery code
        viewModel.openVerifyCurrentPin(PendingSecurityAction.VIEW_RECOVERY_CODE)
        viewModel.onVerifyCurrentPinInputChanged("1234")
        viewModel.submitVerifyCurrentPin()
        kotlinx.coroutines.delay(100)

        // Modal closed, redirects to show recovery code step with generated recovery code
        assertFalse(viewModel.uiState.value.verifyCurrentPinOpen)
        assertEquals(SetupStep.SHOW_RECOVERY_CODE, viewModel.uiState.value.setupStep)
        assertNotNull(viewModel.uiState.value.generatedRecoveryCode)
    }

    @Test
    fun testViewModelVerifyCurrentPinToChangePin() = runBlocking {
        savePinUseCase("1234")

        // Trigger changing PIN
        viewModel.openVerifyCurrentPin(PendingSecurityAction.CHANGE_PIN)
        viewModel.onVerifyCurrentPinInputChanged("1234")
        viewModel.submitVerifyCurrentPin()
        kotlinx.coroutines.delay(100)

        // Modal closed, redirects to setup screen to enter new PIN
        assertFalse(viewModel.uiState.value.verifyCurrentPinOpen)
        assertEquals(SetupStep.ENTER_NEW_PIN, viewModel.uiState.value.setupStep)
    }
}
