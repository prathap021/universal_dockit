package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.HtmlTemplates
import com.prathap021.universal_dockit.OfficeParsers
import com.prathap021.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ExcelDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val parsed = withContext(Dispatchers.IO) {
            OfficeParsers.parseExcel(filePath)
        }
        val html = withContext(Dispatchers.IO) {
            buildString {
                append(HtmlTemplates.header("Excel Spreadsheet", accentColor = "#217346"))
                for (sheet in parsed.workbook.sheets) {
                    val safeName = android.text.TextUtils.htmlEncode(sheet.name)
                    append("<h2 style='color: #217346; margin-top: 24px;'>$safeName</h2>")
                    if (sheet.rows.isEmpty()) {
                        append("<p style='color: #666;'>Empty sheet</p>")
                        continue
                    }
                    append("<div style='overflow: auto; max-width: 100%; border: 1px solid #d7e6dc; border-radius: 6px;'>")
                    append("<table style='border-collapse: collapse; min-width: max-content; width: auto;'>")
                    append("<thead><tr>")
                    append("<th style='position: sticky; left: 0; top: 0; z-index: 3; background: #217346; color: white; min-width: 48px;'>#</th>")
                    for (c in sheet.minCol..sheet.maxCol) {
                        if (sheet.hiddenCols.contains(c)) continue
                        val widthStyle = sheet.colWidths[c]?.let { "width:${it.coerceAtLeast(72f)}px;" } ?: "min-width:96px;"
                        append("<th style='position: sticky; top: 0; z-index: 2; background: #217346; color: white; $widthStyle'>${excelColumnName(c)}</th>")
                    }
                    append("</tr></thead><tbody>")
                    val mergedAnchors = sheet.mergedRegions.associateBy { it.firstRow to it.firstCol }
                    val mergedCoveredCells = buildSet {
                        for (region in sheet.mergedRegions) {
                            for (r in region.firstRow..region.lastRow) {
                                for (c in region.firstCol..region.lastCol) {
                                    if (r == region.firstRow && c == region.firstCol) continue
                                    add(r to c)
                                }
                            }
                        }
                    }
                    for (r in sheet.minRow..sheet.maxRow) {
                        if (sheet.hiddenRows.contains(r)) continue
                        val rowHeightStyle = sheet.rowHeights[r]?.let { "height:${it.coerceAtLeast(16f)}px;" } ?: ""
                        append("<tr>")
                        append("<th style='position: sticky; left: 0; z-index: 1; background: #eef7f1; color: #315942; min-width: 48px;'>${r + 1}</th>")
                        for (c in sheet.minCol..sheet.maxCol) {
                            if (sheet.hiddenCols.contains(c)) continue
                            if (mergedCoveredCells.contains(r to c)) continue
                            val cell = sheet.rows[r]?.get(c)
                            val cellVal = cell?.value.orEmpty()
                            val escapedVal = android.text.TextUtils.htmlEncode(cellVal)
                            val style = buildString {
                                append("border:1px solid #ddd;padding:7px 10px;min-width:96px;")
                                val wrapText = cell?.style?.wrapText ?: false
                                append(if (wrapText) "white-space:pre-wrap;" else "white-space:nowrap;")
                                cell?.style?.horizontalAlign?.let { append("text-align:$it;") }
                                cell?.style?.verticalAlign?.let { append("vertical-align:$it;") }
                                cell?.style?.bgColor?.let { append("background:$it;") }
                                if (cell?.style?.isBold == true) append("font-weight:bold;")
                                if (cell?.style?.isItalic == true) append("font-style:italic;")
                                cell?.style?.textColor?.let { append("color:$it;") }
                                append(rowHeightStyle)
                            }
                            val merged = mergedAnchors[r to c]
                            val rowspanAttr = merged?.let { if (it.lastRow > it.firstRow) " rowspan='${it.lastRow - it.firstRow + 1}'" else "" } ?: ""
                            val colspanAttr = merged?.let { if (it.lastCol > it.firstCol) " colspan='${it.lastCol - it.firstCol + 1}'" else "" } ?: ""
                            append("<td$rowspanAttr$colspanAttr style='$style'>$escapedVal</td>")
                        }
                        append("</tr>")
                    }
                    append("</tbody></table>")
                    append("</div>")
                }
                append(HtmlTemplates.footer())
            }
        }
        callbacks.showWebContent(html)
    }

    private fun excelColumnName(index: Int): String {
        var value = index + 1
        val name = StringBuilder()
        while (value > 0) {
            value--
            name.insert(0, ('A'.code + (value % 26)).toChar())
            value /= 26
        }
        return name.toString()
    }
}
