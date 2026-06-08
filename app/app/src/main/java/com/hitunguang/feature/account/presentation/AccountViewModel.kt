package com.hitunguang.feature.account.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.account.domain.usecase.CreateAccountUseCase
import com.hitunguang.feature.account.domain.usecase.DeleteAccountUseCase
import com.hitunguang.feature.account.domain.usecase.UpdateAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AccountUiState(
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val accountToEdit: Account? = null,
    val accountToDelete: Account? = null,
    val hasTransactions: Boolean = false,
    val showReplacementPicker: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val createAccountUseCase: CreateAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun showEditDialog(account: Account) {
        _uiState.update { it.copy(showEditDialog = true, accountToEdit = account) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, accountToEdit = null) }
    }

    fun showDeleteDialog(account: Account) {
        viewModelScope.launch {
            val txCount = accountRepository.getTransactionCountForAccount(account.id)
            val transferCount = accountRepository.getTransferCountForAccount(account.id)
            val hasData = txCount > 0 || transferCount > 0
            
            _uiState.update { 
                it.copy(
                    showDeleteDialog = true, 
                    accountToDelete = account,
                    hasTransactions = hasData,
                    showReplacementPicker = hasData
                ) 
            }
        }
    }

    fun hideDeleteDialog() {
        _uiState.update { 
            it.copy(
                showDeleteDialog = false, 
                accountToDelete = null,
                hasTransactions = false,
                showReplacementPicker = false,
                error = null
            ) 
        }
    }

    fun createAccount(name: String, type: String, icon: String, initialBalance: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val account = Account(
                id = UUID.randomUUID().toString(),
                name = name,
                accountType = type,
                icon = icon,
                initialBalance = initialBalance,
                currentBalance = initialBalance,
                createdAt = now,
                updatedAt = now
            )
            createAccountUseCase(account)
            hideCreateDialog()
        }
    }

    fun updateAccount(account: Account, name: String, type: String, icon: String) {
        viewModelScope.launch {
            val updated = account.copy(
                name = name,
                accountType = type,
                icon = icon,
                updatedAt = System.currentTimeMillis()
            )
            updateAccountUseCase(updated)
            hideEditDialog()
        }
    }

    fun deleteAccount(replacementAccountId: String? = null) {
        val account = _uiState.value.accountToDelete ?: return
        viewModelScope.launch {
            try {
                deleteAccountUseCase(account, replacementAccountId)
                hideDeleteDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
