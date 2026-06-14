package com.example.universal_dockit

import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
import java.io.File

/**
 * RenderCallbacks — contract between DocumentViewerActivity and each DocumentRenderer.
 *
 * Renderers call these methods to update the UI without knowing about any
 * specific view type. The Activity implements this interface and switches
 * the correct view into focus.
 */
internal interface RenderCallbacks {

    /** Application / activity context (for view construction if needed). */
    val context: Context

    /** Screen metrics — used by renderers that need pixel dimensions. */
    val displayMetrics: DisplayMetrics

    /**
     * Display HTML content in the shared WebView.
     * Used by: DOC, DOCX, XLS, XLSX, CSV, ODT, ODS, ODP renderers.
     */
    suspend fun showWebContent(html: String)

    /**
     * Display plain or attributed text in the shared TextView.
     * Used by: TXT and RTF renderers.
     *
     * @param text     Plain [String] or [android.text.Spanned] (from Html.fromHtml).
     * @param monospace If true, applies a monospace typeface (TXT).
     *                  If false, uses the default proportional typeface (RTF).
     */
    suspend fun showText(text: CharSequence, monospace: Boolean = true)

    /**
     * Display a PDF file in the PdfiumAndroid PDFView.
     * Used by: PDF renderer.
     */
    suspend fun showPdf(file: File)

    /**
     * Display a list of slide bitmaps in the shared slide ScrollView.
     * Used by: PPTX and PPT renderers.
     */
    suspend fun showSlides(bitmaps: List<Bitmap>)

    /**
     * Show an error message. Can be called from any thread.
     * Implementations must switch to the main thread internally.
     */
    fun showError(message: String)
}
