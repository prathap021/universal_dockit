package com.prathap021.universal_dockit.renderers

import android.os.Build
import android.text.Html
import com.prathap021.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RtfDocumentRenderer — renders RTF files using Android's built-in HTML engine.
 *
 * Rendering strategy:
 *  1. Reads the raw RTF bytes using the declared \\ansicpg code page
 *  2. Maps common RTF formatting control words to HTML equivalents
 *     (\b → <b>, \i → <i>, \ul → <u>, \strike → <del>, \par → <br/>)
 *  3. Strips remaining control words and group delimiters
 *  4. Converts to [android.text.Spanned] via [Html.fromHtml]
 *  5. Delivers the Spanned to [RenderCallbacks.showText] (monospace=false)
 *
 * No third-party libraries required — uses built-in Android [Html] class.
 */
internal class RtfDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val spanned = withContext(Dispatchers.IO) { buildSpanned(filePath) }
        callbacks.showText(spanned, monospace = false)
    }

    private fun buildSpanned(filePath: String): CharSequence {
        val bytes = File(filePath).readBytes()
        val raw = decodeRtfBytes(bytes)

        // Step 1: remove nested RTF groups repeatedly
        var cleaned = raw
        repeat(5) { cleaned = cleaned.replace(Regex("\\{[^{}]*\\}"), "") }

        // Step 2: map formatting control words → HTML tags
        cleaned = cleaned
            .replace(Regex("\\\\b\\b"),       "<b>")
            .replace(Regex("\\\\b0\\b"),      "</b>")
            .replace(Regex("\\\\i\\b"),       "<i>")
            .replace(Regex("\\\\i0\\b"),      "</i>")
            .replace(Regex("\\\\ul\\b"),      "<u>")
            .replace(Regex("\\\\ulnone\\b"),  "</u>")
            .replace(Regex("\\\\strike\\b"),  "<del>")
            .replace(Regex("\\\\strike0\\b"), "</del>")
            .replace(Regex("\\\\par\\b"),     "<br/>")
            .replace(Regex("\\\\line\\b"),    "<br/>")

        // Step 3: protect inserted tags, escape HTML special chars, then restore
        val tags = listOf("<b>","</b>","<i>","</i>","<u>","</u>","<del>","</del>","<br/>")
        val placeholders = tags.mapIndexed { i, _ -> "\u0001TAG$i\u0001" }
        tags.forEachIndexed { i, tag -> cleaned = cleaned.replace(tag, placeholders[i]) }
        cleaned = cleaned
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        placeholders.forEachIndexed { i, ph -> cleaned = cleaned.replace(ph, tags[i]) }

        // Step 4: strip remaining RTF control words and delimiters
        cleaned = cleaned
            .replace(Regex("\\\\[a-z]+[0-9]* ?"), " ")
            .replace(Regex("[{}\\\\]"), "")
            .trim()

        // Step 5: parse HTML to Spanned
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(cleaned, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(cleaned)
        }
    }

    /** Decode RTF bytes using the declared \\ansicpg code page, or Windows-1252. */
    private fun decodeRtfBytes(bytes: ByteArray): String {
        val header = String(bytes, 0, minOf(bytes.size, 512), Charsets.ISO_8859_1)
        val codePage = Regex("\\\\ansicpg(\\d+)")
            .find(header)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
        val charset = when (codePage) {
            65001 -> Charsets.UTF_8
            1252, null -> Charsets.ISO_8859_1
            else -> try {
                charset("windows-$codePage")
            } catch (_: Exception) {
                Charsets.ISO_8859_1
            }
        }
        return String(bytes, charset)
    }
}
