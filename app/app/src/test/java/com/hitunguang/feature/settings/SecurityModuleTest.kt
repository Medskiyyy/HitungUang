package com.hitunguang.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.datastore.SettingsDataStore
import com.hitunguang.core.security.PinHasher
import com.hitunguang.core.security.PinLockoutPolicy
import com.hitunguang.feature.settings.data.repository.SettingsRepositoryImpl
import com.hitunguang.feature.settings.domain.model.PinLockState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.MessageDigest

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
    private lateinit var dataStoreScope: CoroutineScope

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        ApplicationProvider.getApplicationContext<Context>()

        // Setup DataStore
        val datastoreFile = File(tmpFolder.newFolder(), "test_settings_prefs.preferences_pb")
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { datastoreFile }
        )
        settingsDataStore = SettingsDataStore(dataStore)
        settingsRepository = SettingsRepositoryImpl(settingsDataStore)

        // Setup Use Cases
        checkSecurityStatusUseCase = CheckSecurityStatusUseCase(settingsRepository)
        validatePinUseCase = ValidatePinUseCase(settingsRepository)
        savePinUseCase = SavePinUseCase(settingsRepository)
        disablePinSecurityUseCase = DisablePinSecurityUseCase(settingsRepository)
        validateRecoveryCodeUseCase = ValidateRecoveryCodeUseCase(settingsRepository)

        viewModel = newViewModel()
    }

    @After
    fun tearDown() {
        runBlocking { dataStoreScope.coroutineContext.job.cancelAndJoin() }
        Dispatchers.resetMain()
    }

    private fun newViewModel() = SecurityViewModel(
        settingsRepository = settingsRepository,
        checkSecurityStatusUseCase = checkSecurityStatusUseCase,
        validatePinUseCase = validatePinUseCase,
        savePinUseCase = savePinUseCase,
        disablePinSecurityUseCase = disablePinSecurityUseCase,
        validateRecoveryCodeUseCase = validateRecoveryCodeUseCase
    )

    private suspend fun waitUntil(timeoutMs: Long = 10_000, condition: suspend () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) fail("Condition not met in time")
            delay(10)
        }
    }

    private suspend fun lockedViewModel(): SecurityViewModel {
        val vm = newViewModel()
        waitUntil { vm.uiState.value.isLockStateKnown && vm.uiState.value.securitySettings != null }
        return vm
    }

    private suspend fun SecurityViewModel.enterPin(pin: String) {
        pin.forEach { digit ->
            waitUntil { !uiState.value.isVerifying }
            onPinDigitEntered(digit.toString())
        }
        waitUntil { !uiState.value.isVerifying }
    }

    private fun legacySha256(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private suspend fun saveLegacyPin(pin: String) {
        settingsRepository.saveSecuritySettings(
            SecuritySettings(
                id = "security_settings",
                pinHash = legacySha256(pin),
                biometricEnabled = false,
                recoveryCodeHash = null,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
    }

    @Test
    fun testPinHasher() {
        val pin = "123456"
        val hash = PinHasher.hash(pin)
        assertTrue(hash.startsWith("pbkdf2$${PinHasher.DEFAULT_ITERATIONS}$"))
        assertEquals(4, hash.split("$").size)
        // Random salt: same input gives different hashes, both verify
        val other = PinHasher.hash(pin)
        assertNotEquals(hash, other)
        assertTrue(PinHasher.verify(pin, hash))
        assertTrue(PinHasher.verify(pin, other))
        assertFalse(PinHasher.verify("654321", hash))
        assertFalse(PinHasher.isLegacy(hash))
    }

    @Test
    fun testPinHasherLegacyAndMalformed() {
        val legacy = legacySha256("1234")
        assertTrue(PinHasher.isLegacy(legacy))
        assertTrue(PinHasher.verify("1234", legacy))
        assertFalse(PinHasher.verify("4321", legacy))
        assertFalse(PinHasher.verify("1234", "pbkdf2\$abc\$def"))
        assertFalse(PinHasher.verify("1234", "pbkdf2\$0\$AAAA\$AAAA"))
        assertFalse(PinHasher.verify("1234", "pbkdf2\$1000\$!!!\$###"))
    }

    @Test
    fun testLockoutPolicy() {
        assertEquals(0L, PinLockoutPolicy.lockoutDurationMs(4))
        assertEquals(30_000L, PinLockoutPolicy.lockoutDurationMs(5))
        assertEquals(60_000L, PinLockoutPolicy.lockoutDurationMs(6))
        assertEquals(120_000L, PinLockoutPolicy.lockoutDurationMs(7))
        assertEquals(30 * 60_000L, PinLockoutPolicy.lockoutDurationMs(11))
        assertEquals(30 * 60_000L, PinLockoutPolicy.lockoutDurationMs(100))
        assertEquals(5, PinLockoutPolicy.remainingAttempts(0))
        assertEquals(0, PinLockoutPolicy.remainingAttempts(7))
    }

    @Test
    fun testPinFormatValidation() {
        assertTrue(SecurityViewModel.isValidPinFormat("1234"))
        assertTrue(SecurityViewModel.isValidPinFormat("123456"))
        assertFalse(SecurityViewModel.isValidPinFormat("123"))
        assertFalse(SecurityViewModel.isValidPinFormat("1234567"))
        assertFalse(SecurityViewModel.isValidPinFormat("12a4"))
        assertFalse(SecurityViewModel.isValidPinFormat("１２３４"))
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
        val savedSettings = settingsRepository.getSecuritySettings().first()!!
        assertTrue(PinHasher.verify(pin, savedSettings.pinHash!!))
        assertTrue(PinHasher.verify(rawRecoveryCode, savedSettings.recoveryCodeHash!!))
        assertEquals(4, savedSettings.pinLength)

        // 3. Validate PIN
        assertTrue(validatePinUseCase(pin))
        assertFalse(validatePinUseCase("5678"))

        // 4. Valid recovery code no longer disables the PIN
        assertTrue(validateRecoveryCodeUseCase(rawRecoveryCode.lowercase()))
        assertTrue(checkSecurityStatusUseCase().first())

        // 5. Invalid recovery code
        assertFalse(validateRecoveryCodeUseCase("WRONGRECOVERY123"))
        assertTrue(checkSecurityStatusUseCase().first())

        // 6. Disable PIN Security Use Case directly
        disablePinSecurityUseCase()
        assertFalse(checkSecurityStatusUseCase().first())
        val disabledSettings = settingsRepository.getSecuritySettings().first()
        assertNull(disabledSettings?.pinHash)
        assertNull(disabledSettings?.recoveryCodeHash)
        assertNull(disabledSettings?.pinLength)
        assertFalse(disabledSettings?.biometricEnabled ?: true)
    }

    @Test
    fun testLegacyHashUpgradedAfterSuccessfulValidation() = runBlocking {
        saveLegacyPin("123456")
        assertFalse(validatePinUseCase("111111"))
        assertTrue(PinHasher.isLegacy(settingsRepository.getSecuritySettings().first()!!.pinHash!!))

        assertTrue(validatePinUseCase("123456"))
        val upgraded = settingsRepository.getSecuritySettings().first()!!
        assertFalse(PinHasher.isLegacy(upgraded.pinHash!!))
        assertEquals(6, upgraded.pinLength)
        assertTrue(validatePinUseCase("123456"))
    }

    @Test
    fun testViewModelInitializationAndAppLock() = runBlocking {
        // App is unlocked when no security settings exist or PIN is null
        assertFalse(viewModel.uiState.value.isAppLocked)

        // Save a PIN
        savePinUseCase("123456")

        // Create a new ViewModel to simulate app launch (cold start) with PIN configured
        val newViewModel = lockedViewModel()

        // App should lock initially because PIN is configured
        assertTrue(newViewModel.uiState.value.isAppLocked)
    }

    @Test
    fun testSixAndFiveDigitPinsUnlock() = runBlocking {
        savePinUseCase("123456")
        val vm = lockedViewModel()
        // The first four digits must not trigger a verification or burn an attempt
        vm.enterPin("1234")
        assertTrue(vm.uiState.value.isAppLocked)
        assertEquals("1234", vm.uiState.value.pinInput)
        assertEquals(5, vm.uiState.value.remainingAttempts)
        vm.enterPin("56")
        assertFalse(vm.uiState.value.isAppLocked)

        savePinUseCase("24680")
        val vm5 = lockedViewModel()
        vm5.enterPin("24680")
        assertFalse(vm5.uiState.value.isAppLocked)
    }

    @Test
    fun testLegacyPinWithoutStoredLength() = runBlocking {
        saveLegacyPin("1234")
        val vm = lockedViewModel()
        assertNull(vm.uiState.value.securitySettings?.pinLength)

        // Wrong guesses at length 4 and 5 are silent, only length 6 counts
        vm.enterPin("99999")
        assertEquals(5, vm.uiState.value.remainingAttempts)
        assertEquals("99999", vm.uiState.value.pinInput)
        vm.enterPin("9")
        assertEquals(4, vm.uiState.value.remainingAttempts)
        assertEquals("", vm.uiState.value.pinInput)

        vm.enterPin("1234")
        assertFalse(vm.uiState.value.isAppLocked)
        assertEquals(0, settingsRepository.getPinLockState().first().failedAttempts)
        val upgraded = settingsRepository.getSecuritySettings().first()!!
        assertEquals(4, upgraded.pinLength)
        assertFalse(PinHasher.isLegacy(upgraded.pinHash!!))
    }

    @Test
    fun testViewModelPinEntryAndLockout() = runBlocking {
        savePinUseCase("1234")

        val vm = lockedViewModel()
        assertTrue(vm.uiState.value.isAppLocked)
        assertEquals(5, vm.uiState.value.remainingAttempts)
        assertFalse(vm.uiState.value.isLockedOut)

        vm.enterPin("1234")
        assertFalse(vm.uiState.value.isAppLocked)
        assertEquals(5, vm.uiState.value.remainingAttempts)

        val vm2 = lockedViewModel()
        assertTrue(vm2.uiState.value.isAppLocked)

        // Enter wrong digits 4 times
        repeat(4) { vm2.enterPin("9999") }
        assertEquals(1, vm2.uiState.value.remainingAttempts)
        assertFalse(vm2.uiState.value.isLockedOut)

        // Enter wrong digits the 5th time -> lockout
        vm2.enterPin("9999")

        assertEquals(0, vm2.uiState.value.remainingAttempts)
        assertTrue(vm2.uiState.value.isLockedOut)
        val lockState = settingsRepository.getPinLockState().first()
        assertEquals(5, lockState.failedAttempts)
        val remaining = lockState.lockoutUntil - System.currentTimeMillis()
        assertTrue(remaining in 1..30_000L)

        // Verify digit entry is blocked when locked out
        vm2.onPinDigitEntered("1")
        assertEquals("", vm2.uiState.value.pinInput)

        // Lockout survives a new ViewModel (process death)
        val vm3 = lockedViewModel()
        waitUntil { vm3.uiState.value.isLockedOut }
        assertEquals(0, vm3.uiState.value.remainingAttempts)
        vm3.onPinDigitEntered("1")
        assertEquals("", vm3.uiState.value.pinInput)
    }

    @Test
    fun testExpiredLockoutDoublesOnNextFailure() = runBlocking {
        savePinUseCase("1234")
        settingsRepository.savePinLockState(PinLockState(failedAttempts = 5, lockoutUntil = 1L))
        val vm = lockedViewModel()
        assertFalse(vm.uiState.value.isLockedOut)

        vm.enterPin("9999")
        assertTrue(vm.uiState.value.isLockedOut)
        val lockState = settingsRepository.getPinLockState().first()
        assertEquals(6, lockState.failedAttempts)
        assertTrue(lockState.lockoutUntil - System.currentTimeMillis() > 30_000L)
    }

    @Test
    fun testRelockAfterBackgroundGrace() = runBlocking {
        savePinUseCase("1234")
        val vm = lockedViewModel()
        vm.enterPin("1234")
        assertFalse(vm.uiState.value.isAppLocked)

        // A short trip to the camera or gallery keeps the app unlocked
        vm.onAppStopped(1_000L)
        vm.onAppStarted(1_000L + SecurityViewModel.RELOCK_GRACE_MS - 1)
        assertFalse(vm.uiState.value.isAppLocked)

        vm.onAppStopped(10_000L)
        vm.onAppStarted(10_000L + SecurityViewModel.RELOCK_GRACE_MS)
        assertTrue(vm.uiState.value.isAppLocked)

        // Without a PIN nothing relocks
        disablePinSecurityUseCase()
        val noPin = newViewModel()
        waitUntil { noPin.uiState.value.isLockStateKnown }
        noPin.onAppStopped(0L)
        noPin.onAppStarted(SecurityViewModel.RELOCK_GRACE_MS * 10)
        assertFalse(noPin.uiState.value.isAppLocked)
    }

    @Test
    fun testViewModelRecoveryFlow() = runBlocking {
        val recoveryCode = savePinUseCase("1234")

        val vm = lockedViewModel()

        // Lockout the app
        repeat(5) { vm.enterPin("9999") }
        assertTrue(vm.uiState.value.isLockedOut)

        // Toggle recovery mode
        vm.enterRecoveryMode(true)
        assertTrue(vm.uiState.value.isRecoveryMode)

        // Enter invalid recovery code
        vm.onRecoveryCodeChanged("WRONG")
        vm.verifyRecoveryCode()
        waitUntil { vm.uiState.value.recoveryCodeError != null }
        assertTrue(vm.uiState.value.isAppLocked)

        // Correct recovery code -> user must set a new PIN, app stays locked until then
        vm.onRecoveryCodeChanged(recoveryCode)
        vm.verifyRecoveryCode()
        waitUntil { vm.uiState.value.setupStep == SetupStep.ENTER_NEW_PIN }
        assertTrue(vm.uiState.value.isAppLocked)
        assertFalse(vm.uiState.value.isRecoveryMode)
        assertFalse(vm.uiState.value.isLockedOut)
        assertEquals(5, vm.uiState.value.remainingAttempts)
        assertTrue(checkSecurityStatusUseCase().first())
        assertEquals(0, settingsRepository.getPinLockState().first().failedAttempts)

        vm.onSetupPinInputChanged("567890")
        vm.submitSetupPin()
        vm.onSetupPinInputChanged("567890")
        vm.submitSetupPin()
        waitUntil { vm.uiState.value.setupStep == SetupStep.SHOW_RECOVERY_CODE }
        assertTrue(vm.uiState.value.isAppLocked)

        vm.finishPinSetup()
        assertFalse(vm.uiState.value.isAppLocked)
        assertTrue(validatePinUseCase("567890"))
        assertFalse(validatePinUseCase("1234"))
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

        // Non-digit PIN is rejected
        viewModel.onSetupPinInputChanged("12a4")
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
        waitUntil { viewModel.uiState.value.setupStep == SetupStep.SHOW_RECOVERY_CODE }

        val recoveryCode = viewModel.uiState.value.generatedRecoveryCode
        assertNotNull(recoveryCode)
        assertEquals(16, recoveryCode?.length)

        // Finish PIN setup
        viewModel.finishPinSetup()
        assertEquals(SetupStep.INACTIVE, viewModel.uiState.value.setupStep)
        assertNull(viewModel.uiState.value.generatedRecoveryCode)
        assertFalse(viewModel.uiState.value.isAppLocked)

        // Toggle biometrics
        viewModel.setBiometricEnabled(true)
        waitUntil { settingsRepository.getSecuritySettings().first()?.biometricEnabled == true }

        viewModel.setBiometricEnabled(false)
        waitUntil { settingsRepository.getSecuritySettings().first()?.biometricEnabled == false }
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
        waitUntil { viewModel.uiState.value.verifyCurrentPinError != null }
        assertTrue(checkSecurityStatusUseCase().first()) // Security still active

        // Enter correct current PIN -> modal closes and security gets disabled
        viewModel.onVerifyCurrentPinInputChanged("1234")
        viewModel.submitVerifyCurrentPin()
        waitUntil { !checkSecurityStatusUseCase().first() }
        assertFalse(viewModel.uiState.value.verifyCurrentPinOpen)
    }

    @Test
    fun testViewModelVerifyCurrentPinToViewRecovery() = runBlocking {
        savePinUseCase("1234")

        // Trigger generating a new recovery code
        viewModel.openVerifyCurrentPin(PendingSecurityAction.VIEW_RECOVERY_CODE)
        viewModel.onVerifyCurrentPinInputChanged("1234")
        viewModel.submitVerifyCurrentPin()
        waitUntil { viewModel.uiState.value.setupStep == SetupStep.SHOW_RECOVERY_CODE }

        // Modal closed, shows the newly generated recovery code
        assertFalse(viewModel.uiState.value.verifyCurrentPinOpen)
        assertNotNull(viewModel.uiState.value.generatedRecoveryCode)
    }

    @Test
    fun testViewModelVerifyCurrentPinToChangePin() = runBlocking {
        savePinUseCase("1234")

        // Trigger changing PIN
        viewModel.openVerifyCurrentPin(PendingSecurityAction.CHANGE_PIN)
        viewModel.onVerifyCurrentPinInputChanged("1234")
        viewModel.submitVerifyCurrentPin()
        waitUntil { viewModel.uiState.value.setupStep == SetupStep.ENTER_NEW_PIN }

        // Modal closed, redirects to setup screen to enter new PIN
        assertFalse(viewModel.uiState.value.verifyCurrentPinOpen)
    }
}
