package com.aekpdf.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aekpdf.app.core.ui.theme.AekPdfTheme
import com.aekpdf.app.features.history.HistoryScreen
import com.aekpdf.app.features.history.HistoryViewModel
import com.aekpdf.app.features.home.HomeScreen
import com.aekpdf.app.features.imagetopdf.ImageToPdfScreen
import com.aekpdf.app.features.imagetopdf.ImageToPdfViewModel
import com.aekpdf.app.features.viewer.PdfPreviewScreen
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

                LaunchedEffect(incomingUris) {
                    if (incomingUris.isNotEmpty()) {
                        imageToPdfViewModel.addImages(incomingUris)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (incomingUris.isNotEmpty()) "image_to_pdf" else "home"
                ) {
                    // Home Dashboard
                    composable("home") {
                        HomeScreen(
                            onNavigateToImageToPdf = {
                                navController.navigate("image_to_pdf")
                            },
                            onNavigateToHistory = {
                                navController.navigate("history")
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
