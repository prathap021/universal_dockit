package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.OdfDomExtractor
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument

/**
 * OdsDocumentRenderer — renders ODS (OpenDocument Spreadsheet) files.
 *
 * Library : org.apache.odftoolkit:odfdom-java:0.10.0 (Apache 2.0)
 *
 * Rendering strategy:
 *  - Opens the file with [OdfSpreadsheetDocument]
 *  - Traverses the content DOM via [OdfDomExtractor]
 *  - table:table elements → <table>, table:table-row → <tr>, table:table-cell → <td>
 *  - Delivers HTML to [RenderCallbacks.showWebContent]
 */
internal class OdsDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("OpenDocument Spreadsheet"))
        try {
            OdfSpreadsheetDocument.loadDocument(filePath).use { doc ->
                append(OdfDomExtractor.extractHtml(doc, "ods"))
            }
        } catch (e: Exception) {
            append("<p class='error'>ODS render error: ${e.message}</p>")
        }
        append(HtmlTemplates.footer())
    }
}
