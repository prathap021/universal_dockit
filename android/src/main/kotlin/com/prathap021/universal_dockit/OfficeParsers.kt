package com.prathap021.universal_dockit

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

// --- Models for Word Document Viewer (.docx) ---
sealed class DocxElement {
    data class Paragraph(
        val text: String,
        val runs: List<TextRun>
    ) : DocxElement()

    data class Table(
        val rows: List<List<String>>
    ) : DocxElement()
}

data class TextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val color: String? = null,
    val size: Float = 16f // sp
)

// --- Models for Spreadsheet Viewer (.xlsx) ---
data class ExcelWorkbook(
    val sheets: List<ExcelSheet>
)

data class ExcelSheet(
    val name: String,
    val rows: Map<Int, Map<Int, String>>, // row_idx -> col_idx -> cell_value String
    val maxRow: Int = 0,
    val maxCol: Int = 0
)

// --- Models for PowerPoint Presentation (.pptx) ---
data class SlideDocument(
    val slides: List<SlideItem>
)

data class SlideItem(
    val index: Int,
    val elements: List<SlideGraphicElement>,
    val backgroundColor: Long = 0xFFFFFFFF
)

sealed class SlideGraphicElement {
    data class TextBlock(
        val text: String,
        val textColor: Long, // ARGB
        val fontSize: Float, // sp
        val isBold: Boolean,
        val isItalic: Boolean,
        val x: Float, // 0..1
        val y: Float, // 0..1
        val width: Float, // 0..1
        val height: Float // 0..1
    ) : SlideGraphicElement()

