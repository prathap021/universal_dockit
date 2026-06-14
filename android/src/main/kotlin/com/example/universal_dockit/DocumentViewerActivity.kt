package com.example.universal_dockit

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.example.universal_dockit.renderers.*
import kotlinx.coroutines.*
import java.io.File

/**
 * DocumentViewerActivity — hosts all native viewer views and dispatches
 * to the correct [DocumentRenderer] based on the file's docType.
 *
 * This activity owns only the view hierarchy and coroutine scope.
 * All rendering logic lives in the per-format renderer classes under
 * the [com.example.universal_dockit.renderers] package.
 *
 * ┌──────────────┬────────────────────────────────────────┐
 * │  docType     │  Renderer class                        │
 * ├──────────────┼────────────────────────────────────────┤
 * │  pdf         │  PdfDocumentRenderer                   │
 * │  docx        │  DocxDocumentRenderer                  │
 * │  doc         │  DocDocumentRenderer                   │
 * │  xlsx        │  XlsxDocumentRenderer                  │
 * │  xls         │  XlsDocumentRenderer                   │
 * │  pptx        │  PptxDocumentRenderer                  │
 * │  ppt         │  PptDocumentRenderer                   │
 * │  txt         │  TxtDocumentRenderer                   │
 * │  csv         │  CsvDocumentRenderer                   │
 * │  rtf         │  RtfDocumentRenderer                   │
 * │  odt         │  OdtDocumentRenderer                   │
 * │  ods         │  OdsDocumentRenderer                   │
 * │  odp         │  OdpDocumentRenderer                   │
 * └──────────────┴────────────────────────────────────────┘
 */
