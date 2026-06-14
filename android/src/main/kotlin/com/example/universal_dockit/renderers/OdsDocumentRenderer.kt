package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.OdfContentParser
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OdsDocumentRenderer — renders ODS (OpenDocument Spreadsheet) files.
 *
 * Uses [OdfContentParser] for direct ZIP + XmlPullParser parsing of `content.xml`.
 * Cell values are rendered as HTML tables.
 */
internal class OdsDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) {
            buildString {
                append(HtmlTemplates.header("OpenDocument Spreadsheet", accentColor = "#217346"))
                append(OdfContentParser.parse(filePath, docKind = "ods"))
                append(HtmlTemplates.footer())
            }
        }
        callbacks.showWebContent(html)
    }
}
