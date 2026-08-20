package com.hypereditor.nativegallery.render.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.hypereditor.nativegallery.domain.model.CropAspectRatio
import com.hypereditor.nativegallery.domain.model.EditorDocument

class GeometryTransformStage : RenderStage {
    override val name: String = "GeometryTransformStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        val crop = document.cropTransform
        val srcW = input.width
        val srcH = input.height

        val totalRotation = (crop.rotation90Degrees.toFloat() + crop.fineStraightenAngle) % 360f
        val hasRotation = Math.abs(totalRotation) > 0.01f
        val hasFlip = crop.flipHorizontal || crop.flipVertical
        val hasProCrop = crop.aspectRatio != CropAspectRatio.ORIGINAL ||
                crop.scale > 1.001f ||
                Math.abs(crop.panXNorm) > 0.001f ||
                Math.abs(crop.panYNorm) > 0.001f

        if (hasProCrop) {
            val targetRatio = when (crop.aspectRatio) {
                CropAspectRatio.ORIGINAL -> srcW.toFloat() / srcH.toFloat()
                CropAspectRatio.FREE -> srcW.toFloat() / srcH.toFloat()
                else -> crop.aspectRatio.getCalculatedRatio(srcW, srcH)
            }

            val outW: Int
            val outH: Int

            if (targetRatio >= 1.0f) {
                outW = srcW
                outH = (srcW / targetRatio).toInt().coerceAtLeast(16)
            } else {
                outH = srcH
                outW = (srcH * targetRatio).toInt().coerceAtLeast(16)
            }

            val outBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outBitmap)

            val matrix = Matrix()
            // 1. Move source center to origin
            matrix.postTranslate(-srcW / 2f, -srcH / 2f)

            // 2. Flip
            if (hasFlip) {
                val scaleX = if (crop.flipHorizontal) -1f else 1f
                val scaleY = if (crop.flipVertical) -1f else 1f
                matrix.postScale(scaleX, scaleY)
            }

            // 3. Rotation
            if (hasRotation) {
                matrix.postRotate(totalRotation)
            }

            // 4. Calculate base scale so image fills crop frame (ContentScale.Crop equivalent)
            val baseScale = maxOf(outW.toFloat() / srcW.toFloat(), outH.toFloat() / srcH.toFloat())
            val finalScale = baseScale * crop.scale.coerceAtLeast(1.0f)
            matrix.postScale(finalScale, finalScale)

            // 5. User pan translation matching interactive canvas pixel-for-pixel
            val transX = (outW / 2f) + (crop.panXNorm * outW)
            val transY = (outH / 2f) + (crop.panYNorm * outH)
            matrix.postTranslate(transX, transY)

            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(input, matrix, paint)

            return outBitmap
        }

        // Standard Crop / Rotation fallback
        var intermediate = input
        val hasNormCrop = crop.cropLeftNorm > 0.001f || crop.cropTopNorm > 0.001f ||
                crop.cropRightNorm < 0.999f || crop.cropBottomNorm < 0.999f

        if (hasNormCrop) {
            val cropL = (crop.cropLeftNorm * srcW).toInt().coerceIn(0, srcW - 2)
            val cropT = (crop.cropTopNorm * srcH).toInt().coerceIn(0, srcH - 2)
            val cropR = (crop.cropRightNorm * srcW).toInt().coerceIn(cropL + 2, srcW)
            val cropB = (crop.cropBottomNorm * srcH).toInt().coerceIn(cropT + 2, srcH)
            val cropW = (cropR - cropL).coerceAtLeast(1)
            val cropH = (cropB - cropT).coerceAtLeast(1)
            intermediate = Bitmap.createBitmap(intermediate, cropL, cropT, cropW, cropH)
        }

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