    data class ImageBlock(
        val bitmap: android.graphics.Bitmap,
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

// --- High level parsed result ---
sealed class ParsedDocument {
    data class Word(val elements: List<DocxElement>) : ParsedDocument()
    data class Excel(val workbook: ExcelWorkbook) : ParsedDocument()
    data class Slides(val presentation: SlideDocument) : ParsedDocument()
    data class Text(val content: String) : ParsedDocument()
}

object OfficeParsers {
    private const val TAG = "OfficeParsers"

    // Parse DOCX
    suspend fun parseDocx(inputStream: InputStream): ParsedDocument.Word {
        val elements = mutableListOf<DocxElement>()
        try {
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            while (entry != null) {
                coroutineContext.ensureActive()
                if (entry.name == "word/document.xml") {
                    elements.addAll(readDocumentXml(zip))
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            zip.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing docx", e)
        }
        return ParsedDocument.Word(elements)
    }

    private suspend fun readDocumentXml(inputStream: InputStream): List<DocxElement> {
        val list = mutableListOf<DocxElement>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType

            var inTable = false
            val currentTableRows = mutableListOf<List<String>>()
            var currentRowCells = mutableListOf<String>()
            var currentCellText = StringBuilder()

            var currentParagraphRuns = mutableListOf<TextRun>()
            var currentRunText = StringBuilder()
            var isBold = false
            var isItalic = false
            var inParagraph = false
            var inRun = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                coroutineContext.ensureActive()
                val tag = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "tbl" -> {
                                inTable = true
                                currentTableRows.clear()
                            }
                            "tr" -> {
                                currentRowCells = mutableListOf()
                            }
                            "tc" -> {
                                currentCellText = StringBuilder()
                            }
                            "p" -> {
                                inParagraph = true
                                currentParagraphRuns = mutableListOf()
                            }
                            "r" -> {
                                inRun = true
                                currentRunText = StringBuilder()
                                isBold = false
                                isItalic = false
                            }
                            "b" -> {
                                isBold = true
                            }
                            "i" -> {
                                isItalic = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inRun) {
                            currentRunText.append(parser.text)
                        } else if (inTable && !inParagraph) {
                            currentCellText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tag) {
                            "r" -> {
                                inRun = false
                                if (currentRunText.isNotEmpty()) {
                                    currentParagraphRuns.add(
                                        TextRun(
                                            text = currentRunText.toString(),
                                            isBold = isBold,
                                            isItalic = isItalic
                                        )
                                    )
                                }
                            }
                            "p" -> {
                                inParagraph = false
                                val fullText = currentParagraphRuns.joinToString("") { it.text }
                                if (fullText.isNotBlank()) {
                                    if (inTable) {
                                        currentCellText.append(fullText).append(" ")
                                    } else {
                                        list.add(DocxElement.Paragraph(fullText, currentParagraphRuns))
                                    }
                                }
                            }
                            "tc" -> {
                                currentRowCells.add(currentCellText.toString().trim())
                            }
                            "tr" -> {
                                if (currentRowCells.isNotEmpty()) {
                                    currentTableRows.add(currentRowCells)
                                }
                            }
                            "tbl" -> {
                                inTable = false
                                if (currentTableRows.isNotEmpty()) {
                                    list.add(DocxElement.Table(currentTableRows.toList()))
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in readDocumentXml", e)
        }
        return list
    }

    // Parse XLSX
    suspend fun parseXlsx(inputStream: InputStream): ParsedDocument.Excel {
        val sheets = mutableListOf<ExcelSheet>()
        val sharedStrings = mutableListOf<String>()
        val sheetsToParse = mutableListOf<Pair<String, String>>() // (name, file_path)

        try {
            // First pass or double scanning requires a copy or multiple files, but in ZipInputStream we can parse in stream
            // Since we can scan zip file sequential, let's process sharedStrings first if possible.
            // But ZipInputStream stream is forward-only. A very elegant way is to do scan the zip entries.
            // We can memorize the files in memory or stream it. Since they are small, XML parsing sheets on the fly is highly efficient.
            // Let's do a double stream or cache worksheets in a Map of bytes of zip entries so we can read sharedStrings first, then sheets!
            val cachedEntries = mutableMapOf<String, ByteArray>()
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            while (entry != null) {
                coroutineContext.ensureActive()
                if (entry.name == "xl/sharedStrings.xml" || 
                    entry.name == "xl/workbook.xml" || 
                    entry.name.startsWith("xl/worksheets/sheet")) {
                    cachedEntries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            zip.close()

            // 1. Parse sharedStrings
            val sharedStringsBytes = cachedEntries["xl/sharedStrings.xml"]
            if (sharedStringsBytes != null) {
                sharedStrings.addAll(parseSharedStrings(sharedStringsBytes.inputStream()))
            }

            // 2. Parse workbook to get sheet names
            val workbookBytes = cachedEntries["xl/workbook.xml"]
            val sheetIdsToNames = mutableMapOf<String, String>() // rId or sheetId to name
            if (workbookBytes != null) {
                sheetIdsToNames.putAll(parseWorkbook(workbookBytes.inputStream()))
            }

            // 3. Parse sheets
            // Group and sort sheet entries
            val sheetEntries = cachedEntries.keys.filter { it.startsWith("xl/worksheets/sheet") }
                .sortedWith(compareBy { entryName -> 
                    entryName.filter { it.isDigit() }.toIntOrNull() ?: 0 
                })

            sheetEntries.forEachIndexed { index, entryName ->
                coroutineContext.ensureActive()
                val sheetName = sheetIdsToNames[entryName] ?: sheetIdsToNames[(index + 1).toString()] ?: "Sheet ${index + 1}"
                val sheetBytes = cachedEntries[entryName]
                if (sheetBytes != null) {
                    val sheet = parseSingleSheet(sheetName, sheetBytes.inputStream(), sharedStrings)
                    sheets.add(sheet)
                }
            }

            if (sheets.isEmpty() && cachedEntries.isNotEmpty()) {
                // Fallback for sheets list
                sheets.add(ExcelSheet("Sheet 1", emptyMap(), 0, 0))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing xlsx", e)
        }

        return ParsedDocument.Excel(ExcelWorkbook(sheets))
    }

    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val list = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            var currentString = StringBuilder()
            var inT = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tag == "t") {
                            inT = true
                            currentString = StringBuilder()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT) {
                            currentString.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag == "t") {
                            inT = false
                            list.add(currentString.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseSharedStrings", e)
        }
        return list
    }

    private fun parseWorkbook(inputStream: InputStream): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                    val name = parser.getAttributeValue(null, "name") ?: ""
                    val sheetId = parser.getAttributeValue(null, "sheetId") ?: ""
                    val rId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id") ?: ""
                    
                    if (name.isNotBlank()) {
                        map[sheetId] = name
                        if (rId.isNotBlank()) {
                            // Link both just in case
                            map["xl/worksheets/sheet$sheetId.xml"] = name
                            map["xl/worksheets/sheet${rId.replace("rId", "")}.xml"] = name
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseWorkbook", e)
        }
        return map
    }

    private suspend fun parseSingleSheet(
        sheetName: String, 
        inputStream: InputStream, 
        sharedStrings: List<String>
    ): ExcelSheet {
        val rRows = mutableMapOf<Int, MutableMap<Int, String>>()
        var maxRow = 0
        var maxCol = 0

        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            var inCell = false
            var cellType = ""
            var currentVal = StringBuilder()
            var cellRef = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                coroutineContext.ensureActive()
                val tag = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tag == "c") {
                            inCell = true
                            cellRef = parser.getAttributeValue(null, "r") ?: ""
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            currentVal = StringBuilder()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inCell) {
                            currentVal.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag == "c") {
                            inCell = false
                            val cellStrValue = currentVal.toString().trim()
                            if (cellRef.isNotBlank() && cellStrValue.isNotBlank()) {
                                // Extract row and column index from reference (e.g. "B4" -> col 1, row 3)
                                val colIndices = cellRef.takeWhile { it.isLetter() }
                                val rowIndices = cellRef.dropWhile { it.isLetter() }

                                val colIdx = excelColToNumber(colIndices) - 1
                                val rowIdx = (rowIndices.toIntOrNull() ?: 1) - 1

                                if (colIdx >= 0 && rowIdx >= 0) {
                                    if (rowIdx > maxRow) maxRow = rowIdx
                                    if (colIdx > maxCol) maxCol = colIdx

                                    val finalVal = if (cellType == "s") {
                                        val stringIdx = cellStrValue.toIntOrNull() ?: -1
                                        if (stringIdx in sharedStrings.indices) {
                                            sharedStrings[stringIdx]
                                        } else {
                                            cellStrValue
                                        }
                                    } else {
                                        cellStrValue
                                    }

                                    val rowData = rRows.getOrPut(rowIdx) { mutableMapOf() }
                                    rowData[colIdx] = finalVal
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseSingleSheet", e)
        }

        // Clean-up sheet mapping
        val cleanRows = rRows.mapValues { it.value.toMap() }
        return ExcelSheet(sheetName, cleanRows, maxRow, maxCol)
    }

    private fun excelColToNumber(col: String): Int {
        var num = 0
        for (i in 0 until col.length) {
            num = num * 26 + (col[i] - 'A' + 1)
        }
        return num
    }

    // Parse PPTX (Slides)
    suspend fun parsePptx(inputStream: InputStream): ParsedDocument.Slides {
        val list = mutableListOf<SlideItem>()
        try {
            val cachedEntries = mutableMapOf<String, ByteArray>()
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            while (entry != null) {
                coroutineContext.ensureActive()
                if (entry.name == "ppt/presentation.xml" ||
                     (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) ||
                     (entry.name.startsWith("ppt/slides/_rels/slide") && entry.name.endsWith(".xml.rels")) ||
                     entry.name.startsWith("ppt/media/")
                ) {
                    cachedEntries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            zip.close()

            // 1. Get slide dimensions from ppt/presentation.xml
            var slideCx = 12192000f // 16:9 default
            var slideCy = 6858000f
            val presentationBytes = cachedEntries["ppt/presentation.xml"]
            if (presentationBytes != null) {
                try {
                    val parser = Xml.newPullParser()
                    parser.setInput(presentationBytes.inputStream(), "UTF-8")
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && parser.name == "sldSz") {
                            val cxStr = parser.getAttributeValue(null, "cx")
                            val cyStr = parser.getAttributeValue(null, "cy")
                            if (cxStr != null && cyStr != null) {
                                slideCx = cxStr.toFloatOrNull() ?: slideCx
                                slideCy = cyStr.toFloatOrNull() ?: slideCy
                            }
                            break
                        }
                        eventType = parser.next()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing sldSz", e)
                }
            }

            // 2. Sort slide files naturally (slide1.xml, slide2.xml, slide10.xml)
            val slideEntries = cachedEntries.keys.filter { 
                it.startsWith("ppt/slides/slide") && !it.contains("_rels") && it.endsWith(".xml") 
            }.sortedWith(compareBy { entryName ->
                entryName.filter { it.isDigit() }.toIntOrNull() ?: 0
            })

            slideEntries.forEachIndexed { index, entryName ->
                coroutineContext.ensureActive()
                val slideBytes = cachedEntries[entryName]
                if (slideBytes != null) {
                    // Parse slide relations to resolve images
                    val slideNum = entryName.filter { it.isDigit() }
                    val relsEntryName = "ppt/slides/_rels/slide$slideNum.xml.rels"
                    val relsBytes = cachedEntries[relsEntryName]
                    val relsMap = mutableMapOf<String, String>() // rId -> Target
                    if (relsBytes != null) {
                        try {
                            val parser = Xml.newPullParser()
                            parser.setInput(relsBytes.inputStream(), "UTF-8")
                            var eventType = parser.eventType
                            while (eventType != XmlPullParser.END_DOCUMENT) {
                                if (eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                                    val rId = parser.getAttributeValue(null, "Id")
                                    val target = parser.getAttributeValue(null, "Target")
                                    if (rId != null && target != null) {
                                        relsMap[rId] = target
                                    }
                                }
                                eventType = parser.next()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing rels for slide $slideNum", e)
                        }
                    }

                    val elements = parseSingleSlideGraphic(slideBytes.inputStream(), relsMap, cachedEntries, slideCx, slideCy)
                    val bgColor = parseSlideBackgroundColor(slideBytes.inputStream())
                    list.add(SlideItem(index + 1, elements, bgColor))
                }
            }

            if (list.isEmpty()) {
                list.add(SlideItem(1, listOf(
                    SlideGraphicElement.TextBlock("Empty Presentation", 0xFF000000, 24f, true, false, 0.1f, 0.1f, 0.8f, 0.2f),
                    SlideGraphicElement.TextBlock("No slides parsed", 0xFF555555, 16f, false, false, 0.1f, 0.3f, 0.8f, 0.1f)
                ), 0xFFFFFFFF))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing pptx", e)
        }
        return ParsedDocument.Slides(SlideDocument(list))
    }

    private fun parseSlideBackgroundColor(inputStream: InputStream): Long {
        var color = 0xFFFFFFFF // Default white
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            var inBg = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tag == "bg") {
                            inBg = true
                        } else if (inBg && tag == "srgbClr") {
                            val hex = parser.getAttributeValue(null, "val")
                            if (hex != null) {
                                color = 0xFF000000L or hex.toLong(16)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag == "bg") inBg = false
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseSlideBackgroundColor", e)
        }
        return color
    }

    private suspend fun parseSingleSlideGraphic(
        inputStream: InputStream,
        relsMap: Map<String, String>,
        cachedEntries: Map<String, ByteArray>,
        slideCx: Float,
        slideCy: Float
    ): List<SlideGraphicElement> {
        val elements = mutableListOf<SlideGraphicElement>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType

            var inSp = false
            var inPic = false
            var inTxBody = false
            var inT = false
            
            // Current element state
            var currentX = 0f
            var currentY = 0f
            var currentW = 0f
            var currentH = 0f
            
            // Text block state
            var isBold = false
            var isItalic = false
            var fontSize = 18f
            var textColor = 0xFF000000L
            val textBuilder = StringBuilder()
            
            // Image block state
            var rEmbed: String? = null
            
            // Shape fill state
            var shapeColor: Long? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                coroutineContext.ensureActive()
                val tag = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "sp" -> {
                                inSp = true
                                inPic = false
                                inTxBody = false
                                textBuilder.clear()
                                isBold = false
                                isItalic = false
                                fontSize = 18f
                                textColor = 0xFF000000L
                                shapeColor = null
                                currentX = 0f; currentY = 0f; currentW = 0f; currentH = 0f
                            }
                            "pic" -> {
                                inPic = true
                                inSp = false
                                inTxBody = false
                                rEmbed = null
                                currentX = 0f; currentY = 0f; currentW = 0f; currentH = 0f
                            }
                            "txBody" -> {
                                inTxBody = true
                            }
                            "off" -> {
                                if (inSp || inPic) {
                                    val xStr = parser.getAttributeValue(null, "x")
                                    val yStr = parser.getAttributeValue(null, "y")
                                    if (xStr != null && yStr != null) {
                                        currentX = (xStr.toFloatOrNull() ?: 0f) / slideCx
                                        currentY = (yStr.toFloatOrNull() ?: 0f) / slideCy
                                    }
                                }
                            }
                            "ext" -> {
                                if (inSp || inPic) {
                                    val cxStr = parser.getAttributeValue(null, "cx")
                                    val cyStr = parser.getAttributeValue(null, "cy")
                                    if (cxStr != null && cyStr != null) {
                                        currentW = (cxStr.toFloatOrNull() ?: 0f) / slideCx
                                        currentH = (cyStr.toFloatOrNull() ?: 0f) / slideCy
                                    }
                                }
                            }
                            "srgbClr" -> {
                                val hex = parser.getAttributeValue(null, "val")
                                if (hex != null) {
                                    val parsedColor = 0xFF000000L or hex.toLong(16)
                                    if (inSp) {
                                        if (inTxBody) {
                                            textColor = parsedColor
                                        } else {
                                            shapeColor = parsedColor
                                        }
                                    }
                                }
                            }
                            "blip" -> {
                                if (inPic) {
                                    rEmbed = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed")
                                        ?: parser.getAttributeValue(null, "embed")
                                }
                            }
                            "rPr" -> {
                                if (inSp) {
                                    val szStr = parser.getAttributeValue(null, "sz")
                                    if (szStr != null) {
                                        fontSize = (szStr.toFloatOrNull() ?: 1800f) / 100f
                                    }
                                    val bStr = parser.getAttributeValue(null, "b")
                                    if (bStr == "1" || bStr == "true") {
                                        isBold = true
                                    }
                                    val iStr = parser.getAttributeValue(null, "i")
                                    if (iStr == "1" || iStr == "true") {
                                        isItalic = true
                                    }
                                }
                            }
                            "t" -> {
                                inT = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT) {
                            textBuilder.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tag) {
                            "t" -> {
                                inT = false
                            }
                            "txBody" -> {
                                inTxBody = false
                            }
                            "sp" -> {
                                inSp = false
                                val fullText = textBuilder.toString().trim()
                                if (fullText.isNotEmpty()) {
                                    elements.add(
                                        SlideGraphicElement.TextBlock(
                                            text = fullText,
                                            textColor = textColor,
                                            fontSize = fontSize,
                                            isBold = isBold,
                                            isItalic = isItalic,
                                            x = currentX,
                                            y = currentY,
                                            width = currentW,
                                            height = currentH
                                        )
                                    )
                                } else if (shapeColor != null) {
                                    elements.add(
                                        SlideGraphicElement.ShapeBlock(
                                            shapeType = "rect",
                                            color = shapeColor!!,
                                            x = currentX,
                                            y = currentY,
                                            width = currentW,
                                            height = currentH
                                        )
                                    )
                                }
                            }
                            "pic" -> {
                                inPic = false
                                if (rEmbed != null) {
                                    val targetPath = relsMap[rEmbed]
                                    if (targetPath != null) {
                                        val cleanPath = if (targetPath.startsWith("../")) {
                                            "ppt/" + targetPath.removePrefix("../")
                                        } else {
                                            targetPath
                                        }
                                        val imageBytes = cachedEntries[cleanPath]
                                        if (imageBytes != null) {
                                            val bitmap = try {
                                                android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                            } catch (e: Exception) {
                                                null
                                            }
                                            if (bitmap != null) {
                                                elements.add(
                                                    SlideGraphicElement.ImageBlock(
                                                        bitmap = bitmap,
                                                        x = currentX,
                                                        y = currentY,
                                                        width = currentW,
                                                        height = currentH
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseSingleSlideGraphic", e)
        }
        return elements
    }

    fun getCacheFileForUri(context: Context, uri: Uri): java.io.File {
        val cacheDir = java.io.File(context.cacheDir, "recent_docs")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val hash = uri.toString().hashCode().toString()
        val ext = getFileName(context, uri).substringAfterLast(".", "bin").lowercase()
        return java.io.File(cacheDir, "${hash}.$ext")
    }

    fun cleanOldCaches(context: Context, currentUri: Uri) {
        try {
            val db = FeathurDatabase.getDatabase(context)
            val recentUris = db.historyDao().getRecentUriStringsSync()
            val allowedHashes = (recentUris.map { Uri.parse(it).toString().hashCode().toString() } + currentUri.toString().hashCode().toString()).toSet()
            
            val cacheDir = java.io.File(context.cacheDir, "recent_docs")
            if (cacheDir.exists() && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { file ->
                    val nameWithoutExt = file.name.substringBeforeLast(".")
                    if (!allowedHashes.contains(nameWithoutExt)) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old caches", e)
        }
    }

    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }
        if (uri.scheme == "content") {
            try {
                val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
                val cursor = context.contentResolver.query(uri, projection, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val idx = it.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                        if (idx != -1) {
                            return it.getString(idx)
                        }
                    }
                }
            } catch (e: Exception) {
            }
            val path = uri.path
            if (path != null) {
                if (path.contains("document/raw:")) {
                    return path.substringAfter("document/raw:")
                }
                if (path.contains("/document/primary:")) {
                    val primaryPath = path.substringAfter("/document/primary:")
                    return "/storage/emulated/0/$primaryPath"
                }
            }
        }
        return null
    }

    fun hasFilePermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    // Comprehensive parsing entry-point
    suspend fun parseUri(context: Context, uri: Uri): ParsedDocument {
        val cacheFile = getCacheFileForUri(context, uri)
        var inputStream: InputStream? = null

        try {
            // First try reading via ContentResolver
            inputStream = context.contentResolver.openInputStream(uri)
            
            // If succeeded, write a copy to cache
            if (inputStream != null) {
                try {
                    cacheFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()
                    inputStream = cacheFile.inputStream()
                    cleanOldCaches(context, uri)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write content stream to cache", e)
                }
            }
        } catch (e: Exception) {
            // If ContentResolver fails, try direct File API if permission is granted
            if (hasFilePermission(context)) {
                val filePath = getFilePathFromUri(context, uri)
                if (filePath != null) {
                    try {
                        val file = java.io.File(filePath)
                        if (file.exists() && file.canRead()) {
                            file.inputStream().use { input ->
                                cacheFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            inputStream = cacheFile.inputStream()
                            cleanOldCaches(context, uri)
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Failed direct file fallback read/cache", ex)
                    }
                }
            }

            // Final fallback: check cache file
            if (inputStream == null) {
                if (cacheFile.exists()) {
                    try {
                        inputStream = cacheFile.inputStream()
                    } catch (ex: Exception) {
                        throw e
                    }
                } else {
                    throw e
                }
            }
        }

        val finalStream = inputStream ?: throw IllegalArgumentException("Could not open input stream")
        val fileName = getFileName(context, uri).lowercase()

        return finalStream.use { stream ->
            when {
                fileName.endsWith(".docx") || fileName.endsWith(".doc") -> {
                    parseDocx(stream)
                }
                fileName.endsWith(".xlsx") || fileName.endsWith(".xls") -> {
                    parseXlsx(stream)
                }
                fileName.endsWith(".pptx") || fileName.endsWith(".ppt") -> {
                    parsePptx(stream)
                }
                else -> {
                    val lines = stream.bufferedReader().use { it.readText() }
                    ParsedDocument.Text(lines)
                }
            }
        }
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "Document"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying filename, using fallback", e)
            val path = uri.path
            if (path != null) {
                val lastSegment = path.substringAfterLast('/')
                if (lastSegment.isNotEmpty()) {
                    name = lastSegment
                }
            }
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = it.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying file size, using fallback", e)
            try {
                val cacheFile = getCacheFileForUri(context, uri)
                if (cacheFile.exists()) {
                    size = cacheFile.length()
                }
            } catch (ex: Exception) {
            }
        }
        return size
    }
}
