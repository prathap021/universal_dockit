package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.HtmlTemplates.esc
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * CsvDocumentRenderer — renders CSV files as an HTML table in a WebView.
 *
 * Rendering strategy:
 *  - Reads lines with [BufferedReader] (memory-efficient for large files)
 *  - Parses each line with an RFC-4180 compliant parser
 *    (handles quoted fields containing commas or newlines)
 *  - First row → <th> header cells  |  subsequent rows → <td> data cells
 *  - Delivers the HTML string to [RenderCallbacks.showWebContent]
 *
 * No third-party libraries required.
 */
internal class CsvDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("CSV"))
        append("<div class='table-wrapper'><table>")
        var firstRow = true
        BufferedReader(
            InputStreamReader(FileInputStream(filePath), StandardCharsets.UTF_8),
        ).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val cells = parseCsvLine(line!!)
                append("<tr>")
                for (cell in cells) {
                    val tag = if (firstRow) "th" else "td"
                    append("<$tag>${cell.trim().esc()}</$tag>")
                }
                append("</tr>")
                firstRow = false
            }
        }
        append("</table></div>")
        append(HtmlTemplates.footer())
    }

    /** RFC-4180 CSV line parser — handles quoted fields containing commas. */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val cur = StringBuilder()
        for (ch in line) {
            when {
                ch == '"'           -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { result.add(cur.toString()); cur.clear() }
                else                -> cur.append(ch)
            }
        }
        result.add(cur.toString())
        return result
    }
}
