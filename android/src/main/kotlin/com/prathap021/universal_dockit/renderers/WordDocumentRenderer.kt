package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.HtmlTemplates
import com.prathap021.universal_dockit.OfficeParsers
import com.prathap021.universal_dockit.ParsedDocument
import com.prathap021.universal_dockit.RenderCallbacks
import com.prathap021.universal_dockit.DocxElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream

internal class WordDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val parsed = withContext(Dispatchers.IO) {
            val fileStream = FileInputStream(filePath)
            OfficeParsers.parseDocx(fileStream)
        }
        val html = withContext(Dispatchers.IO) {
            buildString {
                append(HtmlTemplates.header("Word Document", accentColor = "#2B579A"))
                for (element in parsed.elements) {
                    when (element) {
                        is DocxElement.Paragraph -> {
                            append("<p style='margin-bottom: 8px;'>")
                            for (run in element.runs) {
                                var style = ""
                                if (run.isBold) style += "font-weight: bold;"
                                if (run.isItalic) style += "font-style: italic;"
                                if (run.isUnderline) style += "text-decoration: underline;"
                                if (run.color != null) style += "color: #${run.color};"
                                val escapedText = android.text.TextUtils.htmlEncode(run.text).replace("\n", "<br>")
                                append("<span style='\$style'>\$escapedText</span>")
                            }
                            append("</p>")
                        }
                        is DocxElement.Table -> {
                            append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 16px;'>")
                            for (row in element.rows) {
                                append("<tr>")
                                for (cell in row) {
                                    val escapedCell = android.text.TextUtils.htmlEncode(cell).replace("\n", "<br>")
                                    append("<td style='border: 1px solid #ccc; padding: 4px;'>\$escapedCell</td>")
                                }
                                append("</tr>")
                            }
                            append("</table>")
                        }
                    }
                }
                append(HtmlTemplates.footer())
            }
        }
        callbacks.showWebContent(html)
    }
}
