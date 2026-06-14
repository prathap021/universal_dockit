package com.example.universal_dockit

/**
 * HtmlTemplates — shared HTML page template used by all WebView-based renderers.
 *
 * Provides a consistent dark-themed stylesheet across DOC/DOCX, XLS/XLSX,
 * CSV, and ODF renderers. All renderers inject their content between
 * [header] and [footer].
 */
internal object HtmlTemplates {

    /**
     * Returns a complete HTML page opening — DOCTYPE, head, meta, and styled body open tag.
     * @param title Document title shown in the page (also used for accessibility).
     */
    fun header(title: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="UTF-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <title>${'$'}{title.esc()}</title>
        <style>
          :root {
            --bg:      #0F3460;
            --surface: #16213E;
            --accent:  #E94560;
            --text:    #E0E0E0;
            --muted:   #9E9E9E;
            --border:  #1A1A2E;
          }
          * { box-sizing: border-box; margin: 0; padding: 0; }
          body {
            background: var(--bg);
            color: var(--text);
            font-family: 'Segoe UI', Roboto, Arial, sans-serif;
            font-size: 15px;
            line-height: 1.7;
            padding: 16px;
          }
          h1, h2, h3, h4 {
            color: var(--accent);
            margin: 16px 0 8px;
            line-height: 1.3;
          }
          h1 { font-size: 1.6em; }
          h2 { font-size: 1.3em; }
          h3 { font-size: 1.1em; }
          h4 { font-size: 1.0em; }
          p  { margin: 6px 0; }
          .table-wrapper { overflow-x: auto; margin: 12px 0; }
          table {
            width: 100%;
            border-collapse: collapse;
            background: var(--surface);
            border-radius: 8px;
            overflow: hidden;
          }
          th {
            background: var(--accent);
            color: white;
            font-weight: 600;
            padding: 10px 12px;
            text-align: left;
            font-size: 13px;
            white-space: nowrap;
          }
          td {
            padding: 8px 12px;
            border-bottom: 1px solid var(--border);
            font-size: 13px;
          }
          tr:last-child td { border-bottom: none; }
          tr:nth-child(even) td { background: rgba(255,255,255,.03); }
          .sheet-title {
            margin-top: 24px;
            padding: 8px 12px;
            background: var(--surface);
            border-left: 4px solid var(--accent);
            border-radius: 4px;
          }
          .slide-divider {
            text-align: center;
            padding: 12px;
            margin: 20px 0 8px;
            background: var(--surface);
            color: var(--accent);
            font-weight: bold;
            border-radius: 8px;
          }
          .error { color: #FF5252; font-style: italic; }
          strong, b { color: #FAFAFA; }
          em, i     { color: var(--muted); }
          del       { text-decoration: line-through; color: var(--muted); }
          u         { text-decoration: underline; }
        </style>
        </head>
        <body>
    """.trimIndent()

    /** Closes the HTML page opened by [header]. */
    fun footer(): String = "\n</body></html>"

    /**
     * Escapes [String] for safe embedding in HTML content.
     * Handles the five standard XML/HTML special characters.
     */
    fun String.esc(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
