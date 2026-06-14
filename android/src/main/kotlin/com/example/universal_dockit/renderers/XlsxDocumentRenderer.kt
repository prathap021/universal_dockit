package com.example.universal_dockit.renderers

import com.example.universal_dockit.RenderCallbacks
import com.example.universal_dockit.SpreadsheetHtmlBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * XlsxDocumentRenderer — renders XLSX files using Apache POI XSSFWorkbook.
 *
 * Library : org.apache.poi:poi-ooxml:5.3.0 (Apache 2.0)
 *
 * Delegates HTML table construction to [SpreadsheetHtmlBuilder] (shared with XLS).
 * Supports: multiple sheets, formula evaluation, numeric formatting.
 */
internal class XlsxDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) {
            SpreadsheetHtmlBuilder.build(filePath, isXlsx = true)
        }
        callbacks.showWebContent(html)
    }
}
