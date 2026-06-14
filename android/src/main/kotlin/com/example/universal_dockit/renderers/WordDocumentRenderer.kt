package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates
import com.example.universal_dockit.HtmlTemplates.esc
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.FileInputStream

internal class WordDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val html = withContext(Dispatchers.IO) { buildHtml(filePath) }
        callbacks.showWebContent(html)
    }

    private fun buildHtml(filePath: String): String = buildString {
        val isDocx = filePath.endsWith(".docx", ignoreCase = true)
        val extName = if (isDocx) ".docx" else ".doc"
        append(HtmlTemplates.header("Word Document ($extName)", accentColor = "#2B579A"))

        runCatching {
            java.io.FileInputStream(filePath).use { fis ->
                if (isDocx) {
                    XWPFDocument(fis).use { doc ->
                        for (element in doc.bodyElements) {
                            when (element) {
                                is org.apache.poi.xwpf.usermodel.XWPFParagraph -> {
                                    var pStyle = ""
                                    when (element.alignment) {
                                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER -> pStyle = " style='text-align: center;'"
                                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT -> pStyle = " style='text-align: right;'"
                                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.BOTH -> pStyle = " style='text-align: justify;'"
                                        else -> {}
                                    }
                                    append("<p$pStyle>")
                                    
                                    for (run in element.runs) {
                                        // Extract images embedded in the run
                                        for (pic in run.embeddedPictures) {
                                            val picData = pic.pictureData
                                            val base64 = android.util.Base64.encodeToString(picData.data, android.util.Base64.NO_WRAP)
                                            val mime = picData.suggestFileExtension()
                                            append("<img src='data:image/$mime;base64,$base64' style='max-width: 100%; height: auto; margin: 8px 0;' /><br/>")
                                        }
                                        
                                        // Extract text formatting
                                        val text = run.text() ?: continue
                                        var runStyle = ""
                                        val styles = mutableListOf<String>()
                                        if (run.isBold) styles.add("font-weight: bold")
                                        if (run.isItalic) styles.add("font-style: italic")
                                        if (run.underline != org.apache.poi.xwpf.usermodel.UnderlinePatterns.NONE) styles.add("text-decoration: underline")
                                        
                                        val color = run.color
                                        if (color != null) styles.add("color: #$color")
                                        
                                        val fontSize = run.fontSize
                                        if (fontSize > 0) styles.add("font-size: ${fontSize}pt")
                                        
                                        if (styles.isNotEmpty()) {
                                            runStyle = " style='${styles.joinToString(";")}'"
                                        }
                                        append("<span$runStyle>${text.esc()}</span>")
                                    }
                                    append("</p>\n")
                                }
                                is org.apache.poi.xwpf.usermodel.XWPFTable -> {
                                    append("<table style='border-collapse: collapse; width: 100%; margin: 16px 0;'>")
                                    element.rows.forEach { row ->
                                        append("<tr>")
                                        row.tableCells.forEach { cell ->
                                            val bgColor = cell.color
                                            val bgStyle = if (bgColor != null) " background-color: #$bgColor;" else ""
                                            append("<td style='border: 1px solid #d0d7e5; padding: 8px;$bgStyle'>")
                                            append(cell.text.esc())
                                            append("</td>")
                                        }
                                        append("</tr>")
                                    }
                                    append("</table>")
                                }
                            }
                        }
                    }
                } else {
                    org.apache.poi.hwpf.HWPFDocument(fis).use { doc ->
                        val range = doc.range
                        for (i in 0 until range.numParagraphs()) {
                            val p = range.getParagraph(i)
                            var pStyle = ""
                            when (p.justification) {
                                1 -> pStyle = " style='text-align: center;'"
                                2 -> pStyle = " style='text-align: right;'"
                                3 -> pStyle = " style='text-align: justify;'"
                                else -> {}
                            }
                            append("<p$pStyle>")
                            for (j in 0 until p.numCharacterRuns()) {
                                val run = p.getCharacterRun(j)
                                val text = run.text() ?: continue
                                var runStyle = ""
                                val styles = mutableListOf<String>()
                                if (run.isBold) styles.add("font-weight: bold")
                                if (run.isItalic) styles.add("font-style: italic")
                                val fontSize = run.fontSize / 2
                                if (fontSize > 0) styles.add("font-size: ${fontSize}pt")
                                
                                if (styles.isNotEmpty()) {
                                    runStyle = " style='${styles.joinToString(";")}'"
                                }
                                append("<span$runStyle>${text.esc()}</span>")
                            }
                            append("</p>\n")
                        }
                    }
                }
            }
        }.onFailure { error ->
            append("<p>Unable to read the Word file: ${error.message ?: "unknown error"}</p>")
        }

        append(HtmlTemplates.footer())
    }
}
