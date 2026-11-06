package com.aekpdf.app.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfPageExtractor {

    /**
     * Extracts all pages from an existing PDF file into temporary image files,
     * allowing them to be combined, reordered, and mixed with new camera scans.
     */
    suspend fun extractPagesFromPdf(context: Context, pdfUri: Uri): List<Uri> = withContext(Dispatchers.IO) {
        val extractedUris = mutableListOf<Uri>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@withContext emptyList()
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            val cacheDir = File(context.cacheDir, "extracted_pdf_pages").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                // Render at high crisp resolution (scale = 2x, 150-200 DPI equivalent)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val outFile = File(cacheDir, "page_${timestamp}_${i + 1}.jpg")
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                bitmap.recycle()

                extractedUris.add(outFile.toUri())
            }
        } catch (_: Exception) {
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }

        extractedUris
    }
}
