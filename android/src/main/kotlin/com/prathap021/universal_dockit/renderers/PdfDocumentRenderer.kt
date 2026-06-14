package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PdfDocumentRenderer — renders PDF files using PdfiumAndroid.
 *
 * Library : com.github.barteksc:android-pdf-viewer:3.2.0-beta.1 (Apache 2.0)
 *           Backed by the native PdfiumAndroid rendering engine.
 *
 * Features provided by PDFView:
 *  - Hardware-accelerated page rendering
 *  - Pinch-to-zoom and double-tap zoom
 *  - Fling-scroll between pages
 *  - DefaultScrollHandle (position indicator)
 *  - Anti-aliasing for crisp text at all zoom levels
 *  - Night mode toggle (off by default)
 *  - Configurable page spacing
 */
internal class PdfDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val file = File(filePath)
        if (!file.exists()) {
            callbacks.showError("File not found: $filePath")
            return
        }
        // PDFView must be configured on the main thread
        callbacks.showPdf(file)
    }
}
