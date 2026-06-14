package com.example.universal_dockit.renderers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.sl.usermodel.SlideShow
import java.io.FileInputStream

/**
 * PptDocumentRenderer — renders legacy PPT files using Apache POI HSLFSlideShow.
 *
 * Library : org.apache.poi:poi:5.3.0 (Apache 2.0)
 *
 * Rendering strategy mirrors [PptxDocumentRenderer] — each slide is drawn to
 * a [Bitmap] via the [SlideShow] supertype API so both PPTX and PPT share
 * the same Canvas-based rendering pipeline.
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
            val show     = HSLFSlideShow(fis) as SlideShow<*, *>
            val pageSize  = show.pageSize
            val scale    = screenW.toFloat() / pageSize.width
            val slideH   = (pageSize.height * scale).toInt()

            val result = show.slides.map { slide ->
                val bmp = Bitmap.createBitmap(screenW, slideH, Bitmap.Config.ARGB_8888)
                Canvas(bmp).also { canvas ->
                    canvas.drawColor(Color.WHITE)
                    canvas.scale(scale, scale)
                    slide.draw(canvas)
                }
                bmp
            }
            show.close()
            result
        }
    }
}
