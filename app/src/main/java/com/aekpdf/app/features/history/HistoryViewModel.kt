package com.aekpdf.app.features.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aekpdf.app.AekPdfApplication
import com.aekpdf.app.core.database.PdfHistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val historyDao = (application as AekPdfApplication).database.pdfHistoryDao()

    val historyList: StateFlow<List<PdfHistoryEntity>> = historyDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteHistory(item: PdfHistoryEntity) {
        viewModelScope.launch {
            try {
                // Delete file from disk if desired
                val file = File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}

            historyDao.delete(item)
        }
    }
}
