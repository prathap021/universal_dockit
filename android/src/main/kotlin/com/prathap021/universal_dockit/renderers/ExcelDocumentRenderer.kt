package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.HtmlTemplates
import com.prathap021.universal_dockit.OfficeParsers
import com.prathap021.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream

internal class ExcelDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val parsed = withContext(Dispatchers.IO) {
            val fileStream = FileInputStream(filePath)
            OfficeParsers.parseXlsx(fileStream)
        }
        val html = withContext(Dispatchers.IO) {
            buildString {
                append(HtmlTemplates.header("Excel Spreadsheet", accentColor = "#217346"))
                for (sheet in parsed.workbook.sheets) {
                    val safeName = android.text.TextUtils.htmlEncode(sheet.name)
                    append("<h2 style='color: #217346; margin-top: 24px;'>\$safeName</h2>")
                    append("<div style='overflow-x: auto;'>")
                    append("<table style='border-collapse: collapse; min-width: 100%;'>")
                    for (r in 0..sheet.maxRow) {
                        append("<tr>")
                        for (c in 0..sheet.maxCol) {
                            val cellVal = sheet.rows[r]?.get(c) ?: ""
                            val escapedVal = android.text.TextUtils.htmlEncode(cellVal)
                            append("<td style='border: 1px solid #ddd; padding: 6px; white-space: nowrap;'>\$escapedVal</td>")
                        }
                        append("</tr>")
                    }
                    append("</table>")
                    append("</div>")
                }
                append(HtmlTemplates.footer())
            }
        }
        callbacks.showWebContent(html)
    }
}
