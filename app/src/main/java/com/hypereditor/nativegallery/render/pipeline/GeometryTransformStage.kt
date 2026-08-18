package com.hypereditor.nativegallery.render.pipeline

import android.graphics.Bitmap
import android.graphics.Matrix
import com.hypereditor.nativegallery.domain.model.EditorDocument

class GeometryTransformStage : RenderStage {
    override val name: String = "GeometryTransformStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        val crop = document.cropTransform
        var intermediate = input
        val srcW = intermediate.width
        val srcH = intermediate.height

        // 1. Recorte (Crop)
        val hasCrop = crop.cropLeftNorm > 0.001f || crop.cropTopNorm > 0.001f ||
                crop.cropRightNorm < 0.999f || crop.cropBottomNorm < 0.999f

        if (hasCrop) {
            val cropL = (crop.cropLeftNorm * srcW).toInt().coerceIn(0, srcW - 2)
            val cropT = (crop.cropTopNorm * srcH).toInt().coerceIn(0, srcH - 2)
            val cropR = (crop.cropRightNorm * srcW).toInt().coerceIn(cropL + 2, srcW)
            val cropB = (crop.cropBottomNorm * srcH).toInt().coerceIn(cropT + 2, srcH)
            val cropW = (cropR - cropL).coerceAtLeast(1)
            val cropH = (cropB - cropT).coerceAtLeast(1)
            intermediate = Bitmap.createBitmap(intermediate, cropL, cropT, cropW, cropH)
        }

        // 2. Rotación, Enderezado y Flip
        val totalRotation = (crop.rotation90Degrees.toFloat() + crop.fineStraightenAngle) % 360f
        val hasRotation = totalRotation != 0f
        val hasFlip = crop.flipHorizontal || crop.flipVertical

        if (hasRotation || hasFlip) {
            val matrix = Matrix()
            if (crop.flipHorizontal || crop.flipVertical) {
                val scaleX = if (crop.flipHorizontal) -1f else 1f
                val scaleY = if (crop.flipVertical) -1f else 1f
                matrix.postScale(scaleX, scaleY, intermediate.width / 2f, intermediate.height / 2f)
            }
            if (hasRotation) {
                matrix.postRotate(totalRotation, intermediate.width / 2f, intermediate.height / 2f)
            }
            intermediate = Bitmap.createBitmap(
                intermediate, 0, 0, intermediate.width, intermediate.height, matrix, true
            )
        }

        return intermediate
    }
}
