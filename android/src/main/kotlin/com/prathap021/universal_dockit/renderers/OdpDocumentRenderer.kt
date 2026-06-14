package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.HtmlTemplates
import com.prathap021.universal_dockit.OdfContentParser
import com.prathap021.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OdpDocumentRenderer — renders ODP (OpenDocument Presentation) files.
 *
 * Uses [OdfContentParser]; each `draw:page` becomes a "Slide N" divider in the
 * resulting HTML, with text-frame contents rendered inline as a text-only
 * preview (no graphics rasterisation — avoids AWT).
 */
internal class OdpDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) {
            buildString {
                append(HtmlTemplates.header("OpenDocument Presentation", accentColor = "#D24726"))
                append(OdfContentParser.parse(filePath, docKind = "odp"))
                append(HtmlTemplates.footer())
            }
        }
        callbacks.showWebContent(html)
    }
}
