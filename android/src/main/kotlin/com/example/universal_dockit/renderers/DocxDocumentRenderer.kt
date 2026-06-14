package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.HtmlTemplates.esc
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.docx4j.TextUtils
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import java.io.File

/**
 * DocxDocumentRenderer — renders DOCX files using docx4j.
 *
 * Library : org.docx4j:docx4j-core / docx4j-JAXB-ReferenceImpl (Apache 2.0)
 *
 * Rendering strategy:
 *  - Uses docx4j for OpenXML text extraction, which keeps the DOCX path
 *    separate from legacy binary Office formats.
 *  - Emits plain paragraph HTML for Android/JVM compatibility and large-file
 *    performance.
 *  - Delivers HTML to [RenderCallbacks.showWebContent].
 */
internal class DocxDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("Word Document (.docx)"))

        runCatching {
            val document = WordprocessingMLPackage.load(File(filePath))
            val text = TextUtils.getText(document)

            text.lineSequence().forEach { line ->
                when {
                    line.isBlank() -> append("<br/>\n")
                    else -> append("<p>${line.esc()}</p>\n")
                }
            }
        }.onFailure { error ->
            append("<p>Unable to read the DOCX file: ${error.message ?: "unknown error"}</p>")
        }

        append(HtmlTemplates.footer())
    }
}
