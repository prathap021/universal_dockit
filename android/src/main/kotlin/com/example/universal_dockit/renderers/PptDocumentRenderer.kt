package com.example.universal_dockit.renderers

import android.graphics.Bitmap
import com.example.universal_dockit.RenderCallbacks
import com.example.universal_dockit.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.sl.usermodel.SlideShow
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.FileInputStream

/**
 * PptDocumentRenderer — renders legacy PPT files using Apache POI HSLFSlideShow.
 *
 * Library : org.apache.poi:poi-scratchpad:5.3.0 (Apache 2.0)
 *
 * Rendering strategy mirrors [PptxDocumentRenderer] — each slide is drawn to
 * an AWT [BufferedImage] via the [SlideShow] supertype API so both PPTX and
 * PPT share the same rendering pipeline.
 */
internal class PptDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val bitmaps = withContext(Dispatchers.IO) { renderSlides(filePath, callbacks) }
        callbacks.showSlides(bitmaps)
    }

    @Suppress("UNCHECKED_CAST")
    private fun renderSlides(filePath: String, callbacks: RenderCallbacks): List<Bitmap> {
        val screenW = callbacks.displayMetrics.widthPixels
        return FileInputStream(filePath).use { fis ->
            val show = HSLFSlideShow(fis) as SlideShow<*, *>
            val pageSize = show.pageSize
            val scale = screenW.toFloat() / pageSize.width
            val slideH = (pageSize.height * scale).toInt()

            val result = show.slides.map { slide ->
                val image = BufferedImage(screenW, slideH, BufferedImage.TYPE_INT_ARGB)
                val graphics = image.createGraphics()
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, screenW, slideH)
                graphics.scale(scale.toDouble(), scale.toDouble())
                slide.draw(graphics)
                graphics.dispose()
                image.toBitmap()
            }
            show.close()
            result
        }
    }
}
