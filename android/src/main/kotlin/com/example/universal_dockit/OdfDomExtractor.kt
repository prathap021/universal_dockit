package com.example.universal_dockit

import com.example.universal_dockit.HtmlTemplates.esc
import org.odftoolkit.odfdom.doc.OdfDocument
import org.w3c.dom.Node

/**
 * OdfDomExtractor — shared DOM traversal for ODF documents.
 *
 * Used by [OdtDocumentRenderer], [OdsDocumentRenderer], and [OdpDocumentRenderer].
 * Opens any [OdfDocument] subclass, walks the content DOM, and emits styled HTML.
 *
 * ODF content.xml element → HTML mapping:
 *  text:p / text:h         → <p> or <h1..h4> (outline-level attribute)
 *  table:table             → <table>
 *  table:table-row         → <tr>
 *  table:table-cell        → <td> / <th>
 *  draw:page (ODP)         → slide-divider banner
 *  text:span               → <span>
 *  text:line-break         → space
 *  TEXT_NODE               → escaped text content
 */
internal object OdfDomExtractor {

    /**
     * @param doc     An already-opened OdfDocument (caller must close it).
     * @param docKind "odt", "ods", or "odp" — controls slide-divider emission.
     * @return Styled HTML fragment (without <html> wrapper; use [HtmlTemplates.header]).
     */
    fun extractHtml(doc: OdfDocument, docKind: String): String = buildString {
        val contentDom = doc.contentDom
        val slideIndex = intArrayOf(0)
        val inTable = booleanArrayOf(false)
        traverseNode(contentDom.documentElement, this, docKind, 0, slideIndex, inTable)
    }

    // -------------------------------------------------------------------------

    private fun traverseNode(
        node: Node,
        sb: StringBuilder,
        docKind: String,
        depth: Int,
        slideIndex: IntArray,
        inTable: BooleanArray,
    ) {
        val localName = if (node.nodeType == Node.ELEMENT_NODE) node.localName ?: "" else ""

        when {
            // ODP slide boundary
            localName == "page" && docKind == "odp" -> {
                slideIndex[0]++
                sb.append("<div class='slide-divider'>Slide ${slideIndex[0]}</div>\n")
                recurse(node, sb, docKind, depth + 1, slideIndex, inTable)
            }

            // Table container
            localName == "table" -> {
                sb.append("<div class='table-wrapper'><table>\n")
                inTable[0] = true
                recurse(node, sb, docKind, depth + 1, slideIndex, inTable)
                inTable[0] = false
                sb.append("</table></div>\n")
            }

            localName == "table-row" -> {
                sb.append("<tr>")
                recurse(node, sb, docKind, depth + 1, slideIndex, inTable)
                sb.append("</tr>\n")
            }

            localName == "table-cell" -> {
                val tag = if (depth <= 2) "th" else "td"
                sb.append("<$tag>")
                recurse(node, sb, docKind, depth + 1, slideIndex, inTable)
                sb.append("</$tag>")
            }

            // Paragraph / heading
            localName == "p" || localName == "h" -> {
                val level = (node as? org.w3c.dom.Element)
                    ?.getAttribute("text:outline-level")?.toIntOrNull() ?: 0
                val tag = when {
                    level in 1..4 -> "h$level"
                    localName == "h" -> "h2"
                    else -> "p"
                }
                sb.append("<$tag>")
                recurse(node, sb, docKind, depth + 1, slideIndex, inTable)
                sb.append("</$tag>\n")
            }

            // Inline span
            localName == "span" -> {
                sb.append("<span>")
                recurse(node, sb, docKind, depth + 1, slideIndex, inTable)
                sb.append("</span>")
            }

            // Whitespace elements
            localName == "line-break" || localName == "tab" -> sb.append(" ")

            // Text node
            node.nodeType == Node.TEXT_NODE -> {
                val txt = node.textContent ?: ""
                if (txt.isNotBlank()) sb.append(txt.esc())
            }

            // Default: just recurse into children
            else -> recurse(node, sb, docKind, depth + 1, slideIndex, inTable)
        }
    }

    private fun recurse(
        node: Node,
        sb: StringBuilder,
        docKind: String,
        depth: Int,
        slideIndex: IntArray,
        inTable: BooleanArray,
    ) {
        val children = node.childNodes
        for (i in 0 until children.length) {
            traverseNode(children.item(i), sb, docKind, depth, slideIndex, inTable)
        }
    }
}
