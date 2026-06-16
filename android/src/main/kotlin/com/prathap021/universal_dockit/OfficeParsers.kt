package com.prathap021.universal_dockit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.ensureActive
import org.apache.poi.hslf.extractor.QuickButCruddyTextExtractor
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.ooxml.POIXMLException
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.util.Units
import org.apache.poi.xslf.usermodel.XSLFAutoShape
import org.apache.poi.xslf.usermodel.XSLFPictureShape
import org.apache.poi.xslf.usermodel.XSLFShape
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.coroutines.coroutineContext
import kotlin.math.max

sealed class DocxElement {
    data class Paragraph(
        val text: String,
        val runs: List<TextRun>,
        val alignment: String? = null,
        val marginLeftPx: Int = 0,
        val spacingBeforePx: Int = 0,
        val spacingAfterPx: Int = 8,
        val lineHeight: Float? = null
    ) : DocxElement()

    data class Table(
        val rows: List<List<DocxTableCell>>
    ) : DocxElement()

    data class Image(
        val base64: String,
        val mimeType: String,
        val width: Double,
        val height: Double,
        val description: String?
    ) : DocxElement()
}

data class DocxTableCell(
    val text: String,
    val colSpan: Int = 1,
    val backgroundColor: String? = null,
    val alignment: String? = null
)

data class TextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val color: String? = null,
    val size: Float = 16f
)

data class ExcelWorkbook(
    val sheets: List<ExcelSheet>
)

data class ExcelSheet(
    val name: String,
    val rows: Map<Int, Map<Int, ExcelCell>>,
    val minRow: Int = 0,
    val maxRow: Int = 0,
    val minCol: Int = 0,
    val maxCol: Int = 0,
    val mergedRegions: List<ExcelMergedRegion> = emptyList(),
    val rowHeights: Map<Int, Float> = emptyMap(),
    val colWidths: Map<Int, Float> = emptyMap(),
    val hiddenRows: Set<Int> = emptySet(),
    val hiddenCols: Set<Int> = emptySet()
)

data class ExcelCell(
    val value: String,
    val style: ExcelCellStyle? = null
)

data class ExcelCellStyle(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val textColor: String? = null,
    val bgColor: String? = null,
    val horizontalAlign: String? = null,
    val verticalAlign: String? = null,
    val wrapText: Boolean = false
)

data class ExcelMergedRegion(
    val firstRow: Int,
    val lastRow: Int,
    val firstCol: Int,
    val lastCol: Int
)

data class SlideDocument(
    val slides: List<SlideItem>,
    val pageWidthEmu: Float = 12192000f,
    val pageHeightEmu: Float = 6858000f,
    val limitedMode: Boolean = false,
    val fallbackNote: String? = null
)

data class SlideItem(
    val index: Int,
    val elements: List<SlideGraphicElement>,
    val backgroundColor: Long = 0xFFFFFFFF
)

sealed class SlideGraphicElement {
    data class TextBlock(
        val text: String,
        val textColor: Long,
        val fontSize: Float,
        val isBold: Boolean,
        val isItalic: Boolean,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    ) : SlideGraphicElement()

    data class ImageBlock(
        val bitmap: Bitmap,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    ) : SlideGraphicElement()

    data class ShapeBlock(
        val shapeType: String,
        val color: Long,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    ) : SlideGraphicElement()
}

sealed class ParsedDocument {
    data class Word(
        val elements: List<DocxElement>,
        val limitedMode: Boolean = false,
        val fallbackNote: String? = null
    ) : ParsedDocument()
    data class Excel(val workbook: ExcelWorkbook) : ParsedDocument()
    data class Slides(val presentation: SlideDocument) : ParsedDocument()
    data class Text(val content: String) : ParsedDocument()
}

object OfficeParsers {
    private const val TAG = "OfficeParsers"

    suspend fun parseWord(filePath: String): ParsedDocument.Word {
        return try {
            if (filePath.endsWith(".doc", ignoreCase = true)) {
                parseDoc(filePath)
            } else {
                parseDocx(filePath)
            }
        } catch (e: POIXMLException) {
            Log.e(TAG, "Error parsing OOXML Word document, trying fallback", e)
            parseDocxFallback(filePath, "DOCX has unsupported/strict XML features. Rendered in text compatibility mode.")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Word document", e)
            if (filePath.endsWith(".docx", ignoreCase = true)) {
                parseDocxFallback(filePath, "Failed to parse DOCX layout. Rendered in text compatibility mode.")
            } else {
                ParsedDocument.Word(emptyList(), limitedMode = true, fallbackNote = "Failed to parse full Word content.")
            }
        }
    }

