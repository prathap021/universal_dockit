package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.docx4j.convert.out.svg.SvgExporter
import org.docx4j.openpackaging.packages.PresentationMLPackage
import java.io.File

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
                append("<div class='slide-divider'>Slide ${index + 1}</div>")
                append("<div class='slide-content'>")
                try {
                    val svg = SvgExporter.svg(document, slidePart)
                    append(svg)
                } catch (e: Exception) {
                    append("<p>Unable to render slide as SVG: ${e.message}</p>")
                }
                append("</div>")
            }
        }.onFailure { error ->
            append("<p>Unable to read the PPTX file: ${error.message ?: "unknown error"}</p>")
        }
        append(HtmlTemplates.footer())
    }
}
