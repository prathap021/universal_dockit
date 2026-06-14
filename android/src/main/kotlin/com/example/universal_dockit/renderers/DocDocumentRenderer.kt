package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.HtmlTemplates.esc
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import java.io.FileInputStream

/**
 * DocDocumentRenderer — renders legacy DOC files using Apache POI HWPFDocument.
 *
 * Library : org.apache.poi:poi:5.3.0 (Apache 2.0)
 *
 * Rendering strategy:
 *  - Extracts full text via [WordExtractor] (preserves paragraph breaks)
 *  - Wraps each non-blank line in a <p> tag
 *  - Delivers HTML to [RenderCallbacks.showWebContent]
 *
 * Note: The legacy .doc binary format does not expose rich paragraph styles
 * through HWPF's high-level API; full text extraction is used instead.
 */
internal class DocDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("Word Document (.doc)"))
        FileInputStream(filePath).use { fis ->
            val doc      = HWPFDocument(fis)
            val extractor = WordExtractor(doc)
            for (line in extractor.text.lines()) {
                if (line.isBlank()) append("<br/>")
                else               append("<p>${line.esc()}</p>")
            }
            doc.close()
        }
        append(HtmlTemplates.footer())
    }
}