    private suspend fun parseDocx(filePath: String): ParsedDocument.Word {
        FileInputStream(filePath).use { input ->
            XWPFDocument(input).use { document ->
                val elements = mutableListOf<DocxElement>()
                for (bodyElement in document.bodyElements) {
                    coroutineContext.ensureActive()
                    when (bodyElement) {
                        is XWPFParagraph -> elements.addAll(paragraphElements(bodyElement))
                        is XWPFTable -> elements.add(tableElement(bodyElement))
                        else -> Unit
                    }
                }
                return ParsedDocument.Word(elements)
            }
        }
    }

    private suspend fun parseDoc(filePath: String): ParsedDocument.Word {
        FileInputStream(filePath).use { input ->
            HWPFDocument(input).use { document ->
                WordExtractor(document).use { extractor ->
                    val elements = extractor.paragraphText
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map {
                            coroutineContext.ensureActive()
                            DocxElement.Paragraph(it, listOf(TextRun(it)))
                        }
                    return ParsedDocument.Word(
                        elements = elements,
                        limitedMode = true,
                        fallbackNote = "Legacy .doc rendering is currently text-only and may lose tables/images/styles."
                    )
                }
            }
        }
    }

    private fun paragraphElements(paragraph: XWPFParagraph): List<DocxElement> {
        val elements = mutableListOf<DocxElement>()
        val runs = mutableListOf<TextRun>()

        fun flushParagraph() = flushParagraphWithStyle(elements, runs, paragraph)

        for (run in paragraph.runs) {
            val text = run.text().orEmpty()
            if (text.isNotEmpty()) {
                runs.add(
                    TextRun(
                        text = text,
                        isBold = run.isBold,
                        isItalic = run.isItalic,
                        isUnderline = run.underline.name != "NONE",
                        color = run.color,
                        size = run.fontSizeAsDouble?.toFloat() ?: 16f
                    )
                )
            }

            for (picture in run.embeddedPictures) {
                val pictureData = picture.pictureData ?: continue
                val bytes = pictureData.data ?: continue
                if (bytes.isEmpty()) continue
                flushParagraph()
                elements.add(
                    DocxElement.Image(
                        base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                        mimeType = pictureMimeType(pictureData.suggestFileExtension()),
                        width = normalizeWordImageSize(picture.width.toDouble()),
                        height = normalizeWordImageSize(picture.depth.toDouble()),
                        description = picture.description
                    )
                )
            }
        }

        if (runs.isEmpty() && paragraph.text.isNotBlank()) {
            runs.add(TextRun(paragraph.text))
        }
        flushParagraphWithStyle(
            elements = elements,
            runs = runs,
            paragraph = paragraph
        )
        return elements
    }

    private fun flushParagraphWithStyle(
        elements: MutableList<DocxElement>,
        runs: MutableList<TextRun>,
        paragraph: XWPFParagraph
    ) {
        val text = runs.joinToString("") { it.text }
        if (text.isBlank()) {
            runs.clear()
            return
        }
        elements.add(
            DocxElement.Paragraph(
                text = text,
                runs = runs.toList(),
                alignment = paragraph.alignment?.name?.lowercase(),
                marginLeftPx = twipsToPx(paragraph.indentationLeft),
                spacingBeforePx = twipsToPx(paragraph.spacingBefore),
                spacingAfterPx = twipsToPx(paragraph.spacingAfter).coerceAtLeast(8),
                lineHeight = paragraph.spacingBetween.toFloat().takeIf { it > 0f }
            )
        )
        runs.clear()
    }

    private fun tableElement(table: XWPFTable): DocxElement.Table {
        val rows = table.rows.map { row ->
            row.tableCells.map { cell ->
                DocxTableCell(
                    text = cell.bodyElements.joinToString("\n") { element ->
                        when (element) {
                            is XWPFParagraph -> element.text
                            is XWPFTable -> element.text
                            else -> ""
                        }
                    }.trim(),
                    colSpan = cell.gridSpan(),
                    backgroundColor = cell.color,
                    alignment = cell.paragraphs.firstOrNull()?.alignment?.name?.lowercase()
                )
            }
        }
        return DocxElement.Table(rows)
    }

