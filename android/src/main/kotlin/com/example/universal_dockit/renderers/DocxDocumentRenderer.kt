package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.HtmlTemplates.esc
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.FileInputStream

/**
 * DocxDocumentRenderer — renders DOCX files using Apache POI XWPFDocument.
 *
 * Library : org.apache.poi:poi-ooxml:5.3.0
 *
 * Rendering strategy:
 *  - Uses XWPFWordExtractor for full text extraction
 *  - Emits plain paragraph HTML for Android/JVM compatibility
 */
internal class DocxDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("Word Document (.docx)", accentColor = "#2B579A"))

        runCatching {
            FileInputStream(filePath).use { fis ->
                XWPFDocument(fis).use { doc ->
                    XWPFWordExtractor(doc).text.lineSequence().forEach { line ->
                        when {
                            line.isBlank() -> append("<br/>\n")
                            else -> append("<p>${line.esc()}</p>\n")
                        }
                    }
                }
            }
        }.onFailure { error ->
            append("<p>Unable to read the DOCX file: ${error.message ?: "unknown error"}</p>")
        }

        append(HtmlTemplates.footer())
    }
}
