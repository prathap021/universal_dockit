package com.example.universal_dockit.renderers

import com.example.universal_dockit.RenderCallbacks

/**
 * DocumentRenderer — base interface for all format-specific renderers.
 *
 * Each renderer is responsible for one document format (or a closely related
 * pair such as DOC/DOCX). Renderers communicate results back through the
 * [RenderCallbacks] interface, which decouples them from any particular View.
 *
 * Renderers should:
 *  - Perform all I/O on [kotlinx.coroutines.Dispatchers.IO]
 *  - Call [RenderCallbacks] methods to update the UI
 *  - Catch format-specific exceptions and call [RenderCallbacks.showError]
 */
internal interface DocumentRenderer {
    /**
     * Render the document at [filePath] and deliver the result via [callbacks].
     *
     * This function is called from a coroutine scope managed by the Activity;
     * implementors must use [kotlinx.coroutines.withContext] for thread switching.
     */
    suspend fun render(filePath: String, callbacks: RenderCallbacks)
}
