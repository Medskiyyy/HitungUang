package com.hitunguang.feature.recurring.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hitunguang.R
import com.hitunguang.core.common.util.CurrencyFormatter
import com.hitunguang.core.designsystem.theme.Elevation
import com.hitunguang.core.designsystem.theme.ExpenseRed
import com.hitunguang.core.designsystem.theme.IncomeGreen
import com.hitunguang.core.designsystem.theme.Radius
import com.hitunguang.core.designsystem.theme.Spacing
import com.hitunguang.feature.account.domain.model.Account
import com.hitunguang.feature.category.domain.model.Category
import com.hitunguang.feature.category.presentation.components.CategoryIconHelper
import com.hitunguang.feature.recurring.domain.model.RecurrenceRule
import com.hitunguang.feature.recurring.domain.model.RecurringTransaction
import com.hitunguang.feature.recurring.domain.util.RecurrenceSchedule
import com.hitunguang.feature.recurring.presentation.RecurrenceText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Frequency(val labelRes: Int) {
    DAILY(R.string.recurring_freq_daily), WEEKLY(R.string.recurring_freq_weekly), MONTHLY(R.string.recurring_freq_monthly)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionFormDialog(
    recurring: RecurringTransaction?,
    accounts: List<Account>,
    categories: List<Category>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        title: String,
        note: String?,
        amount: Long,
        accountId: String,
        categoryId: String?,
        rule: RecurrenceRule,
        startDate: LocalDate
    ) -> Unit
) {
    val initialStart = remember(recurring) {
        recurring?.let { RecurrenceSchedule.dateOf(it.nextRunAt) } ?: LocalDate.now()
    }
    var type by remember { mutableStateOf(recurring?.transactionType ?: "EXPENSE") }
    var title by remember { mutableStateOf(recurring?.title ?: "") }
    var note by remember { mutableStateOf(recurring?.note ?: "") }
    var amountStr by remember { mutableStateOf(recurring?.amount?.toString() ?: "") }
    var selectedAccountId by remember { mutableStateOf(recurring?.accountId ?: accounts.firstOrNull()?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf(recurring?.categoryId ?: "") }
    var frequency by remember {
        mutableStateOf(
            when (recurring?.recurrenceRule) {
                RecurrenceRule.Weekly -> Frequency.WEEKLY
                is RecurrenceRule.Monthly -> Frequency.MONTHLY
                else -> Frequency.DAILY
            }
        )
    }
    var dayOfMonthStr by remember {
        mutableStateOf(
            ((recurring?.recurrenceRule as? RecurrenceRule.Monthly)?.dayOfMonth ?: initialStart.dayOfMonth).toString()
        )
    }
    var startDate by remember { mutableStateOf(initialStart) }

    var accountExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val accentColor = if (type == "EXPENSE") ExpenseRed else IncomeGreen
    val filteredCategories = remember(type, categories) { categories.filter { it.categoryType == type } }

    // Edit mode: keep the rule's own category even if it is empty or no longer listed
    val keepsOriginalCategory = recurring != null &&
        type == recurring.transactionType &&
        selectedCategoryId == (recurring.categoryId ?: "")
    val showsUnlistedCategory = keepsOriginalCategory && filteredCategories.none { it.id == selectedCategoryId }

    LaunchedEffect(type, filteredCategories) {
        if (keepsOriginalCategory) return@LaunchedEffect
        if (selectedCategoryId.isEmpty() || filteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = filteredCategories.firstOrNull()?.id ?: ""
        }
    }

    val dayOfMonth = dayOfMonthStr.toIntOrNull()
    val rule: RecurrenceRule? = when (frequency) {
        Frequency.DAILY -> RecurrenceRule.Daily
        Frequency.WEEKLY -> RecurrenceRule.Weekly
        Frequency.MONTHLY -> dayOfMonth?.takeIf { it in 1..31 }?.let { RecurrenceRule.Monthly(it) }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")) }
    val amountMissingText = stringResource(R.string.recurring_err_amount)
    val titleMissingText = stringResource(R.string.recurring_err_title)
    val accountMissingText = stringResource(R.string.recurring_err_account)
    val dayRangeText = stringResource(R.string.recurring_err_day)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.large)
            ) {
                Text(
                    text = if (recurring != null) stringResource(R.string.recurring_form_edit) else stringResource(R.string.recurring_form_add),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = Spacing.medium)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.medium),
                    shape = RoundedCornerShape(Radius.medium),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.extraSmall)
                    ) {
                        listOf("EXPENSE" to R.string.recurring_type_expense, "INCOME" to R.string.recurring_type_income).forEach { (itemType, label) ->
                            val isSelected = type == itemType
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(Radius.medium))
                                    .background(
                                        if (isSelected) {
                                            if (itemType == "EXPENSE") ExpenseRed else IncomeGreen
                                        } else Color.Transparent
                                    )
                                    .clickable { type = itemType },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(label),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = CurrencyFormatter.format(amountStr.toLongOrNull() ?: 0L),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    OutlinedTextField(
                        value = amountStr,
                        // Keep digits only so "2.000.000" typed or pasted still works
                        onValueChange = { amountStr = it.filter(Char::isDigit).take(15) },
                        placeholder = { Text("0") },
                        label = { Text(stringResource(R.string.recurring_form_amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(Radius.medium),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            focusedLabelColor = accentColor
                        )
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.recurring_form_title)) },
                    placeholder = { Text(stringResource(R.string.recurring_form_title_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(Radius.medium),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                Text(
                    stringResource(R.string.recurring_form_category),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    contentPadding = PaddingValues(bottom = Spacing.small)
                ) {
                    if (showsUnlistedCategory) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text(stringResource(R.string.recurring_form_no_category)) },
                                shape = RoundedCornerShape(Radius.medium)
                            )
                        }
                    }
                    items(filteredCategories) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconHelper.getIconByName(category.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            },
                            shape = RoundedCornerShape(Radius.medium),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.1f),
                                selectedLabelColor = accentColor,
                                selectedLeadingIconColor = accentColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                Text(
                    stringResource(R.string.recurring_form_account),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentAccount = accounts.find { it.id == selectedAccountId }
                    Surface(
                        onClick = { accountExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.medium),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = accountIcon(currentAccount?.accountType),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(Spacing.medium))
                            Text(
                                currentAccount?.name ?: stringResource(R.string.recurring_form_pick_account),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name, fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(accountIcon(acc.accountType), contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    selectedAccountId = acc.id
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                Text(
                    stringResource(R.string.recurring_form_frequency),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Frequency.entries.forEach { option ->
                        FilterChip(
                            selected = frequency == option,
                            onClick = { frequency = option },
                            label = { Text(stringResource(option.labelRes)) },
                            shape = RoundedCornerShape(Radius.medium)
                        )
                    }
                }

                when (frequency) {
                    Frequency.DAILY -> Unit
                    Frequency.WEEKLY -> Text(
                        text = stringResource(R.string.recurring_weekly_hint, RecurrenceText.weekdayName(LocalContext.current, startDate.dayOfWeek)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.small)
                    )
                    Frequency.MONTHLY -> {
                        Spacer(modifier = Modifier.height(Spacing.small))
                        OutlinedTextField(
                            value = dayOfMonthStr,
                            onValueChange = { dayOfMonthStr = it.filter(Char::isDigit).take(2) },
                            label = { Text(stringResource(R.string.recurring_form_day_of_month)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = rule == null,
                            supportingText = {
                                Text(
                                    when {
                                        rule == null -> stringResource(R.string.recurring_day_hint)
                                        (dayOfMonth ?: 0) > 28 -> stringResource(R.string.recurring_day_overflow, dayOfMonth.toString())
                                        else -> ""
                                    }
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(Radius.medium),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                Text(
                    stringResource(R.string.recurring_form_start),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = Spacing.small)
                )
                Surface(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.medium),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.medium))
                        Text(startDate.format(dateFormatter))
                    }
                }
                if (rule != null) {
                    Text(
                        text = stringResource(R.string.recurring_first_run, RecurrenceSchedule.firstDate(rule, startDate).format(dateFormatter)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.small)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.recurring_form_note)) },
                    shape = RoundedCornerShape(Radius.medium),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(
                        text = it.ifBlank { stringResource(R.string.recurring_err_save_failed) },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Spacing.medium)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.huge))
            }

            Surface(
                shadowElevation = Elevation.extraHigh,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.large),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Button(
                        onClick = {
                            val amount = amountStr.toLongOrNull() ?: 0L
                            validationMessage = when {
                                amount <= 0L -> amountMissingText
                                title.isBlank() -> titleMissingText
                                accounts.none { it.id == selectedAccountId } -> accountMissingText
                                rule == null -> dayRangeText
                                else -> null
                            }
                            if (validationMessage == null && rule != null) {
                                onSave(
                                    type,
                                    title,
                                    note.ifBlank { null },
                                    amount,
                                    selectedAccountId,
                                    selectedCategoryId.ifBlank { null },
                                    rule,
                                    startDate
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Radius.medium),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(stringResource(R.string.common_save))
                    }
                }
            }
        }
    }

    validationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { validationMessage = null },
            title = { Text(stringResource(R.string.recurring_warning_title), fontWeight = FontWeight.Bold) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { validationMessage = null }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }

    if (showDatePicker) {
        // DatePicker works in UTC midnight millis
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            dayOfMonthStr = startDate.dayOfMonth.toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.common_choose))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun accountIcon(accountType: String?) = when (accountType) {
    "BANK" -> Icons.Default.AccountBalance
    "E_WALLET" -> Icons.Default.AccountBalanceWallet
    else -> Icons.Default.Wallet
}
