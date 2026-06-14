package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.OdfDomExtractor
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.odftoolkit.odfdom.doc.OdfTextDocument

/**
 * OdtDocumentRenderer — renders ODT (OpenDocument Text) files.
 *
 * Library : org.apache.odftoolkit:odfdom-java:0.10.0 (Apache 2.0)
 *
 * Rendering strategy:
 *  - Opens the file with [OdfTextDocument] (Apache ODF Toolkit)
 *  - Traverses the content DOM via [OdfDomExtractor]
 *  - Emits paragraphs, headings, and tables as styled HTML
 *  - Delivers HTML to [RenderCallbacks.showWebContent]
 */
internal class OdtDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("OpenDocument Text"))
        try {
            OdfTextDocument.loadDocument(filePath).use { doc ->
                append(OdfDomExtractor.extractHtml(doc, "odt"))
            }
        } catch (e: Exception) {
            append("<p class='error'>ODT render error: ${e.message}</p>")
        }
        append(HtmlTemplates.footer())
    }
}
