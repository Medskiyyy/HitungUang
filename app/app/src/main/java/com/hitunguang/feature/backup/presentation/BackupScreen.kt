package com.hitunguang.feature.backup.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Folder picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val resolver = context.contentResolver
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                resolver.takePersistableUriPermission(it, flags)
                uiState.backupFolderUri?.let { old ->
                    if (old != it.toString()) {
                        try {
                            resolver.releasePersistableUriPermission(Uri.parse(old), flags)
                        } catch (_: SecurityException) {
                        }
                    }
                }
                viewModel.onFolderSelected(it.toString())
            } catch (_: SecurityException) {
                viewModel.onFolderPermissionFailed()
            }
        }
    }

    // File picker for restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onRestoreFileSelected(it) }
    }

    // Show snackbar messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cadangan & Pemulihan",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- Backup Folder Card ---
                    BackupSectionCard(
                        icon = Icons.Outlined.Folder,
                        title = "Folder Cadangan",
                        iconTint = MaterialTheme.colorScheme.primary
                    ) {
                        if (uiState.backupFolderUri != null) {
                            Text(
                                text = uiState.backupFolderUri!!
                                    .substringAfterLast("%3A")
                                    .substringAfterLast("%2F")
                                    .ifBlank { uiState.backupFolderUri!! },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else {
                            Text(
                                text = "Belum ada folder dipilih",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (uiState.backupFolderUri != null) Icons.Default.FolderOpen else Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (uiState.backupFolderUri != null) "Ubah folder" else "Pilih folder"
                            )
                        }
                    }

                    // --- Last Backup Info ---
                    BackupSectionCard(
                        icon = Icons.Outlined.Schedule,
                        title = "Cadangan Terakhir",
                        iconTint = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = uiState.lastBackupAt?.let { formatTimestamp(it) } ?: "Belum pernah dicadangkan",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (uiState.lastBackupAt != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (uiState.lastBackupAt != null) FontWeight.Medium else FontWeight.Normal
                        )
                    }

                    // --- Auto Backup Settings ---
                    BackupSectionCard(
                        icon = Icons.Outlined.Autorenew,
                        title = "Cadangan Otomatis",
                        iconTint = MaterialTheme.colorScheme.tertiary
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cadangkan otomatis",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = uiState.autoBackupEnabled,
                                onCheckedChange = { viewModel.onAutoBackupToggled(it) }
                            )
                        }

                        AnimatedVisibility(
                            visible = uiState.autoBackupEnabled,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(
                                    text = "Frekuensi cadangan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                val frequencies = listOf(
                                    "DAILY" to "Harian",
                                    "WEEKLY" to "Mingguan"
                                )
                                frequencies.forEach { (value, label) ->
                                    FrequencyOption(
                                        label = label,
                                        selected = (if (uiState.backupFrequency == "REALTIME") "DAILY" else uiState.backupFrequency) == value,
                                        onClick = { viewModel.onFrequencyChanged(value) }
                                    )
                                    if (value != "WEEKLY") Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    // --- Manual Backup Button ---
                    Button(
                        onClick = { viewModel.onBackupNow() },
                        enabled = !uiState.isBackingUp && !uiState.isRestoring,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isBackingUp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Lagi mencadangkan…")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cadangkan sekarang", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // --- Restore Section ---
                    Divider()

                    BackupSectionCard(
                        icon = Icons.Outlined.Restore,
                        title = "Pulihkan dari Cadangan",
                        iconTint = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = "Pilih file cadangan (.zip) buat memulihkan data. Semua data di HP ini akan diganti dengan isi file cadangan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            },
                            enabled = !uiState.isBackingUp && !uiState.isRestoring,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            )
                        ) {
                            if (uiState.isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Lagi memulihkan…")
                            } else {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Pilih file cadangan", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }

            // Restore confirmation dialog
            if (uiState.showRestoreConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.onRestoreDismissed() },
                    icon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    title = { Text("Pulihkan dari cadangan?") },
                    text = {
                        Text(
                            "Semua data sekarang (transaksi, dompet, kategori) akan dihapus dan diganti dengan isi file cadangan. Aplikasi akan mulai ulang otomatis. Lanjut?"
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.onRestoreConfirmed() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Ya, pulihkan") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onRestoreDismissed() }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BackupSectionCard(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
fun FrequencyOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (selected) Modifier.background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) else Modifier
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("id", "ID"))
    return sdf.format(Date(millis))
}
