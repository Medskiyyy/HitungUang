package com.hitunguang.feature.onboarding.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class TutorialPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun TutorialStep(
    onFinishClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember {
        listOf(
            TutorialPage(
                title = "Dashboard Utama",
                description = "Lihat total saldo, ringkasan pengeluaran bulanan, chart kategori, serta ringkasan budget secara real-time.",
                icon = Icons.Default.Dashboard
            ),
            TutorialPage(
                title = "Pencatatan Transaksi",
                description = "Catat pengeluaran, pemasukan, dan transfer antar akun dengan cepat dan mudah dalam hitungan detik.",
                icon = Icons.Default.ReceiptLong
            ),
            TutorialPage(
                title = "Pindai Struk (OCR)",
                description = "Cukup foto struk belanjaan Anda, sistem akan secara otomatis memindai, mengenali teks, dan menjumlahkan item pengeluaran Anda.",
                icon = Icons.Default.QrCodeScanner
            ),
            TutorialPage(
                title = "Manajemen Budget",
                description = "Batas pengeluaran bulanan yang terkontrol. Aplikasi akan mengirim notifikasi saat pengeluaran mendekati batas limit.",
                icon = Icons.Default.BarChart
            ),
            TutorialPage(
                title = "Cadangan Otomatis",
                description = "Data Anda dienkripsi dan dicadangkan secara otomatis dalam format ZIP ke folder lokal pilihan Anda.",
                icon = Icons.Default.Backup
            )
        )
    }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    val page = pages[currentPageIndex]

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
                text = "Panduan Singkat",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = page.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentPageIndex)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    if (currentPageIndex > 0) {
                        currentPageIndex--
                    } else {
                        onBackClick()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Kembali")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Button(
                onClick = {
                    if (currentPageIndex < pages.lastIndex) {
                        currentPageIndex++
                    } else {
                        onFinishClick()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(if (currentPageIndex == pages.lastIndex) "Selesai" else "Lanjut")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TutorialStepPreview() {
    MaterialTheme {
        TutorialStep(
            onFinishClick = {},
            onBackClick = {}
        )
    }
}
