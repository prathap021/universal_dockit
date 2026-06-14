package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.HtmlTemplates.esc
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.FileInputStream

internal class WordDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        val isDocx = filePath.endsWith(".docx", ignoreCase = true)
        val extName = if (isDocx) ".docx" else ".doc"
        append(HtmlTemplates.header("Word Document ($extName)", accentColor = "#2B579A"))

        runCatching {
            FileInputStream(filePath).use { fis ->
                val text = if (isDocx) {
                    XWPFDocument(fis).use { doc ->
                        XWPFWordExtractor(doc).text
                    }
                } else {
                    HWPFDocument(fis).use { doc ->
                        WordExtractor(doc).text
                    }
                }
                text.lineSequence().forEach { line ->
                    when {
                        line.isBlank() -> append("<br/>\n")
                        else -> append("<p>${line.esc()}</p>\n")
                    }
                }
            }
        }.onFailure { error ->
            append("<p>Unable to read the Word file: ${error.message ?: "unknown error"}</p>")
        }

        append(HtmlTemplates.footer())
    }
}
