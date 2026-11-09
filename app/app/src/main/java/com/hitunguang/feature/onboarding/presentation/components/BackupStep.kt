package com.hitunguang.feature.onboarding.presentation.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun BackupStep(
    backupUri: String?,
    onBackupUriSelected: (String?) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                onBackupUriSelected(uri.toString())
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Cadangkan Data",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Data kamu cuma ada di HP ini. Pilih folder buat cadangan otomatis tiap minggu, biar aman kalau aplikasinya kehapus. Lebih aman lagi kalau foldernya kamu salin ke Drive atau PC.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { folderPickerLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (backupUri != null) "Ubah folder cadangan" else "Pilih folder cadangan")
            }

            if (backupUri != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Folder terpilih:",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = backupUri,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Folder belum dipilih, jadi cadangan otomatis mati.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
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
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Button(
                onClick = onNextClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(if (backupUri != null) "Lanjut" else "Lewati")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BackupStepPreview() {
    MaterialTheme {
        BackupStep(
            backupUri = null,
            onBackupUriSelected = {},
            onNextClick = {},
            onBackClick = {}
        )
    }
}
