package com.hitunguang

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import com.hitunguang.feature.category.presentation.CategoryListScreen
import com.hitunguang.feature.transfer.presentation.TransferHistoryScreen
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.hitunguang.core.designsystem.theme.HitungUangTheme
import com.hitunguang.feature.account.presentation.AccountListScreen
import com.hitunguang.feature.dashboard.presentation.DashboardScreen
import com.hitunguang.feature.onboarding.domain.model.UserProfile
import com.hitunguang.feature.onboarding.domain.repository.UserProfileRepository
import com.hitunguang.feature.onboarding.presentation.OnboardingScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.material3.RadioButton
import com.hitunguang.feature.settings.domain.model.AppSettings
import androidx.compose.material.icons.filled.Palette
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
            val appSettings by settingsRepository.getAppSettings().collectAsState(initial = null)
            val isDarkTheme = when (appSettings?.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            HitungUangTheme(darkTheme = isDarkTheme) {
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
                                MainAppScreen(
                                    settingsRepository = settingsRepository,
                                    appSettings = appSettings
                                )
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
fun MainAppScreen(
    settingsRepository: SettingsRepository,
    appSettings: AppSettings?
) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }
    var showScanPlaceholder by rememberSaveable { mutableStateOf(false) }
    var showSettingsPlaceholder by rememberSaveable { mutableStateOf(false) }
    var showThemeSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showCategoryListScreen by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var showSearchScreen by rememberSaveable { mutableStateOf(false) }
    var showRecycleBinScreen by rememberSaveable { mutableStateOf(false) }
    var showSecuritySettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showNotificationSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showBackupScreen by rememberSaveable { mutableStateOf(false) }
    var showScanScreen by rememberSaveable { mutableStateOf(false) }
    var showReviewScreen by rememberSaveable { mutableStateOf(false) }
    var reviewImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var reviewOcrRawText by rememberSaveable { mutableStateOf("") }
    var showReceiptArchiveScreen by rememberSaveable { mutableStateOf(false) }
    var showTransferHistoryScreen by rememberSaveable { mutableStateOf(false) }

    // Scoped to activity / parent level to allow quick add pre-population
    val transactionViewModel: TransactionViewModel = hiltViewModel()

    if (showSearchScreen) {
        SearchScreen(onBack = { showSearchScreen = false })
    } else if (showCategoryListScreen) {
        CategoryListScreen(onBack = { showCategoryListScreen = false })
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
    } else if (showTransferHistoryScreen) {
        TransferHistoryScreen(
            onBack = { showTransferHistoryScreen = false }
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
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        MainTab.DASHBOARD -> {
                            DashboardScreen(
                                onNavigateToTransactions = { currentTab = MainTab.TRANSACTIONS },
                                onNavigateToBudgets = { currentTab = MainTab.BUDGETS },
                                onSettingsClick = { showSettingsPlaceholder = true },
                                onAddTransactionClick = { type ->
                                    currentTab = MainTab.TRANSACTIONS
                                    transactionViewModel.showCreateDialog(
                                        initialType = type
                                    )
                                },
                                onScanClick = {
                                    showScanScreen = true
                                }
                            )
                        }
                        MainTab.TRANSACTIONS -> {
                            TransactionListScreen(
                                onSearchClick = { showSearchScreen = true },
                                onManageCategoriesClick = { showCategoryListScreen = true },
                                viewModel = transactionViewModel
                            )
                        }
                        MainTab.ACCOUNTS -> {
                            AccountListScreen(
                                onNavigateToTransferHistory = { showTransferHistoryScreen = true }
                            )
                        }
                        MainTab.BUDGETS -> {
                            BudgetListScreen()
                        }
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
                            showThemeSettingsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengaturan Tampilan (Theme)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                            showCategoryListScreen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kelola Kategori", fontWeight = FontWeight.Bold)
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

    if (showThemeSettingsDialog) {
        val currentTheme = appSettings?.themeMode ?: "SYSTEM"
        AlertDialog(
            onDismissRequest = { showThemeSettingsDialog = false },
            title = { Text("Pilih Tema Tampilan", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(
                        "SYSTEM" to "Ikuti Sistem",
                        "LIGHT" to "Terang (Light)",
                        "DARK" to "Gelap (Dark)"
                    ).forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val settings = appSettings ?: AppSettings(
                                            id = "app_settings",
                                            themeMode = "SYSTEM",
                                            hideBalance = false,
                                            receiptAutoDeleteDays = 30,
                                            dashboardPeriod = "WEEKLY",
                                            createdAt = System.currentTimeMillis(),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        settingsRepository.saveAppSettings(settings.copy(themeMode = mode, updatedAt = System.currentTimeMillis()))
                                    }
                                    showThemeSettingsDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = currentTheme == mode,
                                onClick = {
                                    scope.launch {
                                        val settings = appSettings ?: AppSettings(
                                            id = "app_settings",
                                            themeMode = "SYSTEM",
                                            hideBalance = false,
                                            receiptAutoDeleteDays = 30,
                                            dashboardPeriod = "WEEKLY",
                                            createdAt = System.currentTimeMillis(),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        settingsRepository.saveAppSettings(settings.copy(themeMode = mode, updatedAt = System.currentTimeMillis()))
                                    }
                                    showThemeSettingsDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeSettingsDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}


