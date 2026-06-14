package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.OdfDomExtractor
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.odftoolkit.odfdom.doc.OdfPresentationDocument

/**
 * OdpDocumentRenderer — renders ODP (OpenDocument Presentation) files.
 *
 * Library : org.apache.odftoolkit:odfdom-java:0.10.0 (Apache 2.0)
 *
 * Rendering strategy:
 *  - Opens the file with [OdfPresentationDocument]
 *  - Traverses content DOM via [OdfDomExtractor]
 *  - draw:page elements → slide-divider banners (numbered)
 *  - Text frames and paragraphs inside each slide are preserved
 *  - Delivers HTML to [RenderCallbacks.showWebContent]
 */
internal class OdpDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("OpenDocument Presentation"))
        try {
            OdfPresentationDocument.loadDocument(filePath).use { doc ->
                append(OdfDomExtractor.extractHtml(doc, "odp"))
            }
        } catch (e: Exception) {
            append("<p class='error'>ODP render error: ${e.message}</p>")
        }
        append(HtmlTemplates.footer())
    }
}
