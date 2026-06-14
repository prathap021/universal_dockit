package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.HtmlTemplates.esc
import org.apache.poi.sl.usermodel.GroupShape
import org.apache.poi.sl.usermodel.Shape
import org.apache.poi.sl.usermodel.TableShape
import org.apache.poi.sl.usermodel.TextShape

/**
 * SlideShapeText — shared shape→HTML walker used by both PPT and PPTX renderers.
 *
 * Recursively walks a slide's shape tree and emits HTML for text shapes,
 * tables, and grouped shapes. Skips graphical shapes entirely — visual
 * fidelity is deliberately traded for reliability (no AWT / Graphics2D).
 */
internal object SlideShapeText {

    fun walk(shapes: Iterable<Shape<*, *>>, sb: StringBuilder) {
        for (shape in shapes) {
            when (shape) {
                is TableShape<*, *> -> appendTable(shape, sb)
                is TextShape<*, *> -> appendTextShape(shape, sb)
                is org.apache.poi.sl.usermodel.PictureShape<*, *> -> appendPictureShape(shape, sb)
                is GroupShape<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    walk(shape.shapes as Iterable<Shape<*, *>>, sb)
                }
            }
        }
    }

    private fun appendTextShape(shape: TextShape<*, *>, sb: StringBuilder) {
        val paragraphs = try { shape.textParagraphs } catch (_: Exception) { null }
        if (paragraphs.isNullOrEmpty()) return
        
        for (paragraph in paragraphs) {
            var align = ""
            try {
                when (paragraph.textAlign) {
                    org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER -> align = "text-align: center"
                    org.apache.poi.sl.usermodel.TextParagraph.TextAlign.RIGHT -> align = "text-align: right"
                    org.apache.poi.sl.usermodel.TextParagraph.TextAlign.JUSTIFY -> align = "text-align: justify"
                    else -> {}
                }
            } catch (_: Exception) {}
            
            val pStyle = if (align.isNotEmpty()) " style='$align'" else ""
            sb.append("<p$pStyle>")
            
            val runs = try { paragraph.textRuns } catch (_: Exception) { emptyList() }
            for (run in runs) {
                val text = run.rawText ?: continue
                
                val styles = mutableListOf<String>()
                try {
                    if (run.isBold == true) styles.add("font-weight: bold")
                    if (run.isItalic == true) styles.add("font-style: italic")
                    if (run.isUnderlined == true) styles.add("text-decoration: underline")
                    val fontSize = run.fontSize
                    if (fontSize != null && fontSize > 0) styles.add("font-size: clamp(12pt, ${fontSize}pt, 7vw)")
                } catch (_: Exception) {}
                
                val runStyle = if (styles.isNotEmpty()) " style='${styles.joinToString(";")}'" else ""
                val escaped = text.replace("\r", "").replace("\n", "<br/>").esc()
                sb.append("<span$runStyle>$escaped</span>")
            }
            sb.append("</p>")
        }
    }

    private fun appendTable(table: TableShape<*, *>, sb: StringBuilder) {
        val rows = table.numberOfRows
        val cols = table.numberOfColumns
        if (rows == 0 || cols == 0) return

        sb.append("<div class='table-wrapper'><table>")
        for (r in 0 until rows) {
            sb.append("<tr>")
            for (c in 0 until cols) {
                val value = try {
                    table.getCell(r, c)?.text?.trim().orEmpty()
                } catch (_: Exception) { "" }
                val tag = if (r == 0) "th" else "td"
                sb.append("<$tag>${value.esc()}</$tag>")
            }
            sb.append("</tr>")
        }
        sb.append("</table></div>")
    }

    private fun appendPictureShape(shape: org.apache.poi.sl.usermodel.PictureShape<*, *>, sb: StringBuilder) {
        val pictureData = try { shape.pictureData } catch (_: Exception) { null } ?: return
        val data = pictureData.data ?: return
        val mimeType = pictureData.contentType ?: "image/png"
        
        val base64 = android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
        sb.append("<div style='margin: 16px 0; text-align: center;'>")
        sb.append("<img src='data:$mimeType;base64,$base64' style='max-width: 100%; height: auto; border-radius: 4px;' />")
        sb.append("</div>")
    }
}
