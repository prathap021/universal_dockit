package com.prathap021.universal_dockit.renderers

import android.graphics.Bitmap
import android.util.Base64
import com.prathap021.universal_dockit.HtmlTemplates
import com.prathap021.universal_dockit.OfficeParsers
import com.prathap021.universal_dockit.RenderCallbacks
import com.prathap021.universal_dockit.SlideGraphicElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

internal class PowerPointDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val parsed = withContext(Dispatchers.IO) {
            val fileStream = FileInputStream(filePath)
            OfficeParsers.parsePptx(fileStream)
        }
        val html = withContext(Dispatchers.IO) {
            buildString {
                append(HtmlTemplates.header("PowerPoint Presentation", accentColor = "#B7472A"))
                
                // Keep 16:9 aspect ratio padding
                val slidePaddingBottom = (6858000f / 12192000f) * 100f
                
                for (slide in parsed.presentation.slides) {
                    val hexBg = String.format("#%06X", 0xFFFFFF and slide.backgroundColor.toInt())
                    append("<div style='position: relative; width: 100%; padding-bottom: \${slidePaddingBottom}%; background-color: \$hexBg; margin-bottom: 24px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); overflow: hidden;'>")
                    
                    for (element in slide.elements) {
                        val left = element.x * 100
                        val top = element.y * 100
                        val width = element.width * 100
                        val height = element.height * 100
                        
                        when (element) {
                            is SlideGraphicElement.TextBlock -> {
                                val hexColor = String.format("#%06X", 0xFFFFFF and element.textColor.toInt())
                                var style = "position: absolute; left: \${left}%; top: \${top}%; width: \${width}%; height: \${height}%; color: \$hexColor; font-size: \${element.fontSize}px; overflow: hidden; display: flex; align-items: center; justify-content: center; text-align: center;"
                                if (element.isBold) style += " font-weight: bold;"
                                if (element.isItalic) style += " font-style: italic;"
                                
                                val escapedText = android.text.TextUtils.htmlEncode(element.text).replace("\n", "<br>")
                                append("<div style='\$style'>\$escapedText</div>")
                            }
                            is SlideGraphicElement.ShapeBlock -> {
                                val hexColor = String.format("#%06X", 0xFFFFFF and element.color.toInt())
                                val style = "position: absolute; left: \${left}%; top: \${top}%; width: \${width}%; height: \${height}%; background-color: \$hexColor;"
                                append("<div style='\$style'></div>")
                            }
                            is SlideGraphicElement.ImageBlock -> {
                                val base64Img = bitmapToBase64(element.bitmap)
                                val style = "position: absolute; left: \${left}%; top: \${top}%; width: \${width}%; height: \${height}%; object-fit: contain;"
                                append("<img src='data:image/png;base64,\$base64Img' style='\$style'/>")
                            }
                        }
                    }
                    
                    append("</div>")
                }
                append(HtmlTemplates.footer())
            }
        }
        callbacks.showWebContent(html)
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