class DocumentViewerActivity : AppCompatActivity(), RenderCallbacks {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_DOC_TYPE  = "extra_doc_type"
    }

    // ── Coroutine scope (cancelled in onDestroy) ───────────────────────────
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── RenderCallbacks contract ───────────────────────────────────────────
    override val context get() = this
    override val displayMetrics get() = resources.displayMetrics

    // ── Views ──────────────────────────────────────────────────────────────
    private lateinit var toolbar:      Toolbar
    private lateinit var progressBar:  ProgressBar
    private lateinit var errorView:    TextView

    /** PdfiumAndroid PDFView — used exclusively by PdfDocumentRenderer. */
    private lateinit var pdfView: PDFView

    /** WebView — used by DOC/DOCX, XLS/XLSX, CSV, ODT/ODS/ODP renderers. */
    private lateinit var webView: WebView

    /** Scrollable TextView — used by TXT and RTF renderers. */
    private lateinit var textScrollView: ScrollView
    private lateinit var textView:       TextView

    /** Vertical ScrollView of slide bitmaps — used by PPTX/PPT renderers. */
    private lateinit var slideScroll:     ScrollView
    private lateinit var slideContainer:  LinearLayout

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContentView())

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: run {
            showError("No file path provided."); return
        }
        val docType = intent.getStringExtra(EXTRA_DOC_TYPE) ?: run {
            showError("No document type provided."); return
        }

        toolbar.title = File(filePath).name
        dispatch(filePath, docType)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // ── Dispatcher ─────────────────────────────────────────────────────────

    private fun dispatch(filePath: String, docType: String) {
        showLoading()
        val renderer: com.example.universal_dockit.renderers.DocumentRenderer = when (docType.lowercase()) {
            "pdf"  -> PdfDocumentRenderer()
            "docx" -> DocxDocumentRenderer()
            "doc"  -> DocDocumentRenderer()
            "xlsx" -> XlsxDocumentRenderer()
            "xls"  -> XlsDocumentRenderer()
            "pptx" -> PptxDocumentRenderer()
            "ppt"  -> PptDocumentRenderer()
            "txt"  -> TxtDocumentRenderer()
            "csv"  -> CsvDocumentRenderer()
            "rtf"  -> RtfDocumentRenderer()
            "odt"  -> OdtDocumentRenderer()
            "ods"  -> OdsDocumentRenderer()
            "odp"  -> OdpDocumentRenderer()
            else   -> { showError("Unsupported type: $docType"); return }
        }
        scope.launch {
            try {
                renderer.render(filePath, this@DocumentViewerActivity)
            } catch (e: Exception) {
                showError("Render error:\n${e.message}")
            }
        }
    }

    // ── RenderCallbacks implementations ────────────────────────────────────

    override suspend fun showWebContent(html: String) = withContext(Dispatchers.Main) {
        hideAll()
        webView.isVisible = true
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    override suspend fun showText(text: CharSequence, monospace: Boolean) =
        withContext(Dispatchers.Main) {
            hideAll()
            textScrollView.isVisible = true
            if (monospace) {
                textView.typeface = android.graphics.Typeface.MONOSPACE
                textView.textSize = 13f
            } else {
                textView.typeface = android.graphics.Typeface.DEFAULT
                textView.textSize = 15f
            }
            textView.text = text
        }

    override suspend fun showPdf(file: File) = withContext(Dispatchers.Main) {
        hideAll()
        pdfView.isVisible = true
        pdfView.fromFile(file)
            .defaultPage(0)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAntialiasing(true)
            .spacing(8)
            .scrollHandle(DefaultScrollHandle(this@DocumentViewerActivity))
            .nightMode(false)
            .onError { e -> showError("PDF error: ${e.message}") }
            .load()
    }

    override suspend fun showSlides(bitmaps: List<Bitmap>) = withContext(Dispatchers.Main) {
        hideAll()
        slideScroll.isVisible = true
        bitmaps.forEachIndexed { i, bmp ->
            slideContainer.addView(TextView(this@DocumentViewerActivity).apply {
                text = "Slide ${i + 1}"
                setTextColor(ACCENT)
                textSize = 13f
                setPadding(16, 16, 16, 4)
            })
            slideContainer.addView(ImageView(this@DocumentViewerActivity).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
                    .also { it.setMargins(8, 0, 8, 16) }
                setImageBitmap(bmp)
                scaleType = ImageView.ScaleType.FIT_XY
                adjustViewBounds = true
            })
        }
    }

    override fun showError(message: String) {
        runOnUiThread {
            hideAll()
            errorView.isVisible = true
            errorView.text = "⚠️ $message"
        }
    }

    // ── View helpers ───────────────────────────────────────────────────────

    private fun showLoading() {
        progressBar.isVisible  = true
        webView.isVisible      = false
        pdfView.isVisible      = false
        textScrollView.isVisible = false
        slideScroll.isVisible  = false
        errorView.isVisible    = false
    }

    private fun hideAll() {
        progressBar.isVisible  = false
        webView.isVisible      = false
        pdfView.isVisible      = false
        textScrollView.isVisible = false
        slideScroll.isVisible  = false
        errorView.isVisible    = false
    }

    // ── Programmatic view construction ─────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
            setBackgroundColor(BG)
        }

        // Toolbar
        toolbar = Toolbar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                MATCH,
                resources.getDimensionPixelSize(
                    androidx.appcompat.R.dimen.abc_action_bar_default_height_material,
                ),
            )
            setBackgroundColor(SURFACE)
            setTitleTextColor(Color.WHITE)
            navigationIcon = getDrawable(android.R.drawable.ic_menu_close_clear_cancel)
            setNavigationOnClickListener { finish() }
        }
        setSupportActionBar(toolbar)
        root.addView(toolbar)

        // Loading spinner
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            indeterminateTintList =
                android.content.res.ColorStateList.valueOf(ACCENT)
            gravity = Gravity.CENTER
        }
        root.addView(progressBar)

        // Error label
        errorView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            isVisible = false
        }
        root.addView(errorView)

        // PdfiumAndroid PDFView
        pdfView = PDFView(this, null).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            isVisible = false
        }
        root.addView(pdfView)

        // WebView (DOC/DOCX, XLS/XLSX, CSV, ODF)
        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            settings.javaScriptEnabled = false
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            setBackgroundColor(BG)
            isVisible = false
        }
        root.addView(webView)

        // TextView in ScrollView (TXT / RTF)
        textView = TextView(this).apply {
            setTextColor(Color.parseColor("#E0E0E0"))
            setLineSpacing(4f, 1.2f)
            setPadding(20, 20, 20, 20)
            setTextIsSelectable(true)
        }
        textScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            setBackgroundColor(BG)
            isVisible = false
        }
        textScrollView.addView(textView)
        root.addView(textScrollView)

        // Slide ScrollView (PPTX / PPT)
        slideContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
        }
        slideScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            isVisible = false
        }
        slideScroll.addView(slideContainer)
        root.addView(slideScroll)

        return root
    }

    // ── Constants ──────────────────────────────────────────────────────────
    private val MATCH   = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP    = ViewGroup.LayoutParams.WRAP_CONTENT
    private val BG      = Color.parseColor("#0F3460")
    private val SURFACE = Color.parseColor("#16213E")
    private val ACCENT  = Color.parseColor("#E94560")
}