    suspend fun parseExcel(filePath: String): ParsedDocument.Excel {
        return try {
            WorkbookFactory.create(File(filePath)).use { workbook ->
                val formatter = DataFormatter()
                val evaluator = workbook.creationHelper.createFormulaEvaluator()
                val sheets = mutableListOf<ExcelSheet>()
                for (sheetIndex in 0 until workbook.numberOfSheets) {
                    coroutineContext.ensureActive()
                    val sheet = workbook.getSheetAt(sheetIndex)
                    val rows = mutableMapOf<Int, MutableMap<Int, ExcelCell>>()
                    var minRow = Int.MAX_VALUE
                    var maxRow = -1
                    var minCol = Int.MAX_VALUE
                    var maxCol = -1
                    val mergedRegions = sheet.mergedRegions.map {
                        ExcelMergedRegion(
                            firstRow = it.firstRow,
                            lastRow = it.lastRow,
                            firstCol = it.firstColumn,
                            lastCol = it.lastColumn
                        )
                    }
                    val mergedAnchors = mergedRegions.associateBy { it.firstRow to it.firstCol }
                    val rowHeights = mutableMapOf<Int, Float>()
                    val colWidths = mutableMapOf<Int, Float>()
                    val hiddenRows = mutableSetOf<Int>()
                    val hiddenCols = mutableSetOf<Int>()
                    for (row in sheet) {
                        coroutineContext.ensureActive()
                        if (row.firstCellNum < 0 || row.lastCellNum < 0) continue
                        if (row.zeroHeight) hiddenRows.add(row.rowNum)
                        if (row.heightInPoints > 0f) rowHeights[row.rowNum] = row.heightInPoints
                        val rowMap = mutableMapOf<Int, ExcelCell>()
                        val rowFirstCol = row.firstCellNum.toInt()
                        val rowLastCol = row.lastCellNum.toInt() - 1
                        for (colIndex in rowFirstCol..rowLastCol) {
                            val cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                            val value = cell?.let { formatter.formatCellValue(it, evaluator) }.orEmpty()
                            val style = cell?.cellStyle?.toExcelCellStyle()
                            val hasMergedAnchor = mergedAnchors.containsKey(row.rowNum to colIndex)
                            if (value.isNotEmpty() || style != null || hasMergedAnchor) {
                                rowMap[colIndex] = ExcelCell(value = value, style = style)
                                minCol = minOf(minCol, colIndex)
                                maxCol = max(maxCol, colIndex)
                            }
                        }
                        if (rowMap.isNotEmpty()) {
                            rows[row.rowNum] = rowMap
                            minRow = minOf(minRow, row.rowNum)
                            maxRow = max(maxRow, row.rowNum)
                        }
                    }
                    if (maxCol >= 0) {
                        for (col in minCol.coerceAtLeast(0)..maxCol) {
                            if (sheet.isColumnHidden(col)) hiddenCols.add(col)
                            colWidths[col] = sheet.getColumnWidthInPixels(col)
                        }
                    }
                    for (region in mergedRegions) {
                        minRow = minOf(minRow, region.firstRow)
                        maxRow = max(maxRow, region.lastRow)
                        minCol = minOf(minCol, region.firstCol)
                        maxCol = max(maxCol, region.lastCol)
                    }
                    sheets.add(
                        ExcelSheet(
                            name = sheet.sheetName,
                            rows = rows,
                            minRow = if (minRow == Int.MAX_VALUE) 0 else minRow,
                            maxRow = max(maxRow, 0),
                            minCol = if (minCol == Int.MAX_VALUE) 0 else minCol,
                            maxCol = max(maxCol, 0),
                            mergedRegions = mergedRegions,
                            rowHeights = rowHeights,
                            colWidths = colWidths,
                            hiddenRows = hiddenRows,
                            hiddenCols = hiddenCols
                        )
                    )
                }
                ParsedDocument.Excel(ExcelWorkbook(sheets))
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Excel workbook unsupported by WorkbookFactory, trying XML fallback", e)
            parseExcelXmlFallback(filePath)
        } catch (e: POIXMLException) {
            Log.e(TAG, "Excel workbook OOXML parse issue, trying XML fallback", e)
            parseExcelXmlFallback(filePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Excel workbook", e)
            parseExcelXmlFallback(filePath)
        }
    }

    suspend fun parsePresentation(filePath: String): ParsedDocument.Slides {
        return try {
            if (filePath.endsWith(".ppt", ignoreCase = true)) {
                parsePpt(filePath)
            } else {
                parsePptx(filePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing PowerPoint presentation", e)
            ParsedDocument.Slides(SlideDocument(emptyList()))
        }
    }

    private suspend fun parsePptx(filePath: String): ParsedDocument.Slides {
        FileInputStream(filePath).use { input ->
            XMLSlideShow(input).use { slideShow ->
                val (pageWidthEmu, pageHeightEmu) = readSlideSizeFromPptx(filePath)
                val slides = slideShow.slides.mapIndexed { index, slide ->
                    coroutineContext.ensureActive()
                    val elements = buildList {
                        var fallbackTextY = 0.08f
                        for (shape in slide.shapes) {
                            val bounds = extractShapeBoundsFromXml(shape)
                            val x = ((bounds?.x ?: (0.08f * pageWidthEmu)) / pageWidthEmu).coerceIn(0f, 1f)
                            val y = ((bounds?.y ?: (fallbackTextY * pageHeightEmu)) / pageHeightEmu).coerceIn(0f, 1f)
                            val width = ((bounds?.width ?: (0.84f * pageWidthEmu)) / pageWidthEmu).coerceIn(0f, 1f)
                            val height = ((bounds?.height ?: (0.12f * pageHeightEmu)) / pageHeightEmu).coerceIn(0f, 1f)
                            when (shape) {
                                is XSLFTextShape -> {
                                    val text = shape.text.orEmpty().trim()
                                    if (text.isEmpty()) continue
                                    val firstRun = shape.textParagraphs
                                        .firstOrNull()
                                        ?.textRuns
                                        ?.firstOrNull()
                                    add(
                                        SlideGraphicElement.TextBlock(
                                            text = text,
                                            textColor = 0xFF000000,
                                            fontSize = (firstRun?.fontSize ?: 16.0).toFloat(),
                                            isBold = firstRun?.isBold ?: false,
                                            isItalic = firstRun?.isItalic ?: false,
                                            x = x,
                                            y = y,
                                            width = width,
                                            height = height
                                        )
                                    )
                                    if (bounds == null) {
                                        fallbackTextY = (fallbackTextY + 0.14f).coerceAtMost(0.88f)
                                    }
                                }
                                is XSLFPictureShape -> {
                                    val bytes = shape.pictureData?.data ?: continue
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue
                                    add(
                                        SlideGraphicElement.ImageBlock(
                                            bitmap = bitmap,
                                            x = x,
                                            y = y,
                                            width = width,
                                            height = height
                                        )
                                    )
                                }
                                is XSLFAutoShape -> {
                                    add(
                                        SlideGraphicElement.ShapeBlock(
                                            shapeType = shape.shapeName ?: "shape",
                                            color = 0xFFD9D9D9,
                                            x = x,
                                            y = y,
                                            width = width,
                                            height = height
                                        )
                                    )
                                }
                            }
                        }
                    }
                    SlideItem(index + 1, elements)
                }
                return ParsedDocument.Slides(
                    SlideDocument(
                        slides = slides,
                        pageWidthEmu = pageWidthEmu,
                        pageHeightEmu = pageHeightEmu
                    )
                )
            }
        }
    }

    private data class ShapeBounds(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    private fun extractShapeBoundsFromXml(shape: XSLFShape): ShapeBounds? {
        val xml = try {
            val method = shape.javaClass.methods.firstOrNull { it.name == "getXmlObject" } ?: return null
            method.invoke(shape)?.toString().orEmpty()
        } catch (_: Throwable) {
            return null
        }
        val xfrmMatch = Regex("<(?:a|p):xfrm[\\s\\S]*?</(?:a|p):xfrm>").find(xml)?.value ?: xml
        val offTag = Regex("<(?:a|p):off[^>]*/?>").find(xfrmMatch)?.value.orEmpty()
        val extTag = Regex("<(?:a|p):ext[^>]*/?>").find(xfrmMatch)?.value.orEmpty()
        val x = Regex("""\bx="(-?\d+)"""").find(offTag)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
        val y = Regex("""\by="(-?\d+)"""").find(offTag)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
        val width = Regex("""\bcx="(\d+)"""").find(extTag)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: return null
        val height = Regex("""\bcy="(\d+)"""").find(extTag)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: return null
        if (width <= 0f || height <= 0f) return null
        return ShapeBounds(x = x, y = y, width = width, height = height)
    }

    private fun readSlideSizeFromPptx(filePath: String): Pair<Float, Float> {
        return try {
            ZipFile(filePath).use { zip ->
                val entry = zip.getEntry("ppt/presentation.xml") ?: return 12192000f to 6858000f
                val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                val sldSzTag = Regex("<p:sldSz[^>]*/>").find(xml)?.value
                    ?: Regex("<p:sldSz[^>]*>").find(xml)?.value
                    ?: return 12192000f to 6858000f
                val cx = Regex("""\bcx="(\d+)"""").find(sldSzTag)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 12192000f
                val cy = Regex("""\bcy="(\d+)"""").find(sldSzTag)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 6858000f
                cx.coerceAtLeast(1f) to cy.coerceAtLeast(1f)
            }
        } catch (_: Exception) {
            12192000f to 6858000f
        }
    }

    private suspend fun parsePpt(filePath: String): ParsedDocument.Slides {
        FileInputStream(filePath).use { input ->
            val extractor = QuickButCruddyTextExtractor(input)
            try {
                val slides = extractor.textAsString
                    .split("\n\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .mapIndexed { index, text ->
                        coroutineContext.ensureActive()
                        SlideItem(
                            index = index + 1,
                            elements = listOf(
                                SlideGraphicElement.TextBlock(
                                    text = text,
                                    textColor = 0xFF000000,
                                    fontSize = 18f,
                                    isBold = false,
                                    isItalic = false,
                                    x = 0.08f,
                                    y = 0.08f,
                                    width = 0.84f,
                                    height = 0.84f
                                )
                            )
                        )
                    }
                return ParsedDocument.Slides(
                    SlideDocument(
                        slides = slides,
                        pageWidthEmu = 12192000f,
                        pageHeightEmu = 6858000f,
                        limitedMode = true,
                        fallbackNote = "Legacy .ppt is rendered using text extraction only; layout and media may be missing."
                    )
                )
            } finally {
                extractor.close()
            }
        }
    }

    private fun pictureMimeType(extension: String?): String {
        return when (extension?.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> "image/png"
        }
    }

    private fun parseDocxFallback(filePath: String, note: String): ParsedDocument.Word {
        return try {
            val paragraphs = mutableListOf<String>()
            ZipFile(filePath).use { zip ->
                val entry = zip.getEntry("word/document.xml") ?: return ParsedDocument.Word(
                    emptyList(),
                    limitedMode = true,
                    fallbackNote = note
                )
                val xml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                Regex("<w:p[\\s\\S]*?</w:p>").findAll(xml).forEach { p ->
                    val raw = p.value
                    val text = Regex("<w:t[^>]*>([\\s\\S]*?)</w:t>")
                        .findAll(raw)
                        .joinToString("") { it.groupValues[1] }
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .trim()
                    if (text.isNotEmpty()) paragraphs.add(text)
                }
            }
            ParsedDocument.Word(
                elements = paragraphs.map { DocxElement.Paragraph(it, listOf(TextRun(it))) },
                limitedMode = true,
                fallbackNote = note
            )
        } catch (inner: Exception) {
            Log.e(TAG, "DOCX fallback also failed", inner)
            ParsedDocument.Word(emptyList(), limitedMode = true, fallbackNote = "Failed to parse DOCX document.")
        }
    }

    private fun parseExcelXmlFallback(filePath: String): ParsedDocument.Excel {
        return try {
            val file = File(filePath)
            if (!file.exists()) return ParsedDocument.Excel(ExcelWorkbook(emptyList()))
            val text = file.bufferedReader().use { it.readText() }
            // SpreadsheetML 2003 XML fallback
            if (text.contains("<Workbook", ignoreCase = true) && text.contains("urn:schemas-microsoft-com:office:spreadsheet", ignoreCase = true)) {
                val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val xmlDoc = docBuilder.parse(file)
                xmlDoc.documentElement.normalize()
                val worksheetNodes = xmlDoc.getElementsByTagName("Worksheet")
                val sheets = mutableListOf<ExcelSheet>()
                for (i in 0 until worksheetNodes.length) {
                    val ws = worksheetNodes.item(i)
                    val wsName = ws.attributes?.getNamedItem("ss:Name")?.nodeValue
                        ?: ws.attributes?.getNamedItem("Name")?.nodeValue
                        ?: "Sheet ${i + 1}"
                    val rowMap = mutableMapOf<Int, MutableMap<Int, ExcelCell>>()
                    val rowNodes = (ws as? org.w3c.dom.Element)?.getElementsByTagName("Row")
                    var rowIndex = 0
                    var maxCol = 0
                    if (rowNodes != null) {
                        for (r in 0 until rowNodes.length) {
                            val rowNode = rowNodes.item(r) as? org.w3c.dom.Element ?: continue
                            val currentRow = rowNode.getAttribute("ss:Index").toIntOrNull()?.minus(1) ?: rowIndex
                            val cellNodes = rowNode.getElementsByTagName("Cell")
                            val cells = mutableMapOf<Int, ExcelCell>()
                            var colIndex = 0
                            for (c in 0 until cellNodes.length) {
                                val cellNode = cellNodes.item(c) as? org.w3c.dom.Element ?: continue
                                val currentCol = cellNode.getAttribute("ss:Index").toIntOrNull()?.minus(1) ?: colIndex
                                val dataNode = cellNode.getElementsByTagName("Data").item(0)
                                val value = dataNode?.textContent?.trim().orEmpty()
                                if (value.isNotEmpty()) {
                                    cells[currentCol] = ExcelCell(value = value)
                                    maxCol = max(maxCol, currentCol)
                                }
                                colIndex = currentCol + 1
                            }
                            if (cells.isNotEmpty()) rowMap[currentRow] = cells
                            rowIndex = currentRow + 1
                        }
                    }
                    sheets.add(
                        ExcelSheet(
                            name = wsName,
                            rows = rowMap,
                            minRow = rowMap.keys.minOrNull() ?: 0,
                            maxRow = rowMap.keys.maxOrNull() ?: 0,
                            minCol = rowMap.values.flatMap { it.keys }.minOrNull() ?: 0,
                            maxCol = maxCol
                        )
                    )
                }
                return ParsedDocument.Excel(ExcelWorkbook(sheets))
            }
            ParsedDocument.Excel(ExcelWorkbook(emptyList()))
        } catch (e: Exception) {
            Log.e(TAG, "Excel XML fallback failed", e)
            ParsedDocument.Excel(ExcelWorkbook(emptyList()))
        }
    }

    private fun normalizeWordImageSize(rawValue: Double): Double {
        if (rawValue <= 0) return 0.0
        // POI may expose image dimensions in EMU for DOCX.
        return if (rawValue > 2000) rawValue / Units.EMU_PER_PIXEL else rawValue
    }

    private fun twipsToPx(twips: Int): Int {
        if (twips <= 0) return 0
        return (twips / 15f).toInt()
    }

    private fun XWPFTableCell.gridSpan(): Int {
        return ctTc?.tcPr?.gridSpan?.`val`?.toInt() ?: 1
    }

    private fun CellStyle.toExcelCellStyle(): ExcelCellStyle? {
        val horizontalAlign = when (alignment) {
            HorizontalAlignment.LEFT -> "left"
            HorizontalAlignment.CENTER -> "center"
            HorizontalAlignment.RIGHT -> "right"
            HorizontalAlignment.JUSTIFY -> "justify"
            else -> null
        }
        val verticalAlign = when (verticalAlignment) {
            VerticalAlignment.TOP -> "top"
            VerticalAlignment.CENTER -> "middle"
            VerticalAlignment.BOTTOM -> "bottom"
            else -> null
        }
        val hasFill = fillPattern != FillPatternType.NO_FILL
        return ExcelCellStyle(
            isBold = false,
            isItalic = false,
            textColor = null,
            bgColor = if (hasFill) indexedColorToHex(fillForegroundColor.toInt()) else null,
            horizontalAlign = horizontalAlign,
            verticalAlign = verticalAlign,
            wrapText = wrapText
        )
    }

    private fun indexedColorToHex(index: Int): String? {
        return when (index) {
            8 -> "#000000"
            9 -> "#FFFFFF"
            10 -> "#FF0000"
            11 -> "#00FF00"
            12 -> "#0000FF"
            13 -> "#FFFF00"
            14 -> "#FF00FF"
            15 -> "#00FFFF"
            else -> null
        }
    }

}
