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

internal class PowerPointDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val parsed = withContext(Dispatchers.IO) {
            OfficeParsers.parsePresentation(filePath)
        }
        val html = withContext(Dispatchers.IO) {
            buildString {
                append(HtmlTemplates.header("PowerPoint Presentation", accentColor = "#B7472A"))
                if (parsed.presentation.limitedMode) {
                    val note = android.text.TextUtils.htmlEncode(
                        parsed.presentation.fallbackNote ?: "This presentation is rendered in limited compatibility mode."
                    )
                    append("<div style='margin-bottom:16px;padding:10px 12px;border-left:4px solid #d48806;background:#fff7e6;color:#8c5a00;'>$note</div>")
                }
                
                val slidePaddingBottom = (
                    parsed.presentation.pageHeightEmu / parsed.presentation.pageWidthEmu
                ) * 100f
                
                for (slide in parsed.presentation.slides) {
                    val hexBg = String.format("#%06X", 0xFFFFFF and slide.backgroundColor.toInt())
                    append("<div style='position: relative; width: 100%; padding-bottom: ${slidePaddingBottom}%; background-color: $hexBg; margin-bottom: 24px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); overflow: hidden;'>")
                    
                    for (element in slide.elements) {
                        when (element) {
                            is SlideGraphicElement.TextBlock -> {
                                val left = element.x * 100
                                val top = element.y * 100
                                val width = element.width * 100
                                val height = element.height * 100
                                val hexColor = String.format("#%06X", 0xFFFFFF and element.textColor.toInt())
                                var style = "position: absolute; left: ${left}%; top: ${top}%; width: ${width}%; height: ${height}%; color: $hexColor; font-size: ${element.fontSize}px; overflow: hidden; white-space: pre-wrap;"
                                if (element.isBold) style += " font-weight: bold;"
                                if (element.isItalic) style += " font-style: italic;"
                                
                                val escapedText = android.text.TextUtils.htmlEncode(element.text).replace("\n", "<br>")
                                append("<div style='$style'>$escapedText</div>")
                            }
                            is SlideGraphicElement.ShapeBlock -> {
                                val left = element.x * 100
                                val top = element.y * 100
                                val width = element.width * 100
                                val height = element.height * 100
                                val hexColor = String.format("#%06X", 0xFFFFFF and element.color.toInt())
                                val style = "position: absolute; left: ${left}%; top: ${top}%; width: ${width}%; height: ${height}%; background-color: $hexColor;"
                                append("<div style='$style'></div>")
                            }
                            is SlideGraphicElement.ImageBlock -> {
                                val left = element.x * 100
                                val top = element.y * 100
                                val width = element.width * 100
                                val height = element.height * 100
                                val base64Img = bitmapToBase64(element.bitmap)
                                val style = "position: absolute; left: ${left}%; top: ${top}%; width: ${width}%; height: ${height}%; object-fit: contain;"
                                append("<img src='data:image/png;base64,$base64Img' style='$style'/>")
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
