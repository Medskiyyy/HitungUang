package com.hitunguang.feature.transfer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hitunguang.core.database.HitungUangDatabase
import com.hitunguang.core.database.entity.AccountEntity
import com.hitunguang.feature.account.data.repository.AccountRepositoryImpl
import com.hitunguang.feature.transfer.data.repository.TransferRepositoryImpl
import com.hitunguang.feature.transfer.domain.model.Transfer
import com.hitunguang.feature.transfer.domain.usecase.ExecuteTransferUseCase
import com.hitunguang.feature.transfer.presentation.TransferUiState
import com.hitunguang.feature.transfer.presentation.TransferViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TransferModuleTest {

    private lateinit var db: HitungUangDatabase
    private lateinit var executeTransferUseCase: ExecuteTransferUseCase
    private lateinit var transferRepo: TransferRepositoryImpl
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var viewModel: TransferViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(context, HitungUangDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        transferRepo = TransferRepositoryImpl(db.transferDao())
        accountRepo = AccountRepositoryImpl(db.accountDao())
        executeTransferUseCase = ExecuteTransferUseCase(transferRepo, db.accountDao())

        viewModel = TransferViewModel(executeTransferUseCase, accountRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testExecuteTransfer_Success() = runBlocking {
        val now = System.currentTimeMillis()
        // Setup two accounts
        val acc1 = AccountEntity("acc-1", "Dompet Tunai", "CASH", "wallet", 100000L, 100000L, now, now)
        val acc2 = AccountEntity("acc-2", "Rekening Bank", "BANK", "bank", 50000L, 50000L, now, now)
        db.accountDao().insertAccount(acc1)
        db.accountDao().insertAccount(acc2)

        val transfer = Transfer(
            id = "transfer-1",
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 30000L,
            adminFee = 2000L,
            note = "Saku bulanan",
            transferDate = now,
            createdAt = now,
            updatedAt = now
        )

        executeTransferUseCase(transfer)

        // Verify sender balance (100k - 30k - 2k = 68k)
        assertEquals(68000L, db.accountDao().getAccountBalance("acc-1"))
        // Verify receiver balance (50k + 30k = 80k)
        assertEquals(80000L, db.accountDao().getAccountBalance("acc-2"))

        // Verify transfer record exists
        val transfers = transferRepo.getAllTransfers().first()
        assertEquals(1, transfers.size)
        assertEquals("transfer-1", transfers[0].id)
        assertEquals(30000L, transfers[0].amount)
        assertEquals(2000L, transfers[0].adminFee)
    }

    @Test
    fun testExecuteTransfer_Fail_SameAccount() {
        runBlocking {
            val now = System.currentTimeMillis()
            val acc1 = AccountEntity("acc-1", "Dompet Tunai", "CASH", "wallet", 100000L, 100000L, now, now)
            db.accountDao().insertAccount(acc1)

            val transfer = Transfer(
                id = "transfer-1",
                fromAccountId = "acc-1",
                toAccountId = "acc-1",
                amount = 30000L,
                adminFee = 2000L,
                note = "Self",
                transferDate = now,
                createdAt = now,
                updatedAt = now
            )

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { executeTransferUseCase(transfer) }
            }
        }
    }

    @Test
    fun testExecuteTransfer_Fail_InvalidAmount() {
        runBlocking {
            val now = System.currentTimeMillis()
            val acc1 = AccountEntity("acc-1", "Dompet Tunai", "CASH", "wallet", 100000L, 100000L, now, now)
            val acc2 = AccountEntity("acc-2", "Rekening Bank", "BANK", "bank", 50000L, 50000L, now, now)
            db.accountDao().insertAccount(acc1)
            db.accountDao().insertAccount(acc2)

            val transfer = Transfer(
                id = "transfer-1",
                fromAccountId = "acc-1",
                toAccountId = "acc-2",
                amount = 0L,
                adminFee = 2000L,
                note = "Zero",
                transferDate = now,
                createdAt = now,
                updatedAt = now
            )

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { executeTransferUseCase(transfer) }
            }
        }
    }

    @Test
    fun testExecuteTransfer_Fail_InvalidAdminFee() {
        runBlocking {
            val now = System.currentTimeMillis()
            val acc1 = AccountEntity("acc-1", "Dompet Tunai", "CASH", "wallet", 100000L, 100000L, now, now)
            val acc2 = AccountEntity("acc-2", "Rekening Bank", "BANK", "bank", 50000L, 50000L, now, now)
            db.accountDao().insertAccount(acc1)
            db.accountDao().insertAccount(acc2)

            val transfer = Transfer(
                id = "transfer-1",
                fromAccountId = "acc-1",
                toAccountId = "acc-2",
                amount = 30000L,
                adminFee = -500L,
                note = "Negative fee",
                transferDate = now,
                createdAt = now,
                updatedAt = now
            )

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { executeTransferUseCase(transfer) }
            }
        }
    }

    @Test
    fun testExecuteTransfer_Fail_InsufficientBalance() {
        runBlocking {
            val now = System.currentTimeMillis()
            val acc1 = AccountEntity("acc-1", "Dompet Tunai", "CASH", "wallet", 10000L, 10000L, now, now)
            val acc2 = AccountEntity("acc-2", "Rekening Bank", "BANK", "bank", 50000L, 50000L, now, now)
            db.accountDao().insertAccount(acc1)
            db.accountDao().insertAccount(acc2)

            val transfer = Transfer(
                id = "transfer-1",
                fromAccountId = "acc-1",
                toAccountId = "acc-2",
                amount = 10000L,
                adminFee = 500L, // Total = 10500 > 10000
                note = "Overdraft",
                transferDate = now,
                createdAt = now,
                updatedAt = now
            )

            assertThrows(IllegalStateException::class.java) {
                runBlocking { executeTransferUseCase(transfer) }
            }
        }
    }

    @Test
    fun testRevertTransfer_Success() = runBlocking {
        val now = System.currentTimeMillis()
        val acc1 = AccountEntity("acc-1", "Dompet Tunai", "CASH", "wallet", 100000L, 68000L, now, now)
        val acc2 = AccountEntity("acc-2", "Rekening Bank", "BANK", "bank", 50000L, 80000L, now, now)
        db.accountDao().insertAccount(acc1)
        db.accountDao().insertAccount(acc2)

        val transfer = Transfer(
            id = "transfer-1",
            fromAccountId = "acc-1",
            toAccountId = "acc-2",
            amount = 30000L,
            adminFee = 2000L,
            note = "Saku bulanan",
            transferDate = now,
            createdAt = now,
            updatedAt = now
        )
        db.transferDao().insertTransfer(com.hitunguang.feature.transfer.data.mapper.TransferMapper.toEntity(transfer))

        // Revert the transfer
        transferRepo.revertTransfer(transfer)

        // Verify sender balance (68k + 30k + 2k = 100k)
        assertEquals(100000L, db.accountDao().getAccountBalance("acc-1"))
        // Verify receiver balance (80k - 30k = 50k)
        assertEquals(50000L, db.accountDao().getAccountBalance("acc-2"))

        // Verify transfer record is deleted
        val transfers = transferRepo.getAllTransfers().first()
        assertTrue(transfers.isEmpty())
    }

    @Test
    fun testTransferViewModel_Flow() = runBlocking {
        val states = mutableListOf<TransferUiState>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect { states.add(it) }
        }

        val now = System.currentTimeMillis()
        db.accountDao().insertAccount(AccountEntity("acc-1", "Dompet Tunai", "CASH", "wallet", 100000L, 100000L, now, now))
        db.accountDao().insertAccount(AccountEntity("acc-2", "Rekening Bank", "BANK", "bank", 50000L, 50000L, now, now))

        // Wait for accounts load
        kotlinx.coroutines.delay(100)
        val accountsList = viewModel.accounts.value
        assertEquals(2, accountsList.size)

        // Simulate form edits
        viewModel.onFromAccountSelected(accountsList[0])
        viewModel.onToAccountSelected(accountsList[1])
        viewModel.onAmountChanged("30000")
        viewModel.onAdminFeeChanged("2000")
        viewModel.onNoteChanged("Uang jajan")

        viewModel.executeTransfer()
        kotlinx.coroutines.delay(100)

        val finalState = states.last()
        assertNull(finalState.error)
        assertTrue(finalState.success)

        job.cancel()
    }
}
