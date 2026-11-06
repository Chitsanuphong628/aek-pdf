package com.aekpdf.app.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfStorageManager {

    /**
     * Generates a clean destination File for the new PDF document.
     */
    fun createOutputFile(context: Context, rawFileName: String?): File {
        val sanitized = if (!rawFileName.isNullOrBlank()) {
            val name = rawFileName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
            if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
        } else {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            "AEK_DOC_$timestamp.pdf"
        }

        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) 
            ?: File(context.filesDir, "documents")
        baseDir.mkdirs()

        return File(baseDir, sanitized)
    }

    /**
     * Obtains a secure content URI using FileProvider for sharing or viewing.
     */
    fun getShareableUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Creates an Intent to share the PDF to apps like LINE, Gmail, Google Drive, WhatsApp, etc.
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val uri = getShareableUri(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Creates an Intent to share directly to LINE (com.linecorp.vg) if installed.
     */
    fun createLineShareIntent(context: Context, file: File): Intent {
        val uri = getShareableUri(context, file)
        val linePackage = "com.linecorp.vg"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val isLineInstalled = try {
            context.packageManager.getPackageInfo(linePackage, 0)
            true
        } catch (_: Exception) {
            false
        }
        if (isLineInstalled) {
            intent.setPackage(linePackage)
        }
        return intent
    }

    /**
     * Creates an Intent to open and view the PDF in the user's default PDF viewer.
     */
    fun createOpenIntent(context: Context, file: File): Intent {
        val uri = getShareableUri(context, file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
