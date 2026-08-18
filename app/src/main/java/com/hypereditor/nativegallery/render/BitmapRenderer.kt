package com.hypereditor.nativegallery.render

import android.graphics.Bitmap
import com.hypereditor.nativegallery.domain.model.EditorDocument
import com.hypereditor.nativegallery.render.pipeline.*

object BitmapRenderer {

    private val pipelineStages: List<RenderStage> = listOf(
        ColorAdjustmentStage(),
        FilterStage(),
        MasksRenderStage(),
        CloneStampRenderStage(),
        LayersRenderStage(),
        BrushDrawRenderStage(),
        TextOverlayRenderStage(),
        GeometryTransformStage()
    )

    fun renderDocument(
        baseBitmap: Bitmap,
        document: EditorDocument,
        isPreview: Boolean = false,
        maxDimension: Int = 2048
    ): Bitmap {
        val srcWidth = baseBitmap.width
        val srcHeight = baseBitmap.height

        var workingBitmap = if (isPreview && (srcWidth > maxDimension || srcHeight > maxDimension)) {
            val scale = maxDimension.toFloat() / maxOf(srcWidth, srcHeight)
            Bitmap.createScaledBitmap(
                baseBitmap,
                (srcWidth * scale).toInt().coerceAtLeast(1),
                (srcHeight * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        for (stage in pipelineStages) {
            workingBitmap = stage.process(workingBitmap, document)
        }

        return workingBitmap
    }
}
