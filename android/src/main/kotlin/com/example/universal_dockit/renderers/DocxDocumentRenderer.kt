package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.HtmlTemplates.esc
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.FileInputStream

/**
 * DocxDocumentRenderer — renders DOCX files using Apache POI XWPFDocument.
 *
 * Library : org.apache.poi:poi-ooxml:5.3.0 (Apache 2.0)
 *
 * Rendering strategy:
 *  - Iterates paragraphs and maps style names to HTML headings (H1/H2/H3)
 *  - Preserves inline formatting per run: bold, italic, underline, strikethrough
 *  - Renders tables as HTML <table> elements
 *  - Delivers HTML to [RenderCallbacks.showWebContent]
 */
internal class DocxDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("Word Document"))
        FileInputStream(filePath).use { fis ->
            val doc = XWPFDocument(fis)

            // --- Paragraphs ---
            for (para in doc.paragraphs) {
                val style = para.style ?: ""
                val text  = para.text
                when {
                    style.contains("Heading1", ignoreCase = true) ||
                    style.equals("Title", ignoreCase = true) ->
                        append("<h1>${text.esc()}</h1>\n")

                    style.contains("Heading2", ignoreCase = true) ->
                        append("<h2>${text.esc()}</h2>\n")

                    style.contains("Heading3", ignoreCase = true) ->
                        append("<h3>${text.esc()}</h3>\n")

                    text.isBlank() -> append("<br/>\n")

                    else -> {
                        append("<p>")
                        for (run in para.runs) {
                            var t = run.text().esc()
                            if (run.isBold)         t = "<strong>$t</strong>"
                            if (run.isItalic)        t = "<em>$t</em>"
                            if (run.isStrikeThrough) t = "<del>$t</del>"
                            if (run.underline != UnderlinePatterns.NONE) t = "<u>$t</u>"
                            append(t)
                        }
                        append("</p>\n")
                    }
                }
            }

            // --- Tables ---
            for (table in doc.tables) {
                append("<div class='table-wrapper'><table>")
                for (row in table.rows) {
                    append("<tr>")
                    for (cell in row.tableCells) append("<td>${cell.text.esc()}</td>")
                    append("</tr>")
                }
                append("</table></div>\n")
            }
            doc.close()
        }
        append(HtmlTemplates.footer())
    }
}
