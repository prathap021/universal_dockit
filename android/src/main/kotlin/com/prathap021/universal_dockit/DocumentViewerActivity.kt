package com.prathap021.universal_dockit

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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.prathap021.universal_dockit.renderers.CsvDocumentRenderer
import com.prathap021.universal_dockit.renderers.DocumentRenderer
import com.prathap021.universal_dockit.renderers.OdpDocumentRenderer
import com.prathap021.universal_dockit.renderers.OdsDocumentRenderer
import com.prathap021.universal_dockit.renderers.OdtDocumentRenderer
import com.prathap021.universal_dockit.renderers.PdfDocumentRenderer
import com.prathap021.universal_dockit.renderers.RtfDocumentRenderer
import com.prathap021.universal_dockit.renderers.TxtDocumentRenderer
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import kotlinx.coroutines.Dispatchers
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
 * │ doc, docx  │ WordDocumentRenderer (POI → HTML)               │
 * │ xls, xlsx  │ ExcelDocumentRenderer (POI → HTML)              │
 * │ ppt, pptx  │ PowerPointDocumentRenderer (POI text → HTML)    │
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

    private val viewModel: DocumentViewerViewModel by viewModels()

    override val context get() = this
    override val displayMetrics get() = resources.displayMetrics

    private var darkMode: Boolean = false
    private var enableSearchFeature: Boolean = false
    private var enableZoomFeature: Boolean = false
    private var enableDarkModeToggleFeature: Boolean = false
    private var currentFilePath: String = ""
    private var currentDocType: String = ""

    private lateinit var loadingContainer: View
    private lateinit var errorView: TextView
    private var pdfView: PDFView? = null
    private lateinit var webView: WebView
    private lateinit var textScrollView: ScrollView
    private lateinit var textView: TextView

    private lateinit var searchInput: android.widget.EditText
    private lateinit var btnSearchToggle: android.widget.ImageButton
    private lateinit var btnZoomIn: android.widget.ImageButton
    private lateinit var btnZoomOut: android.widget.ImageButton
    private lateinit var btnDarkMode: android.widget.ImageButton
    private var originalText: CharSequence? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val features = intent.getSerializableExtra("features") as? HashMap<String, Any>
        darkMode = features?.get("darkMode") as? Boolean ?: false
        enableSearchFeature = features?.get("search") as? Boolean ?: false
        enableZoomFeature = features?.get("zoomInOut") as? Boolean ?: false
        enableDarkModeToggleFeature = features?.get("darkModeToggle") as? Boolean ?: false

        setContentView(buildContentView())

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: run {
            showError("No file path provided."); return
        }
        val docType = intent.getStringExtra(EXTRA_DOC_TYPE) ?: run {
            showError("No document type provided."); return
        }
        currentFilePath = filePath
        currentDocType = docType
        if (!File(filePath).exists()) {
            showError("File not found:\n$filePath"); return
        }

        dispatch(filePath, docType)
    }

    // ── Dispatcher ─────────────────────────────────────────────────────────

    private fun dispatch(filePath: String, docType: String) {
        showLoading()
        viewModel.renderDocument(filePath, docType, this)
    }

    // ── RenderCallbacks ────────────────────────────────────────────────────

    override suspend fun showWebContent(html: String, baseUrl: String?) = withContext(Dispatchers.Main) {
        // Loader stays visible until WebViewClient.onPageFinished() fires.
        hideContent()
        webView.isVisible = true
        
        var finalHtml = html
        if (darkMode) {
            val darkCss = "<style>body { background-color: #121212 !important; color: #E0E0E0 !important; } table, th, td { border-color: #555 !important; }</style>"
            finalHtml = if (finalHtml.contains("</head>")) {
                finalHtml.replace("</head>", "$darkCss</head>")
            } else {
                darkCss + finalHtml
            }
        }
        
        if (baseUrl != null) {
            webView.settings.allowFileAccess = true
            webView.settings.allowContentAccess = true
            webView.loadDataWithBaseURL(baseUrl, finalHtml, "text/html", "UTF-8", null)
        } else {
            webView.loadDataWithBaseURL(null, finalHtml, "text/html", "UTF-8", null)
        }
        setToolbarButtonsVisible(true)
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
            textView.setTextColor(if (darkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#111111"))
            textScrollView.setBackgroundColor(if (darkMode) Color.parseColor("#121212") else Color.WHITE)
            textView.text = text
            originalText = text
            loadingContainer.isVisible = false
            setToolbarButtonsVisible(true)
        }

    override suspend fun showPdf(file: File) = withContext(Dispatchers.Main) {
        hideContent()
        if (pdfView == null) {
            val pdf = PDFView(this@DocumentViewerActivity, null).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH, MATCH)
            }
            val contentLayout = webView.parent as ViewGroup
            contentLayout.addView(pdf, 1)
            pdfView = pdf
        }
        val pv = pdfView!!
        pv.isVisible = true
        pv.fromFile(file)
            .defaultPage(0)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAntialiasing(true)
            .spacing(8)
            .scrollHandle(DefaultScrollHandle(this@DocumentViewerActivity))
            .nightMode(darkMode)
            .onLoad { _ -> loadingContainer.isVisible = false }
            .onError { e ->
                loadingContainer.isVisible = false
                showError("PDF error: ${e.message}")
            }
            .load()
        setToolbarButtonsVisible(true)
    }

    override fun showError(message: String) {
        runOnUiThread {
            hideContent()
            loadingContainer.isVisible = false
            errorView.isVisible = true
            errorView.text = "⚠️  $message"
        }
    }

    // ── View helpers ───────────────────────────────────────────────────────

    private fun showLoading() {
        loadingContainer.isVisible = true
        hideContent()
    }

    private fun hideContent() {
        webView.isVisible = false
        pdfView?.isVisible = false
        textScrollView.isVisible = false
        errorView.isVisible = false
        setToolbarButtonsVisible(false)
    }

    private fun setToolbarButtonsVisible(visible: Boolean) {
        btnSearchToggle.isVisible = visible && enableSearchFeature
        btnZoomIn.isVisible = visible && enableZoomFeature
        btnZoomOut.isVisible = visible && enableZoomFeature
        btnDarkMode.isVisible = enableDarkModeToggleFeature
    }

    private fun highlightTextView(query: String) {
        if (originalText == null) return
        if (query.isEmpty()) {
            textView.text = originalText
            return
        }
        val spannable = android.text.SpannableString(originalText)
        val lowerText = originalText.toString().lowercase()
        val lowerQuery = query.lowercase()
        var index = lowerText.indexOf(lowerQuery)
        while (index >= 0) {
            spannable.setSpan(
                android.text.style.BackgroundColorSpan(Color.YELLOW),
                index, index + query.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            index = lowerText.indexOf(lowerQuery, index + query.length)
        }
        textView.text = spannable
    }

    // ── View construction ──────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildContentView(): View {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
            setBackgroundColor(if (darkMode) Color.BLACK else BG)
        }

        // --- Custom App Bar ---
        val appBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH, 136)
            setBackgroundColor(if (darkMode) Color.parseColor("#1E1E1E") else Color.parseColor("#F8F9FA"))
            elevation = 6f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 0, 24, 0)
        }

        val titleView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
            text = intent.getStringExtra("filePath")?.substringAfterLast("/") ?: "Document"
            textSize = 18f
            setTextColor(if (darkMode) Color.WHITE else Color.BLACK)
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        searchInput = android.widget.EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
            hint = "Search document..."
            setTextColor(if (darkMode) Color.WHITE else Color.BLACK)
            setHintTextColor(Color.GRAY)
            isSingleLine = true
            isVisible = false

            addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    val query = s?.toString() ?: ""
                    if (webView.isVisible) {
                        if (query.isNotEmpty()) webView.findAllAsync(query) else webView.clearMatches()
                    } else if (textScrollView.isVisible) {
                        highlightTextView(query)
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        btnZoomOut = android.widget.ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = 12 }
            setImageResource(android.R.drawable.ic_media_previous)
            setBackgroundColor(Color.TRANSPARENT)
            imageTintList = android.content.res.ColorStateList.valueOf(if (darkMode) Color.WHITE else Color.BLACK)
            isVisible = false
            contentDescription = "Zoom out"
            setOnClickListener {
                when {
                    webView.isVisible -> webView.zoomOut()
                    textScrollView.isVisible -> textView.textSize = (textView.textSize / resources.displayMetrics.scaledDensity - 1f).coerceAtLeast(11f)
                    else -> Unit
                }
            }
        }

        btnZoomIn = android.widget.ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = 12 }
            setImageResource(android.R.drawable.ic_media_next)
            setBackgroundColor(Color.TRANSPARENT)
            imageTintList = android.content.res.ColorStateList.valueOf(if (darkMode) Color.WHITE else Color.BLACK)
            isVisible = false
            contentDescription = "Zoom in"
            setOnClickListener {
                when {
                    webView.isVisible -> webView.zoomIn()
                    textScrollView.isVisible -> textView.textSize = (textView.textSize / resources.displayMetrics.scaledDensity + 1f).coerceAtMost(30f)
                    else -> Unit
                }
            }
        }

        btnDarkMode = android.widget.ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = 12 }
            setImageResource(if (darkMode) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
            setBackgroundColor(Color.TRANSPARENT)
            imageTintList = android.content.res.ColorStateList.valueOf(if (darkMode) Color.WHITE else Color.BLACK)
            isVisible = enableDarkModeToggleFeature
            contentDescription = "Toggle dark mode"
            setOnClickListener {
                darkMode = !darkMode
                recreate()
            }
        }

        btnSearchToggle = android.widget.ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
            setImageResource(android.R.drawable.ic_menu_search)
            setBackgroundColor(Color.TRANSPARENT)
            imageTintList = android.content.res.ColorStateList.valueOf(if (darkMode) Color.WHITE else Color.BLACK)
            isVisible = false

            setOnClickListener {
                if (searchInput.isVisible) {
                    searchInput.isVisible = false
                    titleView.isVisible = true
                    searchInput.setText("")
                    setImageResource(android.R.drawable.ic_menu_search)
                    if (webView.isVisible) webView.clearMatches()
                    if (textScrollView.isVisible && originalText != null) textView.text = originalText
                } else {
                    if (!enableSearchFeature) return@setOnClickListener
                    searchInput.isVisible = true
                    titleView.isVisible = false
                    setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    searchInput.requestFocus()
                }
            }
        }

        appBar.addView(titleView)
        appBar.addView(searchInput)
        appBar.addView(btnZoomOut)
        appBar.addView(btnZoomIn)
        appBar.addView(btnDarkMode)
        appBar.addView(btnSearchToggle)
        mainLayout.addView(appBar)

        val content = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f)
            setBackgroundColor(if (darkMode) Color.BLACK else Color.WHITE)
        }
        mainLayout.addView(content)

        errorView = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER)
            setTextColor(Color.RED)
            isVisible = false
            setPadding(32, 32, 32, 32)
        }
        content.addView(errorView)

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
            settings.javaScriptEnabled = false
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            setBackgroundColor(if (darkMode) Color.parseColor("#121212") else Color.WHITE)
            isVisible = false

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    loadingContainer.isVisible = false
                }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    loadingContainer.isVisible = false
                }
            }
        }
        content.addView(webView)

        textScrollView = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
            setBackgroundColor(if (darkMode) Color.parseColor("#121212") else Color.WHITE)
            isVisible = false
            textView = TextView(this@DocumentViewerActivity).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH, WRAP)
                setPadding(32, 32, 32, 32)
                textSize = 14f
                setTextColor(if (darkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#111111"))
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    breakStrategy = android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY
                }
            }
            addView(textView)
        }
        content.addView(textScrollView)

        loadingContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER)
            gravity = Gravity.CENTER
            isVisible = false
        }

        val spinner = ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
            indeterminateTintList = android.content.res.ColorStateList.valueOf(ACCENT)
        }
        (loadingContainer as LinearLayout).addView(spinner)

        val loadingText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply {
                topMargin = 16
            }
            text = "Loading document..."
            textSize = 14f
            setTextColor(if (darkMode) Color.parseColor("#B0B0B0") else Color.parseColor("#666666"))
        }
        (loadingContainer as LinearLayout).addView(loadingText)

        content.addView(loadingContainer)

        return mainLayout
    }
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val BG = Color.WHITE
    private val ACCENT = Color.parseColor("#E94560")
}
