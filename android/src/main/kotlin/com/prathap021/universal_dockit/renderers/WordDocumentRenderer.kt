package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.HtmlTemplates
import com.prathap021.universal_dockit.OfficeParsers
import com.prathap021.universal_dockit.RenderCallbacks
import com.prathap021.universal_dockit.DocxElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class WordDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val parsed = withContext(Dispatchers.IO) {
            OfficeParsers.parseWord(filePath)
        }
        val html = withContext(Dispatchers.IO) {
            buildString {
                append(HtmlTemplates.header("Word Document", accentColor = "#2B579A"))
                if (parsed.limitedMode) {
                    val note = android.text.TextUtils.htmlEncode(
                        parsed.fallbackNote ?: "This file is rendered with limited compatibility mode."
                    )
                    append("<div style='margin-bottom:16px;padding:10px 12px;border-left:4px solid #d48806;background:#fff7e6;color:#8c5a00;'>$note</div>")
                }
                for (element in parsed.elements) {
                    when (element) {
                        is DocxElement.Paragraph -> {
                            val paragraphStyle = buildString {
                                append("margin-top:${element.spacingBeforePx}px;")
                                append("margin-bottom:${element.spacingAfterPx}px;")
                                if (element.marginLeftPx > 0) append("margin-left:${element.marginLeftPx}px;")
                                if (element.alignment != null) append("text-align:${element.alignment};")
                                if (element.lineHeight != null) append("line-height:${element.lineHeight};")
                            }
                            append("<p style='$paragraphStyle'>")
                            for (run in element.runs) {
                                var style = ""
                                if (run.isBold) style += "font-weight: bold;"
                                if (run.isItalic) style += "font-style: italic;"
                                if (run.isUnderline) style += "text-decoration: underline;"
                                if (run.color != null) style += "color: #${run.color};"
                                val escapedText = android.text.TextUtils.htmlEncode(run.text).replace("\n", "<br>")
                                append("<span style='$style'>$escapedText</span>")
                            }
                            append("</p>")
                        }
                        is DocxElement.Table -> {
                            append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 16px;'>")
                            for (row in element.rows) {
                                append("<tr>")
                                for (cell in row) {
                                    val escapedCell = android.text.TextUtils.htmlEncode(cell.text).replace("\n", "<br>")
                                    val cellStyle = buildString {
                                        append("border:1px solid #ccc;padding:6px;")
                                        if (!cell.backgroundColor.isNullOrBlank()) append("background:#${cell.backgroundColor};")
                                        if (!cell.alignment.isNullOrBlank()) append("text-align:${cell.alignment};")
                                    }
                                    val colspanAttr = if (cell.colSpan > 1) " colspan='${cell.colSpan}'" else ""
                                    append("<td$colspanAttr style='$cellStyle'>$escapedCell</td>")
                                }
                                append("</tr>")
                            }
                            append("</table>")
                        }
                        is DocxElement.Image -> {
                            val alt = android.text.TextUtils.htmlEncode(element.description ?: "Document image")
                            val dimensionStyle = buildString {
                                if (element.width > 0) append("width: ${element.width}px;")
                                if (element.height > 0) append("height: auto;")
                            }
                            append(
                                "<figure style='margin: 16px 0; text-align: center;'>" +
                                    "<img src='data:${element.mimeType};base64,${element.base64}' alt='$alt' " +
                                    "style='max-width: 100%; $dimensionStyle object-fit: contain;'/>" +
                                "</figure>"
                            )
                        }
                    }
                }
                append(HtmlTemplates.footer())
            }
        }
        callbacks.showWebContent(html)
    }
}
