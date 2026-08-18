package com.hypereditor.nativegallery.render.pipeline

import android.graphics.Bitmap
import com.hypereditor.nativegallery.domain.model.EditorDocument

/**
 * Interface representativa de una etapa individual dentro del pipeline de renderizado no destructivo.
 */
interface RenderStage {
    val name: String
    fun process(input: Bitmap, document: EditorDocument): Bitmap
}
