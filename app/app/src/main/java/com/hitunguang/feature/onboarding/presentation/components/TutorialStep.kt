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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hitunguang.core.designsystem.theme.Spacing

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
                description = "Pantau total saldo, ringkasan pengeluaran, grafik kategori, dan status budget bulanan secara real-time.",
                icon = Icons.Default.Dashboard
            ),
            TutorialPage(
                title = "Pencatatan Cepat",
                description = "Catat pengeluaran, pemasukan, dan transfer antar akun dengan mudah. Draft otomatis menyimpan input yang belum selesai.",
                icon = Icons.Default.ReceiptLong
            ),
            TutorialPage(
                title = "Pindai Struk (OCR)",
                description = "Cukup foto struk belanja, sistem akan membaca teks secara offline, mengekstrak item, dan menghitung total otomatis.",
                icon = Icons.Default.QrCodeScanner
            ),
            TutorialPage(
                title = "Manajemen Budget",
                description = "Atur batas pengeluaran per kategori. Dapatkan notifikasi jika mendekati atau melebihi batas.",
                icon = Icons.Default.BarChart
            ),
            TutorialPage(
                title = "Cadangan & Keamanan",
                description = "Data dienkripsi dan dicadangkan ke folder pilihan Anda. Lengkapi dengan PIN atau sidik jari.",
                icon = Icons.Default.Backup
            )
        )
    }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    val page = pages[currentPageIndex]

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
            Text(
                text = "Panduan Singkat",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = "${currentPageIndex + 1} dari ${pages.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with branded background circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = page.title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.huge))

            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.huge))

            // Page indicator dots with animated fill
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                pages.forEachIndexed { index, _ ->
                    val isSelected = index == currentPageIndex
                    val isPast = index < currentPageIndex
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isPast -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
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
                Text(
                    if (currentPageIndex > 0) "Sebelumnya" else "Kembali"
                )
            }

            Spacer(modifier = Modifier.width(Spacing.large))

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
                Text(
                    if (currentPageIndex == pages.lastIndex) "Selesai" else "Lanjut"
                )
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
