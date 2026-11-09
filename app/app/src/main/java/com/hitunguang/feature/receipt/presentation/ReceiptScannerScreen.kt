package com.hitunguang.feature.receipt.presentation

import android.content.Context
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    onBack: () -> Unit,
    onNavigateToReview: (Uri, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiptScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var tempPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.deleteStaleTempFiles()
    }

    val currentOnNavigateToReview by rememberUpdatedState(onNavigateToReview)
    LaunchedEffect(uiState.rawText, uiState.imageUri, uiState.scanQuality, uiState.isLowQualityAccepted) {
        val rawText = uiState.rawText
        val imageUri = uiState.imageUri
        // A LOW quality scan waits for the user to retake or explicitly continue.
        val canContinue = uiState.scanQuality != ScanQuality.LOW || uiState.isLowQualityAccepted
        if (rawText != null && imageUri != null && canContinue) {
            currentOnNavigateToReview(imageUri, rawText)
            viewModel.clearAll()
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        tempPhotoUri?.let { uri ->
            if (success) {
                viewModel.setImageUri(uri)
            } else {
                viewModel.discardTempPhoto(uri)
            }
        }
        tempPhotoUri = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = getTempPhotoUri(context)
            if (uri != null) {
                tempPhotoUri = uri
                takePictureLauncher.launch(uri)
            } else {
                viewModel.setErrorMessage("Gagal menyiapkan foto. Coba lagi, ya.")
            }
        } else {
            viewModel.setErrorMessage("Izinkan akses kamera dulu buat memotret struk.")
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setPickedImageUri(uri)
        }
    }

    val onBackAndDiscard = {
        viewModel.reset()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pindai Struk", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackAndDiscard) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (uiState.imageUri != null || uiState.rawText != null) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Ulangi")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Receipt Image Preview or Picker Area
            if (uiState.imageUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Pilih atau ambil foto struk belanja",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val permissionCheck = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    )
                                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                        val uri = getTempPhotoUri(context)
                                        if (uri != null) {
                                            tempPhotoUri = uri
                                            takePictureLauncher.launch(uri)
                                        } else {
                                            viewModel.setErrorMessage("Gagal menyiapkan foto. Coba lagi, ya.")
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kamera")
                            }

                            FilledTonalButton(
                                onClick = {
                                    pickMediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Galeri")
                            }
                        }
                    }
                }
            } else {
                // Image chosen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = uiState.imageUri,
                        contentDescription = "Pratinjau struk",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (uiState.rawText == null && !uiState.isScanning) {
                    Button(
                        onClick = { viewModel.startScan() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Baca struk")
                    }
                }
            }

            // Low quality scan warning
            if (uiState.scanQuality == ScanQuality.LOW) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Foto struknya kurang jelas, hasil bacaan bisa kurang akurat. Coba foto ulang di tempat yang lebih terang.",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.reset() }) {
                                Text("Foto ulang")
                            }
                            TextButton(onClick = { viewModel.acceptLowQuality() }) {
                                Text("Lanjut aja", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Scanning state / Loader
            if (uiState.isScanning) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Lagi baca struk…",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Error display
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (uiState.imageUri != null && uiState.rawText == null && !uiState.isScanning) {
                            TextButton(
                                onClick = { viewModel.startScan() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Coba lagi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }


        }
    }
}

private fun getTempPhotoUri(context: Context): Uri? {
    return try {
        // Deleted after save, reset, back or a cancelled capture; leftovers are swept
        // by ReceiptFileManager.deleteStaleTempFiles.
        val tempFile = File.createTempFile("camera_photo_", ".jpg", context.cacheDir)
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.hitunguang.fileprovider",
            tempFile
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
