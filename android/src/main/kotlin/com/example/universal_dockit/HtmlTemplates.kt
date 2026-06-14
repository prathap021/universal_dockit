package com.example.universal_dockit

/**
 * HtmlTemplates — shared HTML page template used by all WebView-based renderers.
 */
internal object HtmlTemplates {

    fun header(title: String, accentColor: String = "#E94560"): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="UTF-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <title>${'$'}{title.esc()}</title>
        <style>
          @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
          
          :root {
            --bg:      #FFFFFF;
            --surface: #F8F9FA;
            --accent:  $accentColor;
            --accent-light: ${accentColor}1A; /* 10% opacity */
            --text:    #1A1A1A;
            --muted:   #6C757D;
            --border:  #EAEAEA;
            --shadow-sm: 0 2px 8px rgba(0,0,0,0.04);
            --shadow-md: 0 4px 12px rgba(0,0,0,0.06);
          }
          
          * { box-sizing: border-box; margin: 0; padding: 0; }
          
          body {
            background: var(--bg);
            color: var(--text);
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            font-size: 15px;
            line-height: 1.7;
            padding: 24px 16px;
            max-width: 800px;
            margin: 0 auto;
          }
          
          /* Typography */
          h1, h2, h3, h4 {
            color: var(--accent);
            margin: 24px 0 12px;
            line-height: 1.3;
            font-weight: 700;
            letter-spacing: -0.02em;
          }
          h1 { font-size: 1.8em; }
          h2 { font-size: 1.4em; }
          h3 { font-size: 1.2em; }
          h4 { font-size: 1.1em; }
          p  { margin: 8px 0 16px; color: var(--text); }
          
          /* Tables (Structured Excel) */
          .table-wrapper { 
            overflow-x: auto; 
            margin: 20px 0; 
            border-radius: 8px;
            box-shadow: var(--shadow-sm);
            background: var(--surface);
          }
          table {
            width: 100%;
            border-collapse: collapse;
            background: white;
            border: 1px solid var(--border);
          }
          th {
            background: var(--accent);
            color: white;
            font-weight: 600;
            padding: 12px 14px;
            text-align: left;
            font-size: 13px;
            white-space: nowrap;
            letter-spacing: 0.02em;
            border: 1px solid rgba(255,255,255,0.2);
          }
          td {
            padding: 10px 14px;
            border: 1px solid var(--border);
            font-size: 13px;
            color: var(--text);
            background: white;
          }
          tr:hover td { background: var(--surface); }
          
          /* Presentation / Spreadsheets */
          .sheet-title {
            margin-top: 32px;
            padding: 12px 16px;
            background: var(--surface);
            border-left: 5px solid var(--accent);
            border-radius: 8px;
            box-shadow: var(--shadow-sm);
            display: inline-block;
          }
          .slide-divider {
            text-align: center;
            padding: 14px;
            margin: 32px 0 16px;
            background: var(--surface);
            color: var(--accent);
            font-weight: 700;
            font-size: 1.1em;
            border-radius: 12px;
            box-shadow: var(--shadow-sm);
            border: 1px solid var(--border);
            text-transform: uppercase;
            letter-spacing: 0.05em;
          }
          
          .slide-content {
            background: var(--surface);
            padding: 24px;
            border-radius: 12px;
            box-shadow: var(--shadow-md);
            margin-bottom: 24px;
            border: 1px solid var(--border);
          }
          
          /* Inline Elements */
          .error { 
            color: #FF4757; 
            font-style: italic; 
            background: rgba(255, 71, 87, 0.1); 
            padding: 12px; 
            border-radius: 8px; 
            display: inline-block;
          }
          strong, b { font-weight: 600; color: var(--text); }
          em, i     { color: var(--muted); }
          del       { text-decoration: line-through; color: var(--muted); }
          u         { text-decoration: underline; text-underline-offset: 4px; }
        </style>
        </head>
        <body>
    """.trimIndent()

    fun footer(): String = "\n</body></html>"

    fun String.esc(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
