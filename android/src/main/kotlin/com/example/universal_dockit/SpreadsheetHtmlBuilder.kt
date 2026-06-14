package com.example.universal_dockit

import com.example.universal_dockit.HtmlTemplates.esc
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import kotlin.math.floor

/**
 * SpreadsheetHtmlBuilder — shared HTML-table builder for XLS and XLSX files.
 *
 * Both [XlsDocumentRenderer] and [XlsxDocumentRenderer] delegate here so
 * the table-building logic lives in one place.
 *
 * Rendering strategy:
 *  - Opens the workbook with Apache POI (HSSF for XLS, XSSF for XLSX)
 *  - Evaluates formula cells so computed values are shown, not expressions
 *  - Emits one <table> per sheet, prefixed by a sheet-name heading
 *  - First data row rendered as <th> header cells
 */
internal object SpreadsheetHtmlBuilder {

    /**
     * Builds a full HTML page containing all sheets of the workbook at [filePath].
     * @param isXlsx true → XSSFWorkbook (.xlsx), false → HSSFWorkbook (.xls)
     */
    fun build(filePath: String, isXlsx: Boolean): String = buildString {
        append(HtmlTemplates.header("Spreadsheet", accentColor = "#217346"))
        
        append("""
            <style>
                table { border-collapse: collapse; table-layout: fixed; min-width: 100%; }
                th, td { border: 1px solid #d0d7e5; padding: 4px 8px; overflow: hidden; word-wrap: break-word; }
                th { background-color: #f3f5f9; font-weight: bold; }
                .sheet-title { color: #217346; margin-top: 24px; border-bottom: 2px solid #217346; padding-bottom: 4px; }
            </style>
        """.trimIndent())

        java.io.FileInputStream(filePath).use { fis ->
            val workbook = if (isXlsx) XSSFWorkbook(fis) else HSSFWorkbook(fis)
            workbook.use { wb ->
                val evaluator = wb.creationHelper.createFormulaEvaluator()

                for (si in 0 until wb.numberOfSheets) {
                    val sheet = wb.getSheetAt(si)
                    append("<h2 class='sheet-title'>${sheet.sheetName.esc()}</h2>")
                    append("<div class='table-wrapper'><table>")

                    // Generate a <colgroup> for column widths
                    if (sheet.physicalNumberOfRows > 0) {
                        val firstRow = sheet.getRow(sheet.firstRowNum)
                        if (firstRow != null) {
                            append("<colgroup>")
                            for (colIdx in firstRow.firstCellNum until firstRow.lastCellNum) {
                                val width = sheet.getColumnWidth(colIdx.toInt()) / 256
                                // Approximate pixels per character width
                                val pxWidth = if (width > 0) width * 7 else 80
                                append("<col style='width: ${pxWidth}px;'>")
                            }
                            append("</colgroup>")
                        }
                    }

                    for (rowIdx in sheet.firstRowNum..sheet.lastRowNum) {
                        val row = sheet.getRow(rowIdx) ?: continue
                        // Row height
                        val rowHeight = row.heightInPoints
                        val trStyle = if (rowHeight > 0) " style='height: ${rowHeight}pt;'" else ""
                        
                        append("<tr$trStyle>")
                        for (colIdx in row.firstCellNum until row.lastCellNum) {
                            val cell = row.getCell(colIdx.toInt())
                            
                            var cellContent = ""
                            var inlineStyle = ""
                            val tag = if (rowIdx == sheet.firstRowNum) "th" else "td"

                            if (cell != null) {
                                // Value
                                cellContent = try {
                                    val ev = evaluator.evaluate(cell)
                                    when (ev?.cellType) {
                                        CellType.NUMERIC -> {
                                            val n = ev.numberValue
                                            if (n == floor(n)) n.toLong().toString()
                                            else "%.4f".format(n)
                                        }
                                        CellType.STRING  -> ev.stringValue
                                        CellType.BOOLEAN -> ev.booleanValue.toString()
                                        else             -> cell.toString()
                                    }
                                } catch (_: Exception) { cell.toString() }

                                // Styling
                                val style = cell.cellStyle
                                if (style != null) {
                                    val font = wb.getFontAt(style.fontIndexAsInt)
                                    val bgRgb = extractColorRgb(style.fillForegroundColorColor)
                                    val fontRgb = when (font) {
                                        is org.apache.poi.xssf.usermodel.XSSFFont -> extractColorRgb(font.xssfColor)
                                        is org.apache.poi.hssf.usermodel.HSSFFont -> {
                                            val palette = (wb as? HSSFWorkbook)?.customPalette
                                            val hssfColor = palette?.getColor(font.color)
                                            extractColorRgb(hssfColor)
                                        }
                                        else -> null
                                    }
                                    
                                    val styles = mutableListOf<String>()
                                    
                                    if (bgRgb != null) styles.add("background-color: $bgRgb")
                                    if (fontRgb != null) styles.add("color: $fontRgb")
                                    if (font.bold) styles.add("font-weight: bold")
                                    if (font.italic) styles.add("font-style: italic")
                                    if (font.fontHeightInPoints > 0) styles.add("font-size: ${font.fontHeightInPoints}pt")
                                    
                                    when (style.alignment) {
                                        org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER -> styles.add("text-align: center")
                                        org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT -> styles.add("text-align: right")
                                        else -> {}
                                    }
                                    
                                    when (style.verticalAlignment) {
                                        org.apache.poi.ss.usermodel.VerticalAlignment.TOP -> styles.add("vertical-align: top")
                                        org.apache.poi.ss.usermodel.VerticalAlignment.CENTER -> styles.add("vertical-align: middle")
                                        org.apache.poi.ss.usermodel.VerticalAlignment.BOTTOM -> styles.add("vertical-align: bottom")
                                        else -> {}
                                    }

                                    if (styles.isNotEmpty()) {
                                        inlineStyle = " style='${styles.joinToString(";")}'"
                                    }
                                }
                            }

                            append("<$tag$inlineStyle>${cellContent.esc()}</$tag>")
                        }
                        append("</tr>")
                    }
                    append("</table></div>")
                }
            }
        }
        append(HtmlTemplates.footer())
    }

    private fun extractColorRgb(color: org.apache.poi.ss.usermodel.Color?): String? {
        if (color == null) return null
        return when (color) {
            is org.apache.poi.xssf.usermodel.XSSFColor -> {
                val argb = color.argbHex ?: return null
                if (argb.length == 8) "#${argb.substring(2)}" else "#$argb"
            }
            is org.apache.poi.hssf.util.HSSFColor -> {
                val rgb = color.triplet
                "rgb(${rgb[0]}, ${rgb[1]}, ${rgb[2]})"
            }
            else -> null
        }
    }
}
