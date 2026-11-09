package com.hitunguang.feature.transfer.presentation

import android.content.Context
import com.hitunguang.R
import com.hitunguang.core.common.util.CurrencyFormatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.transfer.domain.model.Transfer
import com.hitunguang.feature.transfer.domain.usecase.ExecuteTransferUseCase
import com.hitunguang.feature.transaction.domain.model.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val executeTransferUseCase: ExecuteTransferUseCase,
    private val accountRepository: AccountRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { list ->
                _accounts.value = list
            }
        }
    }

    fun onFromAccountSelected(account: Account) {
        _uiState.update { it.copy(fromAccount = account, error = null) }
    }

    fun onToAccountSelected(account: Account) {
        _uiState.update { it.copy(toAccount = account, error = null) }
    }

    fun onAmountChanged(amount: String) {
        if (amount.all { it.isDigit() }) {
            _uiState.update { it.copy(amount = amount, error = null) }
        }
    }

    fun onAdminFeeChanged(fee: String) {
        if (fee.all { it.isDigit() }) {
            _uiState.update { it.copy(adminFee = fee, error = null) }
        }
    }

    fun onNoteChanged(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onDateChanged(date: Long) {
        _uiState.update { it.copy(transferDate = date) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun executeTransfer() {
        val state = _uiState.value
        val fromAcc = state.fromAccount
        val toAcc = state.toAccount
        val amountVal = state.amount.toLongOrNull() ?: 0L
        val feeVal = state.adminFee.toLongOrNull() ?: 0L

        if (fromAcc == null) {
            _uiState.update { it.copy(error = context.getString(R.string.transfer_error_from_missing)) }
            return
        }
        if (toAcc == null) {
            _uiState.update { it.copy(error = context.getString(R.string.transfer_error_to_missing)) }
            return
        }
        if (fromAcc.id == toAcc.id) {
            _uiState.update { it.copy(error = context.getString(R.string.transfer_error_same_account)) }
            return
        }
        if (amountVal <= 0L) {
            _uiState.update { it.copy(error = context.getString(R.string.transfer_error_amount_positive)) }
            return
        }
        if (feeVal < 0L) {
            _uiState.update { it.copy(error = context.getString(R.string.transfer_error_fee_negative)) }
            return
        }
        if (fromAcc.currentBalance < (amountVal + feeVal)) {
            _uiState.update { it.copy(error = context.getString(R.string.transfer_error_insufficient_balance, CurrencyFormatter.format(fromAcc.currentBalance), CurrencyFormatter.format(amountVal + feeVal))) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val transfer = Transfer(
                    id = UUID.randomUUID().toString(),
                    fromAccountId = fromAcc.id,
                    toAccountId = toAcc.id,
                    amount = amountVal,
                    adminFee = feeVal,
                    note = state.note.takeIf { it.isNotBlank() },
                    transferDate = state.transferDate,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                executeTransferUseCase(transfer)
                _uiState.update { it.copy(isSaving = false, success = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.userMessage(context) ?: context.getString(R.string.transfer_error_generic)) }
            }
        }
    }

    fun resetState() {
        _uiState.value = TransferUiState()
    }
}
