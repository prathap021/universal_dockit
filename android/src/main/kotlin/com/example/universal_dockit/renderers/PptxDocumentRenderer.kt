package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.io.FileInputStream

/**
 * PptxDocumentRenderer — renders PPTX files as a styled HTML "deck".
 *
 * Library : org.apache.poi:poi-ooxml:5.3.0
 */
internal class PptxDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("PowerPoint Presentation (.pptx)", accentColor = "#D24726"))
        runCatching {
            FileInputStream(filePath).use { fis ->
                XMLSlideShow(fis).use { show ->
                    show.slides.forEachIndexed { index, slide ->
                        append("<div class='slide-divider'>Slide ${index + 1}</div>")
                        append("<div class='slide-content'>")
                        @Suppress("UNCHECKED_CAST")
                        SlideShapeText.walk(
                            slide.shapes as Iterable<org.apache.poi.sl.usermodel.Shape<*, *>>,
                            this@buildString,
                        )
                        append("</div>")
                    }
                }
            }
        }.onFailure { error ->
            append("<p>Unable to read the PPTX file: ${error.message ?: "unknown error"}</p>")
        }
        append(HtmlTemplates.footer())
    }
}
