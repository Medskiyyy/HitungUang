package com.hitunguang.feature.receipt

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.ocr.OcrManager
import com.hitunguang.feature.receipt.domain.usecase.ScanReceiptUseCase
import com.hitunguang.feature.receipt.presentation.ReceiptScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OcrModuleTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeOcrManager: FakeOcrManager
    private lateinit var scanReceiptUseCase: ScanReceiptUseCase
    private lateinit var viewModel: ReceiptScannerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeOcrManager = FakeOcrManager()
        scanReceiptUseCase = ScanReceiptUseCase(fakeOcrManager)
        viewModel = ReceiptScannerViewModel(scanReceiptUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ScanReceiptUseCase calls recognizeText and returns success`() = runTest(testDispatcher) {
        val dummyUri = Uri.parse("content://media/external/images/media/1")
        val expectedText = "Extracted Receipt Text"
        fakeOcrManager.resultText = expectedText

        val result = scanReceiptUseCase(dummyUri)

        assertTrue(result.isSuccess)
        assertEquals(expectedText, result.getOrNull())
        assertEquals(dummyUri, fakeOcrManager.lastScannedUri)
    }

    @Test
    fun `ScanReceiptUseCase returns failure when recognizeText fails`() = runTest(testDispatcher) {
        val dummyUri = Uri.parse("content://media/external/images/media/1")
        fakeOcrManager.shouldFail = true

        val result = scanReceiptUseCase(dummyUri)

        assertTrue(result.isFailure)
        assertEquals("OCR failed error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `viewModel setImageUri updates state and clears past results`() = runTest(testDispatcher) {
        val dummyUri = Uri.parse("content://media/external/images/media/1")
        viewModel.setImageUri(dummyUri)

        val state = viewModel.uiState.value
        assertEquals(dummyUri, state.imageUri)
        assertNull(state.rawText)
        assertNull(state.errorMessage)
        assertFalse(state.isScanning)
    }

    @Test
    fun `viewModel startScan updates state to success with text`() = runTest(testDispatcher) {
        val dummyUri = Uri.parse("content://media/external/images/media/1")
        val expectedText = "OCR Text Success"
        fakeOcrManager.resultText = expectedText
        viewModel.setImageUri(dummyUri)

        viewModel.startScan()
        
        // At start of scan, isScanning should be true
        assertTrue(viewModel.uiState.value.isScanning)
        
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isScanning)
        assertEquals(expectedText, state.rawText)
        assertNull(state.errorMessage)
    }

    @Test
    fun `viewModel startScan updates state to failure with error message`() = runTest(testDispatcher) {
        val dummyUri = Uri.parse("content://media/external/images/media/1")
        fakeOcrManager.shouldFail = true
        viewModel.setImageUri(dummyUri)

        viewModel.startScan()
        
        // At start of scan, isScanning should be true
        assertTrue(viewModel.uiState.value.isScanning)
        
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isScanning)
        assertNull(state.rawText)
        assertEquals("OCR failed error", state.errorMessage)
    }

    @Test
    fun `viewModel clearAll resets uiState`() = runTest(testDispatcher) {
        val dummyUri = Uri.parse("content://media/external/images/media/1")
        viewModel.setImageUri(dummyUri)
        viewModel.startScan()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify state is set
        assertNotNull(viewModel.uiState.value.imageUri)
        assertNotNull(viewModel.uiState.value.rawText)

        viewModel.clearAll()

        val state = viewModel.uiState.value
        assertNull(state.imageUri)
        assertNull(state.rawText)
        assertNull(state.errorMessage)
        assertFalse(state.isScanning)
    }

    private class FakeOcrManager : OcrManager {
        var resultText: String = "Test extracted text"
        var shouldFail: Boolean = false
        var lastScannedUri: Uri? = null

        override suspend fun recognizeText(imageUri: Uri): Result<String> {
            lastScannedUri = imageUri
            return if (shouldFail) {
                Result.failure(Exception("OCR failed error"))
            } else {
                Result.success(resultText)
            }
        }
    }
}
