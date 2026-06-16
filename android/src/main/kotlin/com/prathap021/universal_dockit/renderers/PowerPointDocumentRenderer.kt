package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.PptPdfConverter
import com.prathap021.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PowerPoint renderer — converts PPT/PPTX to PDF on a background thread, then displays via PdfiumAndroid.
 */
internal class PowerPointDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val source = File(filePath)
        if (!source.exists()) {
            callbacks.showError("File not found: $filePath")
            return
        }

        val pdfFile = PptPdfConverter.cachedPdfFile(callbacks.context, filePath)
        val needsConversion = !pdfFile.exists() || pdfFile.lastModified() < source.lastModified()

        if (needsConversion) {
            withContext(Dispatchers.IO) {
                PptPdfConverter.convertToPdf(filePath, pdfFile)
            }
        }

        callbacks.showPdf(pdfFile)
    }
}
