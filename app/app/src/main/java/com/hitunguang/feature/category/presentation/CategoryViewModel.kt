package com.hitunguang.feature.category.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.domain.repository.CategoryRepository
import com.hitunguang.feature.category.domain.usecase.DeleteCategoryUseCase
import com.hitunguang.feature.category.domain.usecase.GetCategoriesUseCase
import com.hitunguang.feature.category.domain.usecase.SaveCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CategoryUiState(
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val categoryToEdit: Category? = null,
    val categoryToDelete: Category? = null,
    val defaultCategoryType: String = "INCOME",
    val hasTransactions: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    val categories: StateFlow<List<Category>> = getCategoriesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val incomeCategories: StateFlow<List<Category>> = categories
        .map { list -> list.filter { it.categoryType == "INCOME" } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expenseCategories: StateFlow<List<Category>> = categories
        .map { list -> list.filter { it.categoryType == "EXPENSE" } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    fun showCreateDialog(defaultType: String) {
        _uiState.update { it.copy(showCreateDialog = true, defaultCategoryType = defaultType) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun showEditDialog(category: Category) {
        _uiState.update { it.copy(showEditDialog = true, categoryToEdit = category) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, categoryToEdit = null) }
    }

    fun showDeleteDialog(category: Category) {
        viewModelScope.launch {
            val txCount = categoryRepository.getTransactionCountForCategory(category.id)
            _uiState.update { 
                it.copy(
                    showDeleteDialog = true,
                    categoryToDelete = category,
                    hasTransactions = txCount > 0
                )
            }
        }
    }

    fun hideDeleteDialog() {
        _uiState.update { 
            it.copy(
                showDeleteDialog = false,
                categoryToDelete = null,
                hasTransactions = false,
                error = null
            )
        }
    }

    fun createCategory(name: String, type: String, icon: String?, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val category = Category(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    categoryType = type,
                    icon = icon,
                    isDefault = false,
                    isPinned = isPinned,
                    createdAt = now,
                    updatedAt = now
                )
                saveCategoryUseCase(category, isEdit = false)
                hideCreateDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateCategory(category: Category, name: String, type: String, icon: String?, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                val updated = category.copy(
                    name = name,
                    categoryType = type,
                    icon = icon,
                    isPinned = isPinned,
                    updatedAt = System.currentTimeMillis()
                )
                saveCategoryUseCase(updated, isEdit = true)
                hideEditDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun togglePinCategory(category: Category) {
        viewModelScope.launch {
            try {
                val updated = category.copy(
                    isPinned = !category.isPinned,
                    updatedAt = System.currentTimeMillis()
                )
                saveCategoryUseCase(updated, isEdit = true)
            } catch (e: Exception) {
                // Pin toggle is inline, so we just log or ignore if error
            }
        }
    }

    fun deleteCategory(forceDelete: Boolean = false) {
        val category = _uiState.value.categoryToDelete ?: return
        viewModelScope.launch {
            try {
                deleteCategoryUseCase(category, forceDelete = forceDelete)
                hideDeleteDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun restoreDefaultCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.restoreDefaultCategories()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
