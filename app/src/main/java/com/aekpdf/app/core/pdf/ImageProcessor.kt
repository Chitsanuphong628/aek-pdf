package com.aekpdf.app.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.aekpdf.app.core.model.ExportQuality
import com.aekpdf.app.core.model.PageFilter
import java.io.InputStream
import kotlin.math.max

object ImageProcessor {

    private fun openStream(context: Context, uri: Uri): InputStream? {
        val uriStr = uri.toString()
        return try {
            if (uri.scheme == "file" || uriStr.startsWith("file:/")) {
                val rawPath = uri.path ?: uriStr.removePrefix("file://")
                val file = java.io.File(rawPath)
                java.io.FileInputStream(file)
            } else {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null && uri.path != null) {
                    val file = java.io.File(uri.path!!)
                    if (file.exists()) java.io.FileInputStream(file) else null
                } else {
                    stream
                }
            }
        } catch (_: Exception) {
            try {
                val rawPath = uri.path ?: uriStr.removePrefix("file://")
                val file = java.io.File(rawPath)
                if (file.exists()) java.io.FileInputStream(file) else null
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Decodes and optimizes a bitmap from a content Uri safely without throwing OutOfMemoryError.
     * Respects EXIF orientation, user rotation, quality constraints, and color filters.
     */
    fun decodeAndOptimize(
        context: Context,
        uri: Uri,
        userRotationDegrees: Int,
        filter: PageFilter,
        quality: ExportQuality
    ): Bitmap? {
        // Step 1: Read bounds and EXIF orientation without loading bitmap pixels into RAM
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        val boundsStream = openStream(context, uri) ?: return null
        boundsStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val rawWidth = options.outWidth
        val rawHeight = options.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        // Step 2: Read EXIF orientation from the image stream
        var exifRotation = 0
        try {
            openStream(context, uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                exifRotation = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {
            exifRotation = 0
        }

        // Step 3: Calculate safe inSampleSize
        val maxTargetDim = quality.maxDimension
        var sampleSize = 1
        val maxRawDim = max(rawWidth, rawHeight)
        while ((maxRawDim / (sampleSize * 2)) >= maxTargetDim) {
            sampleSize *= 2
        }

        // Step 4: Decode sampled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decodeStream = openStream(context, uri) ?: return null
        val sampledBitmap = decodeStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        // Step 5: Apply transformations (Exif + User Rotation)
        val totalRotation = (exifRotation + userRotationDegrees) % 360
        val rotatedBitmap = if (totalRotation != 0) {
            val matrix = Matrix().apply { postRotate(totalRotation.toFloat()) }
            val transformed = Bitmap.createBitmap(
                sampledBitmap, 0, 0,
                sampledBitmap.width, sampledBitmap.height,
                matrix, true
            )
            if (transformed != sampledBitmap) {
                sampledBitmap.recycle()
            }
            transformed
        } else {
            sampledBitmap
        }

        // Step 6: Apply Filters (B&W or Grayscale)
        return when (filter) {
            PageFilter.ORIGINAL -> rotatedBitmap
            PageFilter.GRAYSCALE -> applyGrayscale(rotatedBitmap)
            PageFilter.BLACK_AND_WHITE -> applyHighContrastDocScan(rotatedBitmap)
        }
    }

    private fun applyGrayscale(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        src.recycle()
        return dest
    }

    private fun applyHighContrastDocScan(src: Bitmap): Bitmap {
        // High contrast document filter: Converts to grayscale and enhances text contrast
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix()
        cm.setSaturation(0f)

        // Increase contrast: contrast = 1.6f, brightness shift = -40f
        val contrast = 1.6f
        val brightness = -30f
        val contrastMatrix = floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )
        cm.postConcat(ColorMatrix(contrastMatrix))

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        src.recycle()
        return dest
    }
}
