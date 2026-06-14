package com.example.universal_dockit.renderers

import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * TxtDocumentRenderer — renders plain-text (.txt) files.
 *
 * Rendering strategy:
 *  - Reads the file line-by-line with [BufferedReader] (memory-efficient)
 *  - Passes the raw [String] to [RenderCallbacks.showText] with monospace=true
 *  - The Activity displays it in a dark-themed, selectable [android.widget.TextView]
 *
 * No third-party libraries required — uses standard Java I/O only.
 */
internal class TxtDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val text = withContext(Dispatchers.IO) {
            val sb = StringBuilder()
            BufferedReader(
                InputStreamReader(FileInputStream(filePath), StandardCharsets.UTF_8),
            ).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.appendLine(line)
                }
            }
            sb.toString()
        }
        callbacks.showText(text, monospace = true)
    }
}
