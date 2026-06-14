package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.RenderCallbacks
import com.prathap021.universal_dockit.SpreadsheetHtmlBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ExcelDocumentRenderer : DocumentRenderer {
    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val isXlsx = filePath.endsWith(".xlsx", ignoreCase = true)
        val html = withContext(Dispatchers.IO) {
            SpreadsheetHtmlBuilder.build(filePath, isXlsx)
        }
        callbacks.showWebContent(html)
    }
}
