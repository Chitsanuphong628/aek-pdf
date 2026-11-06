package com.aekpdf.app.core.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.aekpdf.app.core.model.DocumentPage
import com.aekpdf.app.core.model.PageFilter
import com.aekpdf.app.core.model.PageFormat
import com.aekpdf.app.core.model.PdfExportConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object StreamingPdfCreator {

    /**
     * Converts a list of document image pages into a watermark-free PDF.
     * Uses a streaming single-bitmap-at-a-time pipeline to completely prevent OutOfMemoryError.
     */
    suspend fun createPdf(
        context: Context,
        pages: List<DocumentPage>,
        config: PdfExportConfig,
        outputFile: File,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No pages provided"))
        }

        val pdfDocument = PdfDocument()
        val totalPages = pages.size

        try {
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            var renderedPages = 0

            pages.forEachIndexed { index, page ->
                onProgress(index + 1, totalPages)

                // Effective filter: individual page filter takes precedence if set, otherwise config filter
                val activeFilter = if (page.filter != PageFilter.ORIGINAL) page.filter else config.filter

                val bitmap = ImageProcessor.decodeAndOptimize(
                    context = context,
                    uri = page.uri,
                    userRotationDegrees = page.rotationDegrees,
                    filter = activeFilter,
                    quality = config.quality
                ) ?: return@forEachIndexed

                val isLandscape = bitmap.width > bitmap.height

                val (pageWidth, pageHeight) = when (config.pageFormat) {
                    PageFormat.A4 -> {
                        if (isLandscape) {
                            Pair(PageFormat.A4.heightPt, PageFormat.A4.widthPt) // 842 x 595
                        } else {
                            Pair(PageFormat.A4.widthPt, PageFormat.A4.heightPt) // 595 x 842
                        }
                    }
                    PageFormat.AUTO -> {
                        // Standard PDF points (72 points per inch). Convert sampled image aspect ratio safely
                        val maxTargetPt = 842f
                        val imageMaxDim = kotlin.math.max(bitmap.width, bitmap.height).toFloat()
                        val autoScale = min(1.0f, maxTargetPt / imageMaxDim)
                        Pair((bitmap.width * autoScale).toInt().coerceAtLeast(200),
                             (bitmap.height * autoScale).toInt().coerceAtLeast(200))
                    }
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                val margin = config.marginPt
                val printableWidth = (pageWidth - 2 * margin).coerceAtLeast(10f)
                val printableHeight = (pageHeight - 2 * margin).coerceAtLeast(10f)

                val scale = min(printableWidth / bitmap.width.toFloat(), printableHeight / bitmap.height.toFloat())
                val destWidth = bitmap.width * scale
                val destHeight = bitmap.height * scale

                val left = margin + (printableWidth - destWidth) / 2f
                val top = margin + (printableHeight - destHeight) / 2f

                val destRect = RectF(left, top, left + destWidth, top + destHeight)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDocument.finishPage(pdfPage)
                renderedPages++

                // Reclaim bitmap memory immediately to guarantee low memory footprint
                bitmap.recycle()
            }

            if (renderedPages == 0) {
                pdfDocument.close()
                outputFile.delete()
                return@withContext Result.failure(IllegalStateException("ไม่สามารถเปิดอ่านรูปภาพเพื่อสร้าง PDF ได้"))
            }

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
                out.flush()
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure(e)
        } finally {
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
        }
    }
}
