package com.example.universal_dockit.renderers

import com.example.universal_dockit.RenderCallbacks
import com.example.universal_dockit.SpreadsheetHtmlBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * XlsDocumentRenderer — renders legacy XLS files using Apache POI HSSFWorkbook.
 *
 * Library : org.apache.poi:poi:5.3.0 (Apache 2.0)
 *
 * Delegates HTML table construction to [SpreadsheetHtmlBuilder] (shared with XLSX).
 * Supports: multiple sheets, formula evaluation, legacy BIFF format.
 */
internal class XlsDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) {
            SpreadsheetHtmlBuilder.build(filePath, isXlsx = false)
        }
        callbacks.showWebContent(html)
    }
}
