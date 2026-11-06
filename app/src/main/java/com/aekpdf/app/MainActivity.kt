package com.aekpdf.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aekpdf.app.core.scanner.DocumentScannerHelper
import com.aekpdf.app.core.ui.theme.AekPdfTheme
import com.aekpdf.app.features.camera.RapidCameraScreen
import com.aekpdf.app.features.history.HistoryScreen
import com.aekpdf.app.features.history.HistoryViewModel
import com.aekpdf.app.features.home.HomeScreen
import com.aekpdf.app.features.imagetopdf.ImageToPdfScreen
import com.aekpdf.app.features.imagetopdf.ImageToPdfViewModel
import com.aekpdf.app.features.viewer.PdfPreviewScreen
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val incomingUris = extractUrisFromIntent(intent)

        setContent {
            AekPdfTheme {
                val navController = rememberNavController()
                val imageToPdfViewModel: ImageToPdfViewModel = viewModel()
                val context = LocalContext.current
                val activity = context as? Activity

                LaunchedEffect(incomingUris) {
                    if (incomingUris.isNotEmpty()) {
                        imageToPdfViewModel.addImages(incomingUris)
                    }
                }

                // Photo picker launcher for Home
                val homePhotoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia()
                ) { uris: List<Uri> ->
                    if (uris.isNotEmpty()) {
                        imageToPdfViewModel.addImages(uris)
                        navController.navigate("image_to_pdf")
                    }
                }

                // PDF picker launcher for Home (Merge PDFs)
                val homePdfPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments()
                ) { uris: List<Uri> ->
                    if (uris.isNotEmpty()) {
                        uris.forEach { imageToPdfViewModel.addPagesFromPdf(it) }
                        navController.navigate("image_to_pdf")
                    }
                }

                // Scanner launcher for Home
                val homeScannerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                        val scannedUris = scanningResult?.pages?.map { it.imageUri } ?: emptyList()
                        if (scannedUris.isNotEmpty()) {
                            imageToPdfViewModel.addImages(scannedUris)
                            navController.navigate("image_to_pdf")
                        }
                    }
                }

                val launchCameraScanner: () -> Unit = {
                    if (activity != null) {
                        DocumentScannerHelper.startScan(
                            activity = activity,
                            onSuccess = { intentSender ->
                                homeScannerLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            },
                            onFailure = {
                                homePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    } else {
                        homePhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (incomingUris.isNotEmpty()) "image_to_pdf" else "home"
                ) {
                    // Home Dashboard
                    composable("home") {
                        HomeScreen(
                            onLaunchRapidCamera = {
                                navController.navigate("rapid_camera")
                            },
                            onScanWithCamera = launchCameraScanner,
                            onPickImages = {
                                homePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onPickPdfs = {
                                homePdfPickerLauncher.launch(arrayOf("application/pdf"))
                            },
                            onNavigateToHistory = {
                                navController.navigate("history")
                            }
                        )
                    }

                    // Rapid Continuous Multi-Page Camera
                    composable("rapid_camera") {
                        RapidCameraScreen(
                            onDismiss = {
                                navController.popBackStack()
                            },
                            onComplete = { uris ->
                                if (uris.isNotEmpty()) {
                                    imageToPdfViewModel.addImages(uris)
                                    navController.navigate("image_to_pdf") {
                                        popUpTo("home")
                                    }
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    // Image to PDF Conversion Screen
                    composable("image_to_pdf") {
                        ImageToPdfScreen(
                            viewModel = imageToPdfViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToPreview = { filePath ->
                                val encoded = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
                                navController.navigate("preview/$encoded")
                            },
                            onLaunchRapidCamera = {
                                navController.navigate("rapid_camera")
                            }
                        )
                    }

                    // PDF Preview and Sharing Screen
                    composable(
                        route = "preview/{encodedFilePath}",
                        arguments = listOf(navArgument("encodedFilePath") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encoded = backStackEntry.arguments?.getString("encodedFilePath") ?: ""
                        val decodedPath = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())

                        PdfPreviewScreen(
                            filePath = decodedPath,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }

                    // Document History Screen
                    composable("history") {
                        val viewModel: HistoryViewModel = viewModel()
                        HistoryScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onSelectPdf = { filePath ->
                                val encoded = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
                                navController.navigate("preview/$encoded")
                            }
                        )
                    }
                }
            }
        }
    }

    private fun extractUrisFromIntent(intent: Intent?): List<Uri> {
        val result = mutableListOf<Uri>()
        if (intent == null) return result

        if (intent.getBooleanExtra("load_demo", false)) {
            cacheDir.listFiles()?.sortedBy { it.name }?.forEach { file ->
                if (file.name.startsWith("sample") && (file.name.endsWith(".png") || file.name.endsWith(".jpg"))) {
                    val uri = Uri.fromFile(file)
                    result.add(uri)
                }
            }
            if (result.isNotEmpty()) return result
        }

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let { result.add(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.let { result.addAll(it) }
            }
        }
        return result
    }
}
