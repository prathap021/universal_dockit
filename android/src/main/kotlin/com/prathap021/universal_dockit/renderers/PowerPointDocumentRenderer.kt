package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.HtmlTemplates
import com.prathap021.universal_dockit.RenderCallbacks
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
        
        append("""
            <style>
                body { background-color: #f3f3f3; padding: 16px; font-family: sans-serif; }
                .slide-divider { margin: 24px 0 8px 0; font-weight: bold; color: #555; }
                .slide-content { 
                    background-color: white; 
                    border: 1px solid #ccc; 
                    box-shadow: 0 4px 8px rgba(0,0,0,0.1); 
                    min-height: 400px; 
                    padding: 32px; 
                    margin-bottom: 24px;
                    border-radius: 4px;
                    overflow: hidden;
                    position: relative;
                }
                .table-wrapper { overflow-x: auto; margin-top: 16px; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid #d0d7e5; padding: 8px; }
            </style>
        """.trimIndent())
        
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
