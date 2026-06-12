package com.hitunguang.feature.receipt.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.account.domain.repository.AccountRepository
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.receipt.domain.usecase.ParsedItemInput
import com.hitunguang.feature.receipt.domain.usecase.ParseReceiptUseCase
import com.hitunguang.feature.receipt.domain.usecase.SaveReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptReviewViewModel @Inject constructor(
    private val saveReceiptUseCase: SaveReceiptUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val parseReceiptUseCase: ParseReceiptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptReviewUiState())
    val uiState: StateFlow<ReceiptReviewUiState> = _uiState.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private var ocrRawText: String? = null

    init {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { list ->
                _accounts.value = list
                if (_uiState.value.accountId.isEmpty() && list.isNotEmpty()) {
                    _uiState.update { it.copy(accountId = list.first().id) }
                }
                ocrRawText?.let { raw ->
                    val suggested = getSuggestedAccount(raw, list)
                    _uiState.update { it.copy(suggestedAccountId = suggested?.id) }
                }
            }
        }

        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { list ->
                val expenseCategories = list.filter { it.categoryType == "EXPENSE" }
                _categories.value = expenseCategories
                if (_uiState.value.categoryId == null && expenseCategories.isNotEmpty()) {
                    val defaultCat = expenseCategories.find { it.name.lowercase() == "lainnya" } ?: expenseCategories.firstOrNull()
                    _uiState.update { it.copy(categoryId = defaultCat?.id) }
                }
                ocrRawText?.let { raw ->
                    val suggested = getSuggestedCategory(raw, expenseCategories)
                    _uiState.update { it.copy(suggestedCategoryId = suggested?.id) }
                }
            }
        }
    }

    private fun getSuggestedAccount(rawText: String, accounts: List<Account>): Account? {
        val text = rawText.lowercase()
        val hasBankKeyword = listOf("bca", "mandiri", "bri", "bni", "cimb", "debit", "kartu credit", "credit card", "visa", "mastercard").any { text.contains(it) }
        val hasEWalletKeyword = listOf("gopay", "shopeepay", "ovo", "dana", "linkaja").any { text.contains(it) }
        val hasCashKeyword = listOf("cash", "tunai", "kembalian").any { text.contains(it) }

        return when {
            hasEWalletKeyword -> accounts.find { it.accountType == "E_WALLET" } ?: accounts.find { it.accountType == "BANK" }
            hasBankKeyword -> accounts.find { it.accountType == "BANK" }
            hasCashKeyword -> accounts.find { it.accountType == "CASH" }
            else -> null
        }
    }

    private fun getSuggestedCategory(rawText: String, categories: List<Category>): Category? {
        val text = rawText.lowercase()
        val foodKeywords = listOf("makan", "minum", "resto", "cafe", "coffee", "kopi", "bakso", "mie", "nasi", "burger", "pizza", "teh", "susu", "restoran", "warung")
        val shoppingKeywords = listOf("belanja", "mart", "indomaret", "alfamart", "supermarket", "mall", "toko", "baju", "celana", "sepatu")
        val transportKeywords = listOf("gojek", "grab", "uber", "taxi", "bensin", "pertamina", "shell", "parkir", "tol", "ticket", "tiket")
        val healthKeywords = listOf("apotek", "obat", "dokter", "klinik", "sakit", "vitamin", "rs", "rumah sakit")

        return when {
            foodKeywords.any { text.contains(it) } -> categories.find { it.name.lowercase().contains("makan") || it.name.lowercase().contains("kuliner") }
            shoppingKeywords.any { text.contains(it) } -> categories.find { it.name.lowercase().contains("belanja") || it.name.lowercase().contains("shopping") }
            transportKeywords.any { text.contains(it) } -> categories.find { it.name.lowercase().contains("transpor") || it.name.lowercase().contains("perjalanan") }
            healthKeywords.any { text.contains(it) } -> categories.find { it.name.lowercase().contains("sehat") || it.name.lowercase().contains("kesehatan") }
            else -> null
        }
    }

    fun initializeWithOcr(imageUri: Uri, rawText: String) {
        ocrRawText = rawText
        val parsed = parseReceiptUseCase(rawText)
        
        val parsedItems = parsed.items.map { item ->
            ParsedItemInput(
                name = item.name,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                subtotal = item.subtotal
            )
        }

        val taxVal = parsed.tax ?: 0L
        val subtotalVal = parsedItems.sumOf { it.subtotal }
        val totalVal = parsed.total.coerceAtLeast(subtotalVal + taxVal)

        val suggestedAcc = getSuggestedAccount(rawText, _accounts.value)
        val suggestedCat = getSuggestedCategory(rawText, _categories.value)

        _uiState.update {
            it.copy(
                imageUri = imageUri,
                merchantName = parsed.merchantName ?: "",
                receiptDate = parsed.date ?: System.currentTimeMillis(),
                items = parsedItems,
                taxStr = taxVal.toString(),
                subtotal = subtotalVal,
                totalStr = totalVal.toString(),
                isMerchantConfident = !parsed.merchantName.isNullOrBlank(),
                isDateConfident = parsed.date != null,
                isItemsConfident = parsedItems.isNotEmpty(),
                suggestedAccountId = suggestedAcc?.id,
                suggestedCategoryId = suggestedCat?.id
            )
        }
    }

    fun updateMerchantName(name: String) {
        _uiState.update { it.copy(merchantName = name) }
    }

    fun updateReceiptDate(date: Long) {
        _uiState.update { it.copy(receiptDate = date) }
    }

    fun updateAccount(accountId: String) {
        _uiState.update { it.copy(accountId = accountId) }
    }

    fun updateCategory(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun updateTax(tax: String) {
        val taxVal = tax.toLongOrNull() ?: 0L
        _uiState.update {
            val sub = it.subtotal
            it.copy(
                taxStr = tax,
                totalStr = (sub + taxVal).toString()
            )
        }
    }

    fun updateItemName(index: Int, name: String) {
        _uiState.update { state ->
            val updatedItems = state.items.toMutableList()
            if (index in updatedItems.indices) {
                val old = updatedItems[index]
                updatedItems[index] = old.copy(name = name)
            }
            state.copy(items = updatedItems)
        }
    }

    fun updateItemQty(index: Int, qtyStr: String) {
        val qty = qtyStr.replace(',', '.').toDoubleOrNull()
        _uiState.update { state ->
            val updatedItems = state.items.toMutableList()
            if (index in updatedItems.indices) {
                val old = updatedItems[index]
                val newSubtotal = if (qty != null) (qty * old.unitPrice).toLong() else old.subtotal
                updatedItems[index] = old.copy(quantity = qty, subtotal = newSubtotal)
            }
            val newSubtotalSum = updatedItems.sumOf { it.subtotal }
            val taxVal = state.taxStr.toLongOrNull() ?: 0L
            state.copy(
                items = updatedItems,
                subtotal = newSubtotalSum,
                totalStr = (newSubtotalSum + taxVal).toString()
            )
        }
    }

    fun updateItemUnitPrice(index: Int, priceStr: String) {
        val price = priceStr.toLongOrNull() ?: 0L
        _uiState.update { state ->
            val updatedItems = state.items.toMutableList()
            if (index in updatedItems.indices) {
                val old = updatedItems[index]
                val qty = old.quantity ?: 1.0
                val newSubtotal = (qty * price).toLong()
                updatedItems[index] = old.copy(unitPrice = price, subtotal = newSubtotal)
            }
            val newSubtotalSum = updatedItems.sumOf { it.subtotal }
            val taxVal = state.taxStr.toLongOrNull() ?: 0L
            state.copy(
                items = updatedItems,
                subtotal = newSubtotalSum,
                totalStr = (newSubtotalSum + taxVal).toString()
            )
        }
    }

    fun addItem() {
        _uiState.update { state ->
            val updatedItems = state.items.toMutableList()
            updatedItems.add(ParsedItemInput("", 1.0, 0L, 0L))
            state.copy(items = updatedItems)
        }
    }

    fun removeItem(index: Int) {
        _uiState.update { state ->
            val updatedItems = state.items.toMutableList()
            if (index in updatedItems.indices) {
                updatedItems.removeAt(index)
            }
            val newSubtotalSum = updatedItems.sumOf { it.subtotal }
            val taxVal = state.taxStr.toLongOrNull() ?: 0L
            state.copy(
                items = updatedItems,
                subtotal = newSubtotalSum,
                totalStr = (newSubtotalSum + taxVal).toString()
            )
        }
    }

    fun saveReceipt() {
        val state = _uiState.value
        val imageUri = state.imageUri ?: return
        if (state.merchantName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama toko tidak boleh kosong") }
            return
        }
        if (state.accountId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Harus memilih dompet") }
            return
        }
        val total = state.totalStr.toLongOrNull() ?: 0L
        if (total <= 0) {
            _uiState.update { it.copy(errorMessage = "Total belanja harus lebih dari 0") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                saveReceiptUseCase(
                    tempImageUri = imageUri,
                    merchantName = state.merchantName,
                    receiptDate = state.receiptDate,
                    subtotal = state.subtotal,
                    tax = state.taxStr.toLongOrNull() ?: 0L,
                    total = total,
                    ocrRawText = ocrRawText,
                    accountId = state.accountId,
                    categoryId = state.categoryId,
                    items = state.items
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.localizedMessage ?: "Gagal menyimpan struk"
                    )
                }
            }
        }
    }
}
