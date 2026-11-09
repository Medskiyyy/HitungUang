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
import androidx.compose.ui.res.stringResource
import com.hitunguang.R
import androidx.annotation.StringRes

data class TutorialPage(
    @StringRes val title: Int,
    @StringRes val description: Int,
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
                title = R.string.onboarding_tutorial_home_title,
                description = R.string.onboarding_tutorial_home_desc,
                icon = Icons.Default.Dashboard
            ),
            TutorialPage(
                title = R.string.onboarding_tutorial_quick_title,
                description = R.string.onboarding_tutorial_quick_desc,
                icon = Icons.Default.ReceiptLong
            ),
            TutorialPage(
                title = R.string.onboarding_tutorial_scan_title,
                description = R.string.onboarding_tutorial_scan_desc,
                icon = Icons.Default.QrCodeScanner
            ),
            TutorialPage(
                title = R.string.onboarding_tutorial_budget_title,
                description = R.string.onboarding_tutorial_budget_desc,
                icon = Icons.Default.BarChart
            ),
            TutorialPage(
                title = R.string.onboarding_tutorial_backup_title,
                description = R.string.onboarding_tutorial_backup_desc,
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
                text = stringResource(R.string.onboarding_tutorial_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = stringResource(R.string.onboarding_tutorial_progress, currentPageIndex + 1, pages.size),
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
                    contentDescription = stringResource(page.title),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.huge))

            Text(
                text = stringResource(page.title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = stringResource(page.description),
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
                    if (currentPageIndex > 0) stringResource(R.string.onboarding_tutorial_previous) else stringResource(R.string.common_back)
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
                    if (currentPageIndex == pages.lastIndex) stringResource(R.string.common_done) else stringResource(R.string.common_next)
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
