package com.example.universal_dockit.renderers

import com.example.universal_dockit.HtmlTemplates.esc
import com.example.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

internal class CbzDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val (html, baseUrl) = withContext(Dispatchers.IO) {
            val file = File(filePath)
            val cacheDir = File(callbacks.context.cacheDir, "cbz_cache/${file.nameWithoutExtension}")
            cacheDir.mkdirs()

            val images = mutableListOf<String>()
            ZipFile(file).use { zip ->
                zip.entries().asSequence()
                    .filter { !it.isDirectory && it.name.matches(Regex(".*\\.(png|jpe?g|webp|gif|bmp)$", RegexOption.IGNORE_CASE)) }
                    .sortedBy { it.name }
                    .forEach { entry ->
                        val outFile = File(cacheDir, File(entry.name).name)
                        if (!outFile.exists()) {
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(outFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        images.add(outFile.name)
                    }
            }

            val htmlContent = buildString {
                append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=5.0'>")
                append("<style>body { margin: 0; background: #000; text-align: center; } img { max-width: 100%; height: auto; display: block; margin: 0 auto; }</style>")
                append("</head><body>")
                if (images.isEmpty()) {
                    append("<p style='color:white'>No images found in CBZ</p>")
                } else {
                    images.forEach { img ->
                        append("<img src=\"${img.esc()}\" />")
                    }
                }
                append("</body></html>")
            }

            Pair(htmlContent, "file://${cacheDir.absolutePath}/")
        }
        callbacks.showWebContent(html, baseUrl)
    }
}
