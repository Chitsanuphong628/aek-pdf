package com.aekpdf.app.core.model

import android.net.Uri
import java.util.UUID

enum class PageFilter(val displayNameRes: Int) {
    ORIGINAL(com.aekpdf.app.R.string.filter_original),
    BLACK_AND_WHITE(com.aekpdf.app.R.string.filter_black_white),
    GRAYSCALE(com.aekpdf.app.R.string.filter_grayscale)
}

enum class PageFormat(val displayNameRes: Int, val widthPt: Int, val heightPt: Int) {
    A4(com.aekpdf.app.R.string.page_size_a4, 595, 842),
    AUTO(com.aekpdf.app.R.string.page_size_auto, 0, 0)
}

enum class ExportQuality(val displayNameRes: Int, val qualityPercent: Int, val maxDimension: Int) {
    HIGH(com.aekpdf.app.R.string.quality_high, 92, 2400),
    MEDIUM(com.aekpdf.app.R.string.quality_medium, 75, 1600),
    SMALL(com.aekpdf.app.R.string.quality_small, 55, 1200)
}

data class DocumentPage(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val rotationDegrees: Int = 0,
    val filter: PageFilter = PageFilter.ORIGINAL
)

data class PdfExportConfig(
    val fileName: String,
    val pageFormat: PageFormat = PageFormat.A4,
    val quality: ExportQuality = ExportQuality.MEDIUM,
    val filter: PageFilter = PageFilter.ORIGINAL,
    val marginPt: Float = 0f
)
