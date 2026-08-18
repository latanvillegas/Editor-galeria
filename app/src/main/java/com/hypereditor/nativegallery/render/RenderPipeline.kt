package com.hypereditor.nativegallery.render

import android.graphics.Bitmap
import com.hypereditor.nativegallery.domain.model.EditorDocument
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RenderPipeline(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend fun renderPreview(
        baseBitmap: Bitmap,
        document: EditorDocument,
        maxSize: Int = 1920
    ): Bitmap = withContext(defaultDispatcher) {
        BitmapRenderer.renderDocument(baseBitmap, document, isPreview = true, maxDimension = maxSize)
    }

    suspend fun renderFullResolution(
        baseBitmap: Bitmap,
        document: EditorDocument
    ): Bitmap = withContext(defaultDispatcher) {
        BitmapRenderer.renderDocument(baseBitmap, document, isPreview = false)
    }
}
