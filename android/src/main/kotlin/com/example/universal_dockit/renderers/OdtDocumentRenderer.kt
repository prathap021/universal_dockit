package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.OdfContentParser
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OdtDocumentRenderer — renders ODT (OpenDocument Text) files.
 *
 * Uses [OdfContentParser] — a tiny built-in ZIP + XmlPullParser implementation
 * that has no third-party dependencies and works reliably on Android.
 */
internal class OdtDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) {
            buildString {
                append(HtmlTemplates.header("OpenDocument Text"))
                append(OdfContentParser.parse(filePath, docKind = "odt"))
                append(HtmlTemplates.footer())
            }
        }
        callbacks.showWebContent(html)
    }
}
