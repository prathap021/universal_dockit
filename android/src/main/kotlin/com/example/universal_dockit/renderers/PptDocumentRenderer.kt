package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import java.io.FileInputStream

/**
 * PptDocumentRenderer — renders legacy PPT files as styled HTML.
 *
 * Library : org.apache.poi:poi-scratchpad:5.3.0 (Apache 2.0)
 *
 * Same text-only strategy as [PptxDocumentRenderer] — reuses [SlideShapeText]
 * to walk shapes and emit text/tables. No AWT calls.
 */
internal class PptDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        append(HtmlTemplates.header("PowerPoint Presentation (.ppt)"))
        FileInputStream(filePath).use { fis ->
            HSLFSlideShow(fis).use { show ->
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
