package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates.esc
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
                is GroupShape<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    walk(shape.shapes as Iterable<Shape<*, *>>, sb)
                }
            }
        }
    }

    private fun appendTextShape(shape: TextShape<*, *>, sb: StringBuilder) {
        val text = try { shape.text?.trim().orEmpty() } catch (_: Exception) { "" }
        if (text.isEmpty()) return
        for (line in text.lines()) {
            if (line.isBlank()) sb.append("<br/>")
            else sb.append("<p>${line.esc()}</p>")
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
}
