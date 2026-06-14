package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.io.FileInputStream

internal class PowerPointDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        val isPptx = filePath.endsWith(".pptx", ignoreCase = true)
        val extName = if (isPptx) ".pptx" else ".ppt"
        append(HtmlTemplates.header("PowerPoint Presentation ($extName)", accentColor = "#D24726"))
        
        runCatching {
            FileInputStream(filePath).use { fis ->
                if (isPptx) {
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
                } else {
                    HSLFSlideShow(fis).use { show ->
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
            }
        }.onFailure { error ->
            append("<p>Unable to read the PowerPoint file: ${error.message ?: "unknown error"}</p>")
        }
        append(HtmlTemplates.footer())
    }
}
