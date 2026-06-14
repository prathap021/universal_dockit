package com.prathap021.universal_dockit

import com.prathap021.universal_dockit.HtmlTemplates.esc
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.util.zip.ZipFile

/**
 * OdfContentParser — pure-Android parser for OpenDocument files (.odt/.ods/.odp).
 *
 * Opens the file as a ZIP, reads `content.xml`, and walks it with Android's
 * built-in `XmlPullParser`. Emits styled HTML suitable for display in a WebView.
 *
 * Replaces the Apache ODF Toolkit dependency, which depends on
 * `org.w3c.dom.events.*` / `.traversal.*` interfaces that Android's
 * bootclasspath rejects.
 *
 * Recognised ODF elements (matched by local name, namespace-agnostic):
 *   text:h            → <h1..h4>   (uses text:outline-level)
 *   text:p            → <p>
 *   text:span         → <span>
 *   text:line-break   → <br/>
 *   text:tab          → &emsp;
 *   text:s            → space(s)   (uses text:c count)
 *   text:list         → <ul>
 *   text:list-item    → <li>
 *   table:table       → <table> inside <div class="table-wrapper">
 *   table:table-row   → <tr>
 *   table:table-cell  → <td> (or <th> for the first row)
 *   draw:page         → "Slide N" divider (ODP only)
 *   any other         → walked transparently (text children are still emitted)
 */
internal object OdfContentParser {

    /**
     * @param filePath absolute path to the .odt/.ods/.odp file
     * @param docKind  "odt", "ods", or "odp" — controls slide-divider emission
     * @return Body HTML fragment (combine with [HtmlTemplates.header] / [footer])
     */
    fun parse(filePath: String, docKind: String): String {
        val file = File(filePath)
        require(file.exists()) { "ODF file not found: $filePath" }

        return ZipFile(file).use { zip ->
            val entry = zip.getEntry("content.xml")
                ?: error("ODF archive does not contain content.xml")

            zip.getInputStream(entry).use { input ->
                val parser = XmlPullParserFactory.newInstance().apply {
                    isNamespaceAware = true
                }.newPullParser()
                parser.setInput(input, "UTF-8")
                buildString { walk(parser, this, docKind) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun walk(parser: XmlPullParser, sb: StringBuilder, docKind: String) {
        // One push per START_TAG, one pop per END_TAG. Empty string for elements
        // that don't open a matching HTML tag (e.g. self-closing or unhandled).
        val closeStack = ArrayDeque<String>()
        var slideIndex = 0
        var tableDepth = 0
        var rowIndex = 0

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val close = when (parser.name) {
                        "h" -> {
                            val level = parser.localAttr("outline-level")
                                ?.toIntOrNull()?.coerceIn(1, 4) ?: 2
                            sb.append("<h$level>")
                            "</h$level>"
                        }
                        "p" -> { sb.append("<p>"); "</p>" }
                        "span" -> { sb.append("<span>"); "</span>" }
                        "line-break" -> { sb.append("<br/>"); "" }
                        "tab" -> { sb.append("&emsp;"); "" }
                        "s" -> {
                            val count = parser.localAttr("c")?.toIntOrNull() ?: 1
                            repeat(count) { sb.append(' ') }
                            ""
                        }
                        "list" -> { sb.append("<ul>"); "</ul>" }
                        "list-item" -> { sb.append("<li>"); "</li>" }
                        "table" -> {
                            sb.append("<div class='table-wrapper'><table>")
                            tableDepth++
                            rowIndex = 0
                            "</table></div>"
                        }
                        "table-row" -> {
                            sb.append("<tr>")
                            rowIndex++
                            "</tr>"
                        }
                        "table-cell" -> {
                            val tag = if (rowIndex == 1) "th" else "td"
                            sb.append("<$tag>")
                            "</$tag>"
                        }
                        "page" -> if (docKind == "odp") {
                            slideIndex++
                            sb.append("<div class='slide-divider'>Slide $slideIndex</div>")
                            ""
                        } else ""
                        else -> ""
                    }
                    closeStack.addLast(close)
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text
                    if (!text.isNullOrEmpty()) sb.append(text.esc())
                }

                XmlPullParser.END_TAG -> {
                    if (closeStack.isNotEmpty()) sb.append(closeStack.removeLast())
                    if (parser.name == "table") {
                        tableDepth = (tableDepth - 1).coerceAtLeast(0)
                        if (tableDepth == 0) rowIndex = 0
                    }
                }
            }
            event = parser.next()
        }

        // Defensive: close anything still open if the document was malformed.
        while (closeStack.isNotEmpty()) sb.append(closeStack.removeLast())
    }

    /** Attribute lookup by local name only — namespace-agnostic. */
    private fun XmlPullParser.localAttr(name: String): String? {
        for (i in 0 until attributeCount) {
            if (getAttributeName(i) == name) return getAttributeValue(i)
        }
        return null
    }
}
