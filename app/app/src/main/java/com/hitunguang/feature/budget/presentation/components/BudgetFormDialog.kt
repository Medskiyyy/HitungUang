package com.hitunguang.feature.budget.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.hitunguang.R
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.budget.domain.model.Budget
import com.hitunguang.feature.budget.domain.util.BudgetDates
import com.hitunguang.feature.category.domain.model.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormDialog(
    budget: Budget?,
    categories: List<Category>,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (
        categoryId: String?,
        budgetType: String,
        amount: Long,
        thresholdPercent: Int,
        startDate: Long,
        endDate: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var budgetType by remember { mutableStateOf(budget?.budgetType ?: "GLOBAL") }
    var selectedCategoryId by remember { mutableStateOf(budget?.categoryId ?: "") }
    var amountStr by remember { mutableStateOf(budget?.amount?.toString() ?: "") }
    var thresholdStr by remember { mutableStateOf(budget?.thresholdPercent?.toString() ?: "80") }
    // Local millis: start of the first day and 23:59:59.999 of the last day
    var startDateMillis by remember { mutableStateOf(BudgetDates.startOfDay(budget?.startDate ?: System.currentTimeMillis())) }
    var endDateMillis by remember { mutableStateOf(BudgetDates.endOfDay(budget?.endDate ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var amountError by remember { mutableStateOf<String?>(null) }
    var thresholdError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

    val expenseCategories = remember(categories) {
        categories.filter { it.categoryType == "EXPENSE" }
    }

    val isEditMode = budget != null
    val amountInvalidText = stringResource(R.string.budget_err_amount)
    val thresholdInvalidText = stringResource(R.string.budget_err_threshold)
    val categoryMissingText = stringResource(R.string.budget_err_category)
    val dateInvalidText = stringResource(R.string.budget_err_dates)
    val saveFailedText = stringResource(R.string.budget_err_save_failed)
    val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Radius.large),
        title = {
            Text(if (isEditMode) stringResource(R.string.budget_form_edit) else stringResource(R.string.budget_form_add), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                // Tipe Budget
                Column {
                    Text(stringResource(R.string.budget_form_type), style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = budgetType == "GLOBAL",
                            onClick = { budgetType = "GLOBAL"; selectedCategoryId = "" }
                        )
                        Text(stringResource(R.string.budget_form_all_categories))

                        Spacer(modifier = Modifier.width(Spacing.medium))

                        RadioButton(
                            selected = budgetType == "CATEGORY",
                            onClick = { budgetType = "CATEGORY" }
                        )
                        Text(stringResource(R.string.budget_form_category))
                    }
                }

                // Category selection dropdown if CATEGORY budget
                if (budgetType == "CATEGORY") {
                    Column {
                        Text(stringResource(R.string.budget_form_category_required), style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(Spacing.extraSmall))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val currentCategory = expenseCategories.find { it.id == selectedCategoryId }
                            OutlinedButton(
                                onClick = { categoryExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(Radius.medium)
                            ) {
                                Text(currentCategory?.name ?: stringResource(R.string.budget_form_pick_category))
                            }
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                expenseCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            categoryExpanded = false
                                            categoryError = null
                                        }
                                    )
                                }
                            }
                        }
                        categoryError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Amount limit
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        val v = it.toLongOrNull() ?: 0L
                        amountError = if (v <= 0L) amountInvalidText else null
                    },
                    label = { Text(stringResource(R.string.budget_form_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.medium)
                )

                // Threshold percent
                OutlinedTextField(
                    value = thresholdStr,
                    onValueChange = {
                        thresholdStr = it
                        val v = it.toIntOrNull() ?: 0
                        thresholdError = if (v !in 1..100) thresholdInvalidText else null
                    },
                    label = { Text(stringResource(R.string.budget_form_threshold)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = thresholdError != null,
                    supportingText = { thresholdError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.medium)
                )

                // Dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.budget_form_start), style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(Spacing.extraSmall))
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radius.medium)
                        ) {
                            Text(dateFormatter.format(Date(startDateMillis)))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.budget_form_end), style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(Spacing.extraSmall))
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radius.medium)
                        ) {
                            Text(dateFormatter.format(Date(endDateMillis)))
                        }
                    }
                }
                dateError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                errorMessage?.let {
                    Text(it.ifBlank { saveFailedText }, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toLongOrNull() ?: 0L
                    val threshold = thresholdStr.toIntOrNull() ?: 80
                    var hasError = false

                    if (amount <= 0L) {
                        amountError = amountInvalidText
                        hasError = true
                    }
                    if (threshold !in 1..100) {
                        thresholdError = thresholdInvalidText
                        hasError = true
                    }
                    if (budgetType == "CATEGORY" && selectedCategoryId.isBlank()) {
                        categoryError = categoryMissingText
                        hasError = true
                    }
                    if (startDateMillis > endDateMillis) {
                        dateError = dateInvalidText
                        hasError = true
                    } else {
                        dateError = null
                    }

                    if (!hasError) {
                        onSave(
                            selectedCategoryId.takeIf { it.isNotBlank() },
                            budgetType,
                            amount,
                            threshold,
                            startDateMillis,
                            endDateMillis
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = BudgetDates.localToPicker(startDateMillis))
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { startDateMillis = BudgetDates.pickerToLocalStart(it) }
                        showStartDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.common_choose))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = BudgetDates.localToPicker(endDateMillis))
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { endDateMillis = BudgetDates.pickerToLocalEnd(it) }
                        showEndDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.common_choose))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
