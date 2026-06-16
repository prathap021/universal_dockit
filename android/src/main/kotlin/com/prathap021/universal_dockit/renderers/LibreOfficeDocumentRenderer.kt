package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.LibreOfficeRuntime
import com.prathap021.universal_dockit.RenderCallbacks
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders Word, Excel, and PowerPoint via LibreOfficeKit (convert to PDF, display in PDFView).
 * Falls back to the POI-based renderers when the native LibreOffice runtime is unavailable.
 */
internal class LibreOfficeDocumentRenderer(
    private val docType: String
) : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val source = File(filePath)
        if (!source.exists()) {
            callbacks.showError("File not found: $filePath")
            return
        }

        if (!LibreOfficeRuntime.isAvailable()) {
            fallbackRenderer().render(filePath, callbacks)
            return
        }

        val pdfFile = LibreOfficeRuntime.cachedPdfFile(callbacks.context, filePath)
        val needsConversion = !pdfFile.exists() || pdfFile.lastModified() < source.lastModified()

        try {
            if (needsConversion) {
                withContext(Dispatchers.IO) {
                    LibreOfficeRuntime.convertToPdf(callbacks.context, source, pdfFile)
                }
            }
            callbacks.showPdf(pdfFile)
        } catch (e: Throwable) {
            try {
                fallbackRenderer().render(filePath, callbacks)
            } catch (fallbackError: Throwable) {
                callbacks.showError(
                    "LibreOffice render failed: ${e.message ?: e.javaClass.simpleName}\n" +
                        "Fallback also failed: ${fallbackError.message ?: fallbackError.javaClass.simpleName}"
                )
            }
        }
    }

    private fun fallbackRenderer(): DocumentRenderer = when (docType.lowercase()) {
        "doc", "docx" -> WordDocumentRenderer()
        "xls", "xlsx" -> ExcelDocumentRenderer()
        "ppt", "pptx" -> PowerPointDocumentRenderer()
        else -> WordDocumentRenderer()
    }
}
