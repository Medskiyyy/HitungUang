package com.hitunguang.feature.transfer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.transfer.domain.model.Transfer
import com.hitunguang.feature.transfer.domain.repository.TransferRepository
import com.hitunguang.feature.transfer.domain.usecase.RevertTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferWithAccountNames(
    val transfer: Transfer,
    val fromAccountName: String,
    val toAccountName: String
)

data class TransferHistoryUiState(
    val transfers: List<TransferWithAccountNames> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TransferHistoryViewModel @Inject constructor(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository,
    private val revertTransferUseCase: RevertTransferUseCase
) : ViewModel() {

    val uiState: StateFlow<TransferHistoryUiState> = combine(
        transferRepository.getAllTransfers(),
        accountRepository.getAllAccounts()
    ) { transfers, accounts ->
        val accountMap = accounts.associateBy { it.id }
        val mappedTransfers = transfers.map { transfer ->
            TransferWithAccountNames(
                transfer = transfer,
                fromAccountName = accountMap[transfer.fromAccountId]?.name ?: "Akun Terhapus",
                toAccountName = accountMap[transfer.toAccountId]?.name ?: "Akun Terhapus"
            )
        }
        TransferHistoryUiState(
            transfers = mappedTransfers,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransferHistoryUiState(isLoading = true)
    )

    fun revertTransfer(transfer: Transfer) {
        viewModelScope.launch {
            try {
                revertTransferUseCase(transfer)
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }
}
