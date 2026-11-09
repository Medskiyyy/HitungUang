package com.hitunguang.feature.onboarding.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hitunguang.core.designsystem.theme.Spacing

@Composable
fun BudgetStep(
    budgetAmount: String,
    onBudgetAmountChange: (String) -> Unit,
    onNextClick: () -> Unit,
    onSkipClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.doubleLarge),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.medium))
                Text(
                    text = "Atur batas pengeluaran bulanan",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = "Tentukan batas pengeluaran bulananmu biar keuangan lebih terkontrol.\n\nLangkah ini boleh dilewati, bisa diatur nanti.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.huge))

            OutlinedTextField(
                value = budgetAmount,
                // Keep digits only (users may type "2.000.000"); dots are shown by the visual transformation
                onValueChange = { onBudgetAmountChange(it.filter(Char::isDigit).take(15)) },
                label = { Text("Anggaran bulanan (Rp)") },
                placeholder = { Text("Contoh: 2.000.000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandSeparatorTransformation,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Text(
                        text = "Rp",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = Spacing.large)
                    )
                }
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Kembali")
                }

                Spacer(modifier = Modifier.width(Spacing.large))

                Button(
                    onClick = onNextClick,
                    enabled = budgetAmount.isNotBlank() && budgetAmount.toLongOrNull() != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Simpan")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Explicit skip button — clearer than dynamically changing main button label
            OutlinedButton(
                onClick = onSkipClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    "Lewati dulu →",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

internal fun formatThousands(digits: String): String =
    digits.reversed().chunked(3).joinToString(".").reversed()

private object ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatThousands(text.text)
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var digits = 0
                formatted.forEachIndexed { index, c ->
                    if (c != '.') {
                        digits++
                        if (digits == offset) return index + 1
                    }
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int =
                formatted.take(offset).count { it != '.' }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetStepPreview() {
    MaterialTheme {
        BudgetStep(
            budgetAmount = "1500000",
            onBudgetAmountChange = {},
            onNextClick = {},
            onSkipClick = {},
            onBackClick = {}
        )
    }
}
