package com.aekpdf.app

import com.aekpdf.app.core.model.ExportQuality
import com.aekpdf.app.core.model.PageFilter
import com.aekpdf.app.core.model.PageFormat
import com.aekpdf.app.core.model.PdfExportConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsAndStorageTest {

    @Test
    fun testPageFormatDimensions() {
        // Verify standard A4 dimensions in PDF points (72 DPI)
        assertEquals(595, PageFormat.A4.widthPt)
        assertEquals(842, PageFormat.A4.heightPt)

        // Verify Auto dimensions
        assertEquals(0, PageFormat.AUTO.widthPt)
        assertEquals(0, PageFormat.AUTO.heightPt)
    }

    @Test
    fun testExportQualityParameters() {
        assertTrue(ExportQuality.HIGH.qualityPercent > ExportQuality.MEDIUM.qualityPercent)
        assertTrue(ExportQuality.MEDIUM.qualityPercent > ExportQuality.SMALL.qualityPercent)

        assertTrue(ExportQuality.HIGH.maxDimension > ExportQuality.MEDIUM.maxDimension)
        assertTrue(ExportQuality.MEDIUM.maxDimension > ExportQuality.SMALL.maxDimension)
    }

    @Test
    fun testPdfExportConfigDefaults() {
        val config = PdfExportConfig(fileName = "Test_Document")
        assertEquals("Test_Document", config.fileName)
        assertEquals(PageFormat.A4, config.pageFormat)
        assertEquals(ExportQuality.MEDIUM, config.quality)
        assertEquals(PageFilter.ORIGINAL, config.filter)
        assertEquals(0f, config.marginPt, 0.001f)
    }
}
