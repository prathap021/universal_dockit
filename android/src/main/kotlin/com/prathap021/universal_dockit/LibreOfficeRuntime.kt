package com.prathap021.universal_dockit

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import org.libreoffice.kit.Document
import org.libreoffice.kit.LibreOfficeKit
import org.libreoffice.kit.Office
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Headless LibreOfficeKit runtime for Office → PDF conversion on a dedicated background thread.
 *
 * Requires native runtime files installed via [android/scripts/setup_libreoffice_runtime.sh].
 */
internal object LibreOfficeRuntime {
    private const val CACHE_VERSION = 1

    private val kitThread = HandlerThread("LibreOfficeKit").apply { start() }
    private val kitHandler = Handler(kitThread.looper)

    @Volatile
    private var office: Office? = null

    @Volatile
    private var nativeAvailable: Boolean? = null

    fun isAvailable(): Boolean {
        nativeAvailable?.let { return it }
        return try {
            LibreOfficeKit.initializeLibrary()
            nativeAvailable = true
            true
        } catch (_: UnsatisfiedLinkError) {
            nativeAvailable = false
            false
        } catch (_: Throwable) {
            nativeAvailable = false
            false
        }
    }

    fun cachedPdfFile(context: Context, sourcePath: String): File {
        val name = sourcePath.hashCode().toString(16)
        return File(context.cacheDir, "lo_converted_v${CACHE_VERSION}_$name.pdf")
    }

    suspend fun convertToPdf(context: Context, source: File, output: File) {
        if (!isAvailable()) {
            throw IllegalStateException(
                "LibreOffice native runtime is not installed. " +
                    "Run android/scripts/setup_libreoffice_runtime.sh with a LibreOffice APK."
            )
        }
        if (!source.exists()) {
            throw IllegalArgumentException("Source file not found: ${source.absolutePath}")
        }

        suspendCancellableCoroutine { cont ->
            kitHandler.post {
                try {
                    ensureOffice(context)
                    val officeInstance = office
                        ?: throw IllegalStateException("LibreOffice office handle is null")

                    val loadPath = encodedLoadPath(source)
                    var document = officeInstance.documentLoad(loadPath)
                    if (document == null) {
                        officeInstance.destroy()
                        office = Office(LibreOfficeKit.getLibreOfficeKitHandle()).apply {
                            setOptionalFeatures(Document.LOK_FEATURE_DOCUMENT_PASSWORD)
                        }
                        document = office?.documentLoad(loadPath)
                    }

                    if (document == null) {
                        val error = office?.error?.takeIf { it.isNotEmpty() } ?: "unknown error"
                        throw IllegalStateException("LibreOffice failed to open document: $error")
                    }

                    try {
                        output.parentFile?.mkdirs()
                        if (output.exists()) {
                            output.delete()
                        }
                        document.saveAs("file://${output.absolutePath}", "pdf", "")
                        val exportError = office?.error?.takeIf { it.isNotEmpty() }
                        if (!output.exists() || output.length() == 0L) {
                            throw IllegalStateException(
                                exportError ?: "LibreOffice PDF export produced an empty file"
                            )
                        }
                    } finally {
                        document.destroy()
                    }

                    cont.resume(Unit)
                } catch (e: Throwable) {
                    cont.resumeWithException(e)
                }
            }
        }
    }

    private fun ensureOffice(context: Context) {
        if (office != null) {
            return
        }
        LibreOfficeKit.putenv("SAL_LOK_OPTIONS=compact_fonts")
        LibreOfficeKit.init(context)
        office = Office(LibreOfficeKit.getLibreOfficeKitHandle()).apply {
            setOptionalFeatures(Document.LOK_FEATURE_DOCUMENT_PASSWORD)
        }
    }

    private fun encodedLoadPath(file: File): String {
        val encodedName = Uri.encode(file.name)
        return File(file.parent, encodedName).path
    }
}
