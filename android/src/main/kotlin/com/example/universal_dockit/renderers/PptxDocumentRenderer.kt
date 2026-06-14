package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.HtmlTemplates.esc
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.docx4j.TextUtils
import org.docx4j.openpackaging.packages.PresentationMLPackage
import java.io.File

/**
 * PptxDocumentRenderer — renders PPTX files as a styled HTML "deck".
 *
 * Library : org.docx4j:docx4j-core / docx4j-openxml-objects-pml (Apache 2.0)
 *
 * Text-only rendering strategy:
 *  - Opens the slide show with [PresentationMLPackage]
 *  - Emits a "Slide N" divider for each slide
 *  - Extracts text using docx4j's TextUtils
 */
internal class PptxDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("PowerPoint Presentation"))
        runCatching {
            val document = PresentationMLPackage.load(File(filePath)) as PresentationMLPackage
            val slideParts = document.mainPresentationPart.slideParts
            slideParts?.forEachIndexed { index, slidePart ->
                append("<div class='slide-divider'>Slide \${index + 1}</div>")
                val text = TextUtils.getText(slidePart.jaxbElement)
                text.lineSequence().forEach { line ->
                    if (line.isNotBlank()) {
                        append("<p>\${line.esc()}</p>\n")
                    }
                }
            }
        }.onFailure { error ->
            append("<p>Unable to read the PPTX file: \${error.message ?: "unknown error"}</p>")
        }
        append(HtmlTemplates.footer())
    }
}
