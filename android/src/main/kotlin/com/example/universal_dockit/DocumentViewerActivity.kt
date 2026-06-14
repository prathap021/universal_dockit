package com.example.universal_dockit

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.universal_dockit.renderers.CsvDocumentRenderer
import com.example.universal_dockit.renderers.DocDocumentRenderer
import com.example.universal_dockit.renderers.DocumentRenderer
import com.example.universal_dockit.renderers.DocxDocumentRenderer
import com.example.universal_dockit.renderers.OdpDocumentRenderer
import com.example.universal_dockit.renderers.OdsDocumentRenderer
import com.example.universal_dockit.renderers.OdtDocumentRenderer
import com.example.universal_dockit.renderers.PdfDocumentRenderer
import com.example.universal_dockit.renderers.PptDocumentRenderer
import com.example.universal_dockit.renderers.PptxDocumentRenderer
import com.example.universal_dockit.renderers.RtfDocumentRenderer
import com.example.universal_dockit.renderers.TxtDocumentRenderer
import com.example.universal_dockit.renderers.XlsDocumentRenderer
import com.example.universal_dockit.renderers.XlsxDocumentRenderer
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * DocumentViewerActivity — single host Activity that swaps between three viewers
 * (PDFView / WebView / TextView) depending on the document type.
 *
 * ┌────────────┬─────────────────────────────────────────┐
 * │ docType    │ Renderer                                │
 * ├────────────┼─────────────────────────────────────────┤
 * │ pdf        │ PdfDocumentRenderer (PdfiumAndroid)     │
 * │ doc, docx  │ DocDocumentRenderer / DocxDocumentRenderer (POI → HTML) │
 * │ xls, xlsx  │ XlsDocumentRenderer / XlsxDocumentRenderer (POI → HTML) │
 * │ ppt, pptx  │ PptDocumentRenderer / PptxDocumentRenderer (POI text → HTML) │
 * │ txt        │ TxtDocumentRenderer (TextView, monospace)│
 * │ csv        │ CsvDocumentRenderer (RFC4180 → HTML)    │
 * │ rtf        │ RtfDocumentRenderer (Html.fromHtml)     │
 * │ odt/ods/odp│ Odt/Ods/OdpDocumentRenderer (ZIP+XML → HTML) │
 * └────────────┴─────────────────────────────────────────┘
 */
class DocumentViewerActivity : AppCompatActivity(), RenderCallbacks {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_DOC_TYPE = "extra_doc_type"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override val context get() = this
    override val displayMetrics get() = resources.displayMetrics

    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: TextView
    private lateinit var pdfView: PDFView
    private lateinit var webView: WebView
    private lateinit var textScrollView: ScrollView
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContentView())

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: run {
            showError("No file path provided."); return
        }
        val docType = intent.getStringExtra(EXTRA_DOC_TYPE) ?: run {
            showError("No document type provided."); return
        }
        if (!File(filePath).exists()) {
            showError("File not found:\n$filePath"); return
        }

        dispatch(filePath, docType)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // ── Dispatcher ─────────────────────────────────────────────────────────

    private fun dispatch(filePath: String, docType: String) {
        val renderer: DocumentRenderer = when (docType.lowercase()) {
            "pdf" -> PdfDocumentRenderer()
            "docx" -> DocxDocumentRenderer()
            "doc" -> DocDocumentRenderer()
            "xlsx" -> XlsxDocumentRenderer()
            "xls" -> XlsDocumentRenderer()
            "pptx" -> PptxDocumentRenderer()
            "ppt" -> PptDocumentRenderer()
            "txt" -> TxtDocumentRenderer()
            "csv" -> CsvDocumentRenderer()
            "rtf" -> RtfDocumentRenderer()
            "odt" -> OdtDocumentRenderer()
            "ods" -> OdsDocumentRenderer()
            "odp" -> OdpDocumentRenderer()
            else -> {
                showError("Unsupported document type: $docType"); return
            }
        }

        showLoading()
        scope.launch {
            try {
                renderer.render(filePath, this@DocumentViewerActivity)
            } catch (e: Throwable) {
                showError("Failed to render document\n${e.javaClass.simpleName}: ${e.message ?: ""}")
            }
        }
    }

    // ── RenderCallbacks ────────────────────────────────────────────────────

    override suspend fun showWebContent(html: String) = withContext(Dispatchers.Main) {
        // Loader stays visible until WebViewClient.onPageFinished() fires.
        hideContent()
        webView.isVisible = true
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    override suspend fun showText(text: CharSequence, monospace: Boolean) =
        withContext(Dispatchers.Main) {
            hideContent()
            textScrollView.isVisible = true
            if (monospace) {
                textView.typeface = android.graphics.Typeface.MONOSPACE
                textView.textSize = 13f
            } else {
                textView.typeface = android.graphics.Typeface.DEFAULT
                textView.textSize = 15f
            }
            textView.text = text
            progressBar.isVisible = false
        }

    override suspend fun showPdf(file: File) = withContext(Dispatchers.Main) {
        hideContent()
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
            .onLoad { _ -> progressBar.isVisible = false }
            .onError { e ->
                progressBar.isVisible = false
                showError("PDF error: ${e.message}")
            }
            .load()
    }

    override fun showError(message: String) {
        runOnUiThread {
            hideContent()
            progressBar.isVisible = false
            errorView.isVisible = true
            errorView.text = "⚠️  $message"
        }
    }

    // ── View helpers ───────────────────────────────────────────────────────

    private fun showLoading() {
        progressBar.isVisible = true
        hideContent()
    }

    private fun hideContent() {
        webView.isVisible = false
        pdfView.isVisible = false
        textScrollView.isVisible = false
        errorView.isVisible = false
    }

    // ── View construction ──────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildContentView(): View {
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
            setBackgroundColor(BG)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
        }

        errorView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            setTextColor(Color.parseColor("#B71C1C"))
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            isVisible = false
        }
        content.addView(errorView)

        pdfView = PDFView(this, null).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            isVisible = false
        }
        content.addView(pdfView)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            settings.javaScriptEnabled = false
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.defaultTextEncodingName = "UTF-8"
            setBackgroundColor(Color.WHITE)
            isVisible = false

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    progressBar.isVisible = false
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    progressBar.isVisible = false
                }
            }
        }
        content.addView(webView)

        textView = TextView(this).apply {
            setTextColor(Color.parseColor("#111111"))
            setLineSpacing(4f, 1.2f)
            setPadding(20, 20, 20, 20)
            setTextIsSelectable(true)
        }
        textScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            setBackgroundColor(Color.WHITE)
            isVisible = false
        }
        textScrollView.addView(textView)
        content.addView(textScrollView)

        root.addView(content)

        // Centered loading overlay (drawn last → on top)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply {
            layoutParams = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER)
            indeterminateTintList =
                android.content.res.ColorStateList.valueOf(ACCENT)
        }
        root.addView(progressBar)

        return root
    }

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val BG = Color.WHITE
    private val ACCENT = Color.parseColor("#E94560")
}
