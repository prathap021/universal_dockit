package com.prathap021.universal_dockit.renderers

import com.prathap021.universal_dockit.RenderCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

internal class EpubDocumentRenderer : DocumentRenderer {

    override suspend fun render(filePath: String, callbacks: RenderCallbacks) {
        val (html, baseUrl) = withContext(Dispatchers.IO) {
            val file = File(filePath)
            val cacheDir = File(callbacks.context.cacheDir, "epub_cache/${file.nameWithoutExtension}")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
            cacheDir.mkdirs()

            ZipFile(file).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory) {
                        val outFile = File(cacheDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outFile).use { output -> input.copyTo(output) }
                        }
                    }
                }
            }

            var opfPath = ""
            val containerFile = File(cacheDir, "META-INF/container.xml")
            if (containerFile.exists()) {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(containerFile.inputStream(), "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                        opfPath = parser.getAttributeValue(null, "full-path") ?: ""
                        break
                    }
                    event = parser.next()
                }
            }

            val opfFile = File(cacheDir, opfPath)
            val spineIds = mutableListOf<String>()
            val manifest = mutableMapOf<String, String>()

            if (opfFile.exists()) {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(opfFile.inputStream(), "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        if (parser.name == "item") {
                            val id = parser.getAttributeValue(null, "id")
                            val href = parser.getAttributeValue(null, "href")
                            if (id != null && href != null) manifest[id] = href
                        } else if (parser.name == "itemref") {
                            val idref = parser.getAttributeValue(null, "idref")
                            if (idref != null) spineIds.add(idref)
                        }
                    }
                    event = parser.next()
                }
            }

            val opfDir = opfFile.parentFile ?: cacheDir

            val finalHtml = buildString {
                append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=5.0'>")
                append("<style>body { padding: 16px; font-family: sans-serif; max-width: 800px; margin: 0 auto; }</style>")
                append("</head><body>")
                
                if (spineIds.isEmpty()) {
                    append("<p>Could not parse EPUB spine.</p>")
                } else {
                    spineIds.forEach { id ->
                        val href = manifest[id]
                        if (href != null) {
                            val chapterFile = File(opfDir, href)
                            if (chapterFile.exists()) {
                                val content = chapterFile.readText()
                                val bodyRegex = Regex("<body[^>]*>(.*?)</body>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                                val bodyMatch = bodyRegex.find(content)
                                if (bodyMatch != null) {
                                    append("<div>${bodyMatch.groupValues[1]}</div>")
                                } else {
                                    append("<div>$content</div>")
                                }
                                append("<hr style='margin: 40px 0; border: none; border-bottom: 1px solid #ccc;'/>")
                            }
                        }
                    }
                }
                append("</body></html>")
            }

            Pair(finalHtml, "file://${opfDir.absolutePath}/")
        }
        
        callbacks.showWebContent(html, baseUrl)
    }
}
