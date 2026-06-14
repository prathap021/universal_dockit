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
 * Library : org.apache.poi:poi-ooxml:5.3.0 (Apache 2.0)
 *
 * Text-only rendering strategy (reliable on Android):
 *  - Opens the slide show with [XMLSlideShow]
 *  - Emits a "Slide N" divider for each slide
 *  - Extracts text via [SlideShapeText] (recursively walks text/table/group shapes)
 *  - Does NOT call `slide.draw()` (which requires AWT Graphics2D)
 */
internal class PptxDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("PowerPoint Presentation"))
        FileInputStream(filePath).use { fis ->
            XMLSlideShow(fis).use { show ->
                show.slides.forEachIndexed { index, slide ->
                    append("<div class='slide-divider'>Slide ${index + 1}</div>")
                    @Suppress("UNCHECKED_CAST")
                    SlideShapeText.walk(
                        slide.shapes as Iterable<org.apache.poi.sl.usermodel.Shape<*, *>>,
                        this@buildString,
                    )
                }
            }
        }
        append(HtmlTemplates.footer())
    }
}
