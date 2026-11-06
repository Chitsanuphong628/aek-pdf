package com.aekpdf.app.features.imagetopdf

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aekpdf.app.AekPdfApplication
import com.aekpdf.app.core.database.PdfHistoryEntity
import com.aekpdf.app.core.model.DocumentPage
import com.aekpdf.app.core.model.ExportQuality
import com.aekpdf.app.core.model.PageFilter
import com.aekpdf.app.core.model.PageFormat
import com.aekpdf.app.core.model.PdfExportConfig
import com.aekpdf.app.core.pdf.StreamingPdfCreator
import com.aekpdf.app.core.storage.PdfStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

data class ImageToPdfUiState(
    val pages: List<DocumentPage> = emptyList(),
    val config: PdfExportConfig = PdfExportConfig(
        fileName = "AEK_DOC_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
    ),
    val isGenerating: Boolean = false,
    val currentProgressPage: Int = 0,
    val totalProgressPages: Int = 0,
    val generatedFile: File? = null,
    val errorMessage: String? = null
)

class ImageToPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ImageToPdfUiState())
    val uiState: StateFlow<ImageToPdfUiState> = _uiState.asStateFlow()

    private val historyDao = (application as AekPdfApplication).database.pdfHistoryDao()

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val newPages = uris.map { uri -> DocumentPage(uri = uri) }
        _uiState.update { current ->
            current.copy(pages = current.pages + newPages)
        }
    }

    fun addPagesFromPdf(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val extractedUris = com.aekpdf.app.core.pdf.PdfPageExtractor.extractPagesFromPdf(context, uri)
            if (extractedUris.isNotEmpty()) {
                addImages(extractedUris)
            } else {
                _uiState.update { it.copy(errorMessage = "ไม่สามารถเปิดหรือดึงหน้าจากไฟล์ PDF ได้") }
            }
        }
    }

    fun applyThaiNamePreset(presetName: String) {
        val thaiLocale = Locale("th", "TH")
        val dateFormatted = SimpleDateFormat("ddMMM", thaiLocale).format(Date())
        val yearShort = (SimpleDateFormat("yy", thaiLocale).format(Date()).toIntOrNull() ?: 26)
        val fullPreset = "${presetName}_${dateFormatted}${yearShort}"
        updateFileName(fullPreset)
    }

    fun rotatePage(index: Int) {
        _uiState.update { current ->
            if (index !in current.pages.indices) return@update current
            val updatedPages = current.pages.toMutableList()
            val target = updatedPages[index]
            val newRotation = (target.rotationDegrees + 90) % 360
            updatedPages[index] = target.copy(rotationDegrees = newRotation)
            current.copy(pages = updatedPages)
        }
    }

    fun removePage(index: Int) {
        _uiState.update { current ->
            if (index !in current.pages.indices) return@update current
            val updatedPages = current.pages.toMutableList().apply { removeAt(index) }
            current.copy(pages = updatedPages)
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        _uiState.update { current ->
            if (fromIndex !in current.pages.indices || toIndex !in current.pages.indices || fromIndex == toIndex) {
                return@update current
            }
            val updatedPages = current.pages.toMutableList()
            val item = updatedPages.removeAt(fromIndex)
            updatedPages.add(toIndex, item)
            current.copy(pages = updatedPages)
        }
    }

    fun movePageUp(index: Int) {
        _uiState.update { current ->
            if (index <= 0 || index >= current.pages.size) return@update current
            val updatedPages = current.pages.toMutableList()
            Collections.swap(updatedPages, index, index - 1)
            current.copy(pages = updatedPages)
        }
    }

    fun movePageDown(index: Int) {
        _uiState.update { current ->
            if (index < 0 || index >= current.pages.size - 1) return@update current
            val updatedPages = current.pages.toMutableList()
            Collections.swap(updatedPages, index, index + 1)
            current.copy(pages = updatedPages)
        }
    }

    fun clearAll() {
        _uiState.update { it.copy(pages = emptyList(), generatedFile = null, errorMessage = null) }
    }

    fun updateFileName(name: String) {
        _uiState.update { current ->
            current.copy(config = current.config.copy(fileName = name))
        }
    }

    fun updatePageFormat(format: PageFormat) {
        _uiState.update { current ->
            current.copy(config = current.config.copy(pageFormat = format))
        }
    }

    fun updateQuality(quality: ExportQuality) {
        _uiState.update { current ->
            current.copy(config = current.config.copy(quality = quality))
        }
    }

    fun updateFilter(filter: PageFilter) {
        _uiState.update { current ->
            current.copy(config = current.config.copy(filter = filter))
        }
    }

    fun generatePdf(onSuccess: (File) -> Unit) {
        val currentState = _uiState.value
        val pages = currentState.pages
        if (pages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "กรุณาเลือกรูปภาพอย่างน้อย 1 ภาพ") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    currentProgressPage = 1,
                    totalProgressPages = pages.size,
                    errorMessage = null
                )
            }

            val context = getApplication<Application>()
            val outputFile = PdfStorageManager.createOutputFile(context, currentState.config.fileName)

            val result = StreamingPdfCreator.createPdf(
                context = context,
                pages = pages,
                config = currentState.config,
                outputFile = outputFile,
                onProgress = { current, total ->
                    _uiState.update {
                        it.copy(currentProgressPage = current, totalProgressPages = total)
                    }
                }
            )

            result.fold(
                onSuccess = { file ->
                    // Save record to local Room database
                    try {
                        historyDao.insert(
                            PdfHistoryEntity(
                                title = file.name,
                                filePath = file.absolutePath,
                                pageCount = pages.size,
                                fileSizeBytes = file.length()
                            )
                        )
                    } catch (_: Exception) {}

                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            generatedFile = file
                        )
                    }
                    onSuccess(file)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = error.localizedMessage ?: "เกิดข้อผิดพลาดในการสร้าง PDF"
                        )
                    }
                }
            )
        }
    }

    fun resetGeneratedFile() {
        _uiState.update { it.copy(generatedFile = null, errorMessage = null) }
    }
}
