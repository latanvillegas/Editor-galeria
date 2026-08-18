package com.hypereditor.nativegallery.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.hypereditor.nativegallery.domain.model.EditorDocument
import com.hypereditor.nativegallery.render.RenderPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportManager(
    private val context: Context,
    private val renderPipeline: RenderPipeline
) {
    suspend fun renderAndExport(
        baseBitmap: Bitmap,
        document: EditorDocument,
        quality: Int = 95
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val fullResolution = renderPipeline.renderFullResolution(baseBitmap, document)
            MediaStoreSaver.saveBitmapToGallery(context, fullResolution, quality = quality)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
