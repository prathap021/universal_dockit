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
        append(HtmlTemplates.header("Spreadsheet"))
        FileInputStream(filePath).use { fis ->
            val workbook = if (isXlsx) XSSFWorkbook(fis) else HSSFWorkbook(fis)
            workbook.use { wb ->
                val evaluator = wb.creationHelper.createFormulaEvaluator()

                for (si in 0 until wb.numberOfSheets) {
                    val sheet = wb.getSheetAt(si)
                    append("<h2 class='sheet-title'>${sheet.sheetName.esc()}</h2>")
                    append("<div class='table-wrapper'><table>")

                    for (rowIdx in sheet.firstRowNum..sheet.lastRowNum) {
                        val row = sheet.getRow(rowIdx) ?: continue
                        append("<tr>")
                        for (colIdx in row.firstCellNum until row.lastCellNum) {
                            val cell = row.getCell(colIdx)
                            val value = cell?.let {
                                try {
                                    val ev = evaluator.evaluate(it)
                                    when (ev?.cellType) {
                                        CellType.NUMERIC -> {
                                            val n = ev.numberValue
                                            if (n == floor(n)) n.toLong().toString()
                                            else "%.4f".format(n)
                                        }
                                        CellType.STRING  -> ev.stringValue
                                        CellType.BOOLEAN -> ev.booleanValue.toString()
                                        else             -> it.toString()
                                    }
                                } catch (_: Exception) { it.toString() }
                            } ?: ""
                            val tag = if (rowIdx == sheet.firstRowNum) "th" else "td"
                            append("<$tag>${value.esc()}</$tag>")
                        }
                        append("</tr>")
                    }
                    append("</table></div>")
                }
            }
        }
        append(HtmlTemplates.footer())
    }
}
