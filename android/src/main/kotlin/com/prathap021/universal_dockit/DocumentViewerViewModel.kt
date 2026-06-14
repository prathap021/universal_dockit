package com.prathap021.universal_dockit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathap021.universal_dockit.renderers.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentViewerViewModel : ViewModel() {

    internal fun renderDocument(filePath: String, docType: String, callbacks: RenderCallbacks) {
        val renderer: DocumentRenderer = when (docType.lowercase()) {
            "pdf" -> PdfDocumentRenderer()
            "txt" -> TxtDocumentRenderer()
            "csv" -> CsvDocumentRenderer()
            "rtf" -> RtfDocumentRenderer()
            "odt" -> OdtDocumentRenderer()
            "ods" -> OdsDocumentRenderer()
            "odp" -> OdpDocumentRenderer()
            "docx", "doc", "xlsx", "xls", "pptx", "ppt" -> {
                callbacks.showError("Office documents should be opened via All_Document_Reader_Activity")
                return
            }
            else -> {
                callbacks.showError("Unsupported document type: $docType")
                return
            }
        }

        viewModelScope.launch {
            try {
                // The renderer internals already use withContext(Dispatchers.IO) for parsing.
                renderer.render(filePath, callbacks)
            } catch (e: Throwable) {
                callbacks.showError("Failed to render document\n${e.javaClass.simpleName}: ${e.message ?: ""}")
            }
        }
    }
}
