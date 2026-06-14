package com.example.universal_dockit.renderers

import android.graphics.Bitmap
import com.example.universal_dockit.RenderCallbacks
import com.example.universal_dockit.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.FileInputStream

/**
 * PptxDocumentRenderer — renders PPTX files using Apache POI XMLSlideShow.
 *
 * Library : org.apache.poi:poi-ooxml:5.3.0 (Apache 2.0)
 *
 * Rendering strategy:
 *  - Opens the slide show with [XMLSlideShow]
 *  - Scales each slide to fill the screen width, preserving aspect ratio
 *  - Draws each slide to an AWT [BufferedImage], then converts to [Bitmap]
 *  - Delivers the list of bitmaps to [RenderCallbacks.showSlides]
 *    which places them in a vertical ScrollView with slide-number labels
 */
internal class PptxDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val bitmaps = withContext(Dispatchers.IO) { renderSlides(filePath, callbacks) }
        callbacks.showSlides(bitmaps)
    }

    private fun renderSlides(filePath: String, callbacks: RenderCallbacks): List<Bitmap> {
        val screenW = callbacks.displayMetrics.widthPixels
        return FileInputStream(filePath).use { fis ->
            val show = XMLSlideShow(fis)
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
