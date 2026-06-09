package com.hitunguang.feature.category.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hitunguang.feature.category.domain.model.Category

@Composable
fun CategoryFormDialog(
    category: Category?,
    defaultType: String,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, icon: String?, isPinned: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var type by remember { mutableStateOf(category?.categoryType ?: defaultType) }
    var icon by remember { mutableStateOf(category?.icon ?: "") }
    var isPinned by remember { mutableStateOf(category?.isPinned ?: false) }
    var nameError by remember { mutableStateOf<String?>(null) }

    val isEditMode = category != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditMode) "Edit Kategori" else "Tambah Kategori")
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.isBlank()) "Nama kategori tidak boleh kosong" else null
                    },
                    label = { Text("Nama Kategori *") },
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Hanya izinkan ubah tipe jika bukan mengedit kategori bawaan (default)
                if (category == null || !category.isDefault) {
                    Column {
                        Text("Tipe Kategori", style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = type == "INCOME",
                                onClick = { type = "INCOME" }
                            )
                            Text("Pemasukan")

                            Spacer(modifier = Modifier.width(16.dp))

                            RadioButton(
                                selected = type == "EXPENSE",
                                onClick = { type = "EXPENSE" }
                            )
                            Text("Pengeluaran")
                        }
                    }
                }

                Column {
                    Text("Ikon Kategori *", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 48.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CategoryIconHelper.availableIcons.size) { index ->
                            val (iconName, label) = CategoryIconHelper.availableIcons[index]
                            val isSelected = icon == iconName || (icon.isBlank() && iconName == "category")
                            androidx.compose.material3.IconButton(
                                onClick = { icon = iconName },
                                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
                                ),
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = CategoryIconHelper.getIconByName(iconName),
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it }
                    )
                    Text("Sematkan Kategori (Pin)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Nama kategori tidak boleh kosong"
                    } else {
                        onSave(name, type, icon.ifBlank { null }, isPinned)
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
