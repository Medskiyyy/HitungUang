package com.hitunguang

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.hitunguang.core.designsystem.theme.HitungUangTheme
import com.hitunguang.feature.account.presentation.AccountListScreen
import com.hitunguang.feature.dashboard.presentation.DashboardScreen
import com.hitunguang.feature.onboarding.domain.model.UserProfile
import com.hitunguang.feature.onboarding.domain.repository.UserProfileRepository
import com.hitunguang.feature.onboarding.presentation.OnboardingScreen
import com.hitunguang.feature.budget.presentation.BudgetListScreen
import com.hitunguang.feature.transaction.presentation.TransactionListScreen
import com.hitunguang.feature.transaction.presentation.SearchScreen
import android.net.Uri
import com.hitunguang.feature.receipt.presentation.ReceiptScannerScreen
import com.hitunguang.feature.receipt.presentation.ReceiptReviewScreen
import com.hitunguang.feature.receipt.presentation.ReceiptArchiveScreen
import com.hitunguang.feature.receipt.domain.usecase.AutoDeleteReceiptsUseCase
import androidx.compose.material.icons.filled.Receipt
import com.hitunguang.feature.transaction.presentation.TransactionViewModel
import com.hitunguang.feature.backup.presentation.BackupScreen
import com.hitunguang.feature.recyclebin.presentation.RecycleBinScreen
import com.hitunguang.feature.settings.presentation.SecurityViewModel
import com.hitunguang.feature.settings.presentation.components.LockScreen
import com.hitunguang.feature.settings.presentation.components.SecuritySettingsScreen
import com.hitunguang.feature.settings.presentation.components.NotificationSettingsScreen
import com.hitunguang.core.notification.NotificationHelper
import com.hitunguang.core.notification.NotificationScheduler
import com.hitunguang.feature.settings.domain.repository.SettingsRepository
import androidx.compose.material.icons.filled.Notifications
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    @Inject
    lateinit var autoDeleteReceiptsUseCase: AutoDeleteReceiptsUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            notificationHelper.createNotificationChannels()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        lifecycleScope.launch {
            try {
                autoDeleteReceiptsUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        lifecycleScope.launch {
            try {
                settingsRepository.getNotificationSettings().collect { settings ->
                    if (settings != null) {
                        notificationScheduler.scheduleAll(settings)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        setContent {
            HitungUangTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Collect state using Unit as loading indicator to prevent flashing
                    val profileState by userProfileRepository.getUserProfile().collectAsState(initial = Unit)

                    when (val state = profileState) {
                        is Unit -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        null -> {
                            OnboardingScreen(
                                onOnboardingFinished = {
                                    // The database write will trigger Room flow to emit a UserProfile,
                                    // automatically updating this when branch executes.
                                }
                            )
                        }
                        is UserProfile -> {
                            val securityViewModel: SecurityViewModel = hiltViewModel()
                            val securityState by securityViewModel.uiState.collectAsState()

                            if (securityState.isAppLocked) {
                                LockScreen(
                                    onBiometricPromptTrigger = { showBiometricPrompt(securityViewModel) },
                                    viewModel = securityViewModel
                                )
                            } else {
                                MainAppScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(viewModel: SecurityViewModel) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(this)
        val biometricPrompt = androidx.biometric.BiometricPrompt(
            this,
            executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.triggerBiometricUnlock()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autentikasi Biometrik")
            .setSubtitle("Gunakan sidik jari Anda untuk masuk")
            .setNegativeButtonText("Gunakan PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

enum class MainTab {
    DASHBOARD, TRANSACTIONS, ACCOUNTS, BUDGETS
}

@Composable
fun MainAppScreen() {
    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var showScanPlaceholder by remember { mutableStateOf(false) }
    var showSettingsPlaceholder by remember { mutableStateOf(false) }

    var showSearchScreen by remember { mutableStateOf(false) }
    var showRecycleBinScreen by remember { mutableStateOf(false) }
    var showSecuritySettingsScreen by remember { mutableStateOf(false) }
    var showNotificationSettingsScreen by remember { mutableStateOf(false) }
    var showBackupScreen by remember { mutableStateOf(false) }
    var showScanScreen by remember { mutableStateOf(false) }
    var showReviewScreen by remember { mutableStateOf(false) }
    var reviewImageUri by remember { mutableStateOf<Uri?>(null) }
    var reviewOcrRawText by remember { mutableStateOf("") }
    var showReceiptArchiveScreen by remember { mutableStateOf(false) }

    // Scoped to activity / parent level to allow quick add pre-population
    val transactionViewModel: TransactionViewModel = hiltViewModel()

    if (showSearchScreen) {
        SearchScreen(onBack = { showSearchScreen = false })
    } else if (showRecycleBinScreen) {
        RecycleBinScreen(onBack = { showRecycleBinScreen = false })
    } else if (showSecuritySettingsScreen) {
        SecuritySettingsScreen(onBack = { showSecuritySettingsScreen = false })
    } else if (showNotificationSettingsScreen) {
        NotificationSettingsScreen(onBack = { showNotificationSettingsScreen = false })
    } else if (showBackupScreen) {
        BackupScreen(onNavigateBack = { showBackupScreen = false })
    } else if (showScanScreen) {
        ReceiptScannerScreen(
            onBack = { showScanScreen = false },
            onNavigateToReview = { uri, text ->
                reviewImageUri = uri
                reviewOcrRawText = text
                showReviewScreen = true
                showScanScreen = false
            }
        )
    } else if (showReviewScreen) {
        val uri = reviewImageUri
        if (uri != null) {
            ReceiptReviewScreen(
                imageUri = uri,
                ocrRawText = reviewOcrRawText,
                onBack = {
                    showReviewScreen = false
                    showScanScreen = true
                },
                onSaveSuccess = {
                    showReviewScreen = false
                    showReceiptArchiveScreen = true
                }
            )
        }
    } else if (showReceiptArchiveScreen) {
        ReceiptArchiveScreen(
            onBack = { showReceiptArchiveScreen = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == MainTab.DASHBOARD,
                        onClick = { currentTab = MainTab.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.TRANSACTIONS,
                        onClick = { currentTab = MainTab.TRANSACTIONS },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transaksi") },
                        label = { Text("Transaksi") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { showScanScreen = true },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan") },
                        label = { Text("Scan") }
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.ACCOUNTS,
                        onClick = { currentTab = MainTab.ACCOUNTS },
                        icon = { Icon(Icons.Default.Wallet, contentDescription = "Akun") },
                        label = { Text("Akun") }
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.BUDGETS,
                        onClick = { currentTab = MainTab.BUDGETS },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Budget") },
                        label = { Text("Budget") }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    MainTab.DASHBOARD -> {
                        DashboardScreen(
                            onNavigateToTransactions = { currentTab = MainTab.TRANSACTIONS },
                            onNavigateToBudgets = { currentTab = MainTab.BUDGETS },
                            onSettingsClick = { showSettingsPlaceholder = true },
                            onQuickAddClick = { categoryId, transactionType ->
                                currentTab = MainTab.TRANSACTIONS
                                transactionViewModel.showCreateDialog(
                                    initialCategoryId = categoryId,
                                    initialType = transactionType
                                )
                            }
                        )
                    }
                    MainTab.TRANSACTIONS -> {
                        TransactionListScreen(
                            onSearchClick = { showSearchScreen = true },
                            viewModel = transactionViewModel
                        )
                    }
                    MainTab.ACCOUNTS -> {
                        AccountListScreen()
                    }
                    MainTab.BUDGETS -> {
                        BudgetListScreen()
                    }
                }
            }
        }
    }

    if (showScanPlaceholder) {
        AlertDialog(
            onDismissRequest = { showScanPlaceholder = false },
            title = { Text("Scan Struk (OCR)", fontWeight = FontWeight.Bold) },
            text = { Text("Fitur scan struk (OCR) secara offline akan segera hadir pada Milestone 20-22!") },
            confirmButton = {
                TextButton(onClick = { showScanPlaceholder = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showSettingsPlaceholder) {
        AlertDialog(
            onDismissRequest = { showSettingsPlaceholder = false },
            title = { Text("Pengaturan", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Pilih menu pengaturan di bawah ini:")
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            showSettingsPlaceholder = false
                            showSecuritySettingsScreen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengaturan Keamanan (PIN & Biometrik)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            showSettingsPlaceholder = false
                            showNotificationSettingsScreen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengaturan Notifikasi", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            showSettingsPlaceholder = false
                            showBackupScreen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup & Restore", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            showSettingsPlaceholder = false
                            showReceiptArchiveScreen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Arsip Struk Belanja", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            showSettingsPlaceholder = false
                            showRecycleBinScreen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Tempat Sampah", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsPlaceholder = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}


