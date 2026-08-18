package com.hypereditor.nativegallery.render.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.hypereditor.nativegallery.domain.model.EditorDocument

class ColorAdjustmentStage : RenderStage {
    override val name: String = "ColorAdjustmentStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        val adj = document.adjustments
        val hasBrightnessOrExposure = adj.brightness != 0f || adj.exposure != 0f
        val hasContrast = adj.contrast != 1f
        val hasSaturation = adj.saturation != 1f
        val hasTempOrTint = adj.temperature != 0f || adj.tint != 0f

        if (!hasBrightnessOrExposure && !hasContrast && !hasSaturation && !hasTempOrTint) {
            return input
        }

        val result = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val combinedMatrix = ColorMatrix()

        // 1. Brillo y Exposición
        val brightnessShift = (adj.brightness * 255f) + (adj.exposure * 60f)
        if (brightnessShift != 0f) {
            val brightnessMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, brightnessShift,
                    0f, 1f, 0f, 0f, brightnessShift,
                    0f, 0f, 1f, 0f, brightnessShift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(brightnessMatrix)
        }

        // 2. Contraste
        if (adj.contrast != 1f) {
            val c = adj.contrast
            val contrastTranslate = (-0.5f * c + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, contrastTranslate,
                    0f, c, 0f, 0f, contrastTranslate,
                    0f, 0f, c, 0f, contrastTranslate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(contrastMatrix)
        }

        // 3. Saturación
        if (adj.saturation != 1f) {
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(adj.saturation)
            combinedMatrix.postConcat(satMatrix)
        }

        // 4. Temperatura & Tinte
        if (hasTempOrTint) {
            val tempR = if (adj.temperature > 0) adj.temperature * 30f else 0f
            val tempB = if (adj.temperature < 0) -adj.temperature * 30f else 0f
            val tintG = if (adj.tint < 0) -adj.tint * 25f else 0f
            val tintM = if (adj.tint > 0) adj.tint * 25f else 0f
            val tempTintMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, tempR + tintM,
                    0f, 1f, 0f, 0f, tintG,
                    0f, 0f, 1f, 0f, tempB + tintM,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(tempTintMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(input, 0f, 0f, paint)
        return result
    }
}
