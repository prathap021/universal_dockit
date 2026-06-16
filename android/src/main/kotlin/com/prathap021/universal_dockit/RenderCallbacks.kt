package com.prathap021.universal_dockit

import android.content.Context
import android.util.DisplayMetrics
import java.io.File

/**
 * RenderCallbacks — contract between DocumentViewerActivity and each DocumentRenderer.
 *
 * Renderers call these methods to deliver results to the UI without knowing
 * about any specific view type. The Activity implements this interface.
 */
internal interface RenderCallbacks {

    /** Activity context (for view construction if a renderer needs it). */
    val context: Context

    /** Screen metrics — exposed for any renderer needing pixel dimensions. */
    val displayMetrics: DisplayMetrics

    /**
     * Display HTML content in the shared WebView.
     * Used by: DOC, DOCX, XLS, XLSX, CSV, ODT, ODS, ODP renderers.
     */
    suspend fun showWebContent(html: String, baseUrl: String? = null)

    /**
     * Display plain or attributed text in the shared TextView.
     * Used by: TXT and RTF renderers.
     *
     * @param text      Plain [String] or [android.text.Spanned] (from Html.fromHtml).
     * @param monospace If true, applies a monospace typeface (TXT).
     *                  If false, uses the default proportional typeface (RTF).
     */
    suspend fun showText(text: CharSequence, monospace: Boolean = true)

    /** Display a PDF file in the PdfiumAndroid PDFView. Used by: PDF and PPT/PPTX renderers. */
    suspend fun showPdf(file: File)

    /** Show an error message. Can be called from any thread. */
    fun showError(message: String)
}
