package com.aekpdf.app.core.scanner

import android.app.Activity
import android.content.IntentSender
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

object DocumentScannerHelper {

    /**
     * Builds options with SCANNER_MODE_FULL:
     * - Automatic edge detection (AI)
     * - Automatic perspective warp / deskew
     * - Auto-clean, shadow removal
     * - Filter modes: Original, Color Enhanced (High Contrast), Grayscale, Black & White
     */
    fun createScannerOptions(): GmsDocumentScannerOptions {
        return GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }

    fun startScan(
        activity: Activity,
        onSuccess: (IntentSender) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val scanner = GmsDocumentScanning.getClient(createScannerOptions())
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                onSuccess(intentSender)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
