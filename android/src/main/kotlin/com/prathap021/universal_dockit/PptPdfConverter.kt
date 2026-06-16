package com.prathap021.universal_dockit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Converts PowerPoint files to PDF for viewing on a background thread via [PdfDocument].
 */
internal object PptPdfConverter {
    private const val CACHE_VERSION = 4
    private const val EMU_PER_INCH = 914400f
    private const val POINTS_PER_INCH = 72f

    fun cachedPdfFile(context: Context, sourcePath: String): File {
        val name = sourcePath.hashCode().toString(16)
        return File(context.cacheDir, "ppt_converted_v${CACHE_VERSION}_$name.pdf")
    }

    suspend fun convertToPdf(filePath: String, outputFile: File) {
        val parsed = OfficeParsers.parsePresentation(filePath)
        val presentation = parsed.presentation
        val pageWidthPt = emuToPoints(presentation.pageWidthEmu).toInt().coerceAtLeast(1)
        val pageHeightPt = emuToPoints(presentation.pageHeightEmu).toInt().coerceAtLeast(1)

        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val bitmapCache = mutableMapOf<String, Bitmap>()
        val pdf = PdfDocument()
        try {
            presentation.slides.forEachIndexed { index, slide ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, index + 1).create()
                val page = pdf.startPage(pageInfo)
                drawSlide(
                    canvas = page.canvas,
                    slide = slide,
                    pageWidthPt = pageWidthPt,
                    pageHeightPt = pageHeightPt,
                    bitmapCache = bitmapCache
                )
                pdf.finishPage(page)
            }

            if (presentation.slides.isEmpty()) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, 1).create()
                val page = pdf.startPage(pageInfo)
                page.canvas.drawColor(0xFFFFFFFF.toInt())
                pdf.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                pdf.writeTo(out)
            }
        } finally {
            pdf.close()
            bitmapCache.values.forEach { it.recycle() }
        }

        if (!outputFile.exists() || outputFile.length() == 0L) {
            throw IllegalStateException("PDF conversion produced an empty file")
        }
    }

    private fun emuToPoints(emu: Float): Float =
        (emu / EMU_PER_INCH * POINTS_PER_INCH).coerceAtLeast(1f)

    private fun drawSlide(
        canvas: Canvas,
        slide: SlideItem,
        pageWidthPt: Int,
        pageHeightPt: Int,
        bitmapCache: MutableMap<String, Bitmap>
    ) {
        canvas.drawColor(toArgb(slide.backgroundColor))

        slide.backgroundImageBase64?.takeIf { it.isNotBlank() }?.let { base64 ->
            decodeBitmap(base64, bitmapCache)?.let { bitmap ->
                drawBitmapCover(canvas, bitmap, pageWidthPt.toFloat(), pageHeightPt.toFloat())
            }
        }

        for (element in slide.elements) {
            when (element) {
                is SlideGraphicElement.ShapeBlock -> drawShape(canvas, element, pageWidthPt, pageHeightPt)
                is SlideGraphicElement.ImageBlock -> drawImage(canvas, element, pageWidthPt, pageHeightPt, bitmapCache)
                is SlideGraphicElement.TextBlock -> drawText(canvas, element, pageWidthPt, pageHeightPt)
            }
        }
    }

    private fun drawShape(
        canvas: Canvas,
        element: SlideGraphicElement.ShapeBlock,
        pageWidthPt: Int,
        pageHeightPt: Int
    ) {
        val rect = elementRect(element.x, element.y, element.width, element.height, pageWidthPt, pageHeightPt)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toArgb(element.color)
            style = Paint.Style.FILL
        }
        canvas.drawRect(rect, paint)
    }

    private fun drawImage(
        canvas: Canvas,
        element: SlideGraphicElement.ImageBlock,
        pageWidthPt: Int,
        pageHeightPt: Int,
        bitmapCache: MutableMap<String, Bitmap>
    ) {
        val bitmap = decodeBitmap(element.base64, bitmapCache) ?: return
        val rect = elementRect(element.x, element.y, element.width, element.height, pageWidthPt, pageHeightPt)
        canvas.drawBitmap(bitmap, null, rect, null)
    }

    private fun drawText(
        canvas: Canvas,
        element: SlideGraphicElement.TextBlock,
        pageWidthPt: Int,
        pageHeightPt: Int
    ) {
        if (element.text.isEmpty()) return
        val rect = elementRect(element.x, element.y, element.width, element.height, pageWidthPt, pageHeightPt)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toArgb(element.textColor)
            textSize = element.fontSize.coerceAtLeast(8f)
            typeface = textTypeface(element.isBold, element.isItalic)
        }
        val layout = StaticLayout.Builder
            .obtain(element.text, 0, element.text.length, paint, rect.width().toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(rect.left, rect.top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun elementRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        pageWidthPt: Int,
        pageHeightPt: Int
    ): RectF {
        val left = x * pageWidthPt
        val top = y * pageHeightPt
        return RectF(left, top, left + width * pageWidthPt, top + height * pageHeightPt)
    }

    private fun drawBitmapCover(canvas: Canvas, bitmap: Bitmap, pageWidth: Float, pageHeight: Float) {
        val scale = max(pageWidth / bitmap.width, pageHeight / bitmap.height)
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val left = (pageWidth - scaledWidth) / 2f
        val top = (pageHeight - scaledHeight) / 2f
        val dest = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(bitmap, null, dest, null)
    }

    private fun decodeBitmap(base64: String, cache: MutableMap<String, Bitmap>): Bitmap? {
        cache[base64]?.let { return it }
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.also { cache[base64] = it }
        } catch (_: Exception) {
            null
        }
    }

    private fun toArgb(color: Long): Int = (color and 0xFFFFFFFF).toInt()

    private fun textTypeface(bold: Boolean, italic: Boolean): Typeface {
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        return Typeface.create(Typeface.SANS_SERIF, style)
    }
}
