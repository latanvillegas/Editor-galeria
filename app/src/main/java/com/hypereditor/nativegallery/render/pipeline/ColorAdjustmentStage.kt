package com.hypereditor.nativegallery.render.pipeline

import android.graphics.*
import com.hypereditor.nativegallery.domain.model.EditorDocument
import kotlin.math.sqrt

class ColorAdjustmentStage : RenderStage {
    override val name: String = "ColorAdjustmentStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        val adj = document.adjustments

        val hasBrightnessOrExposure = adj.brightness != 0f || adj.exposure != 0f
        val hasContrast = adj.contrast != 1f
        val hasSaturation = adj.saturation != 1f
        val hasTempOrTint = adj.temperature != 0f || adj.tint != 0f
        val hasShadowsHighlights = adj.shadows != 0f || adj.highlights != 0f
        val hasAmbiance = adj.ambiance != 0f
        val hasFade = adj.fade > 0f
        val hasVignette = adj.vignette > 0.01f
        val hasBlur = adj.blur > 0.01f
        val hasGrain = adj.grain > 0.01f

        if (!hasBrightnessOrExposure && !hasContrast && !hasSaturation && !hasTempOrTint &&
            !hasShadowsHighlights && !hasAmbiance && !hasFade && !hasVignette && !hasBlur && !hasGrain
        ) {
            return input
        }

        var working = input

        // 1. Blur adjustment (fast high-performance box/scale blur)
        if (hasBlur) {
            val blurFactor = (adj.blur * 0.15f).coerceIn(0.01f, 0.25f)
            val downW = (input.width * (1f - blurFactor * 3.5f)).toInt().coerceIn(16, input.width)
            val downH = (input.height * (1f - blurFactor * 3.5f)).toInt().coerceIn(16, input.height)
            val smallBmp = Bitmap.createScaledBitmap(working, downW, downH, true)
            working = Bitmap.createScaledBitmap(smallBmp, input.width, input.height, true)
        }

        val result = Bitmap.createBitmap(working.width, working.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val combinedMatrix = ColorMatrix()

        // 2. Brillo, Exposición y Ambiente
        val brightnessShift = (adj.brightness * 255f) + (adj.exposure * 60f) + (adj.ambiance * 20f)
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

        // 3. Contraste & Sombras/Luces (Luminosity adjustments)
        val effectiveContrast = adj.contrast + (adj.highlights * -0.15f) + (adj.shadows * 0.15f)
        if (effectiveContrast != 1f) {
            val c = effectiveContrast.coerceIn(0.1f, 3.0f)
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

        // 4. Saturación & Ambiente
        val effectiveSat = (adj.saturation * (1f + adj.ambiance * 0.3f)).coerceAtLeast(0f)
        if (effectiveSat != 1f) {
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(effectiveSat)
            combinedMatrix.postConcat(satMatrix)
        }

        // 5. Temperatura & Tinte
        if (hasTempOrTint) {
            val tempR = if (adj.temperature > 0) adj.temperature * 32f else 0f
            val tempB = if (adj.temperature < 0) -adj.temperature * 32f else 0f
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

        // 6. Fade (Efecto Mate / Lifted Blacks)
        if (hasFade) {
            val fadeVal = adj.fade.coerceIn(0f, 1f) * 45f
            val fadeMatrix = ColorMatrix(
                floatArrayOf(
                    1f - (adj.fade * 0.1f), 0f, 0f, 0f, fadeVal,
                    0f, 1f - (adj.fade * 0.1f), 0f, 0f, fadeVal,
                    0f, 0f, 1f - (adj.fade * 0.1f), 0f, fadeVal,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(fadeMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(working, 0f, 0f, paint)

        // 7. Viñeta no destructiva con RadialGradient
        if (hasVignette) {
            val w = result.width.toFloat()
            val h = result.height.toFloat()
            val centerX = w / 2f
            val centerY = h / 2f
            val radius = sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()

            val vignetteAlpha = (adj.vignette.coerceIn(0f, 1f) * 220).toInt()
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    centerX, centerY, radius,
                    intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(vignetteAlpha, 0, 0, 0)),
                    floatArrayOf(0f, 0.45f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            canvas.drawRect(0f, 0f, w, h, vignettePaint)
        }

        // 8. Grano analógico sutil
        if (hasGrain) {
            val grainPaint = Paint().apply {
                color = Color.WHITE
                alpha = (adj.grain.coerceIn(0f, 1f) * 45).toInt()
                xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            }
            val rng = java.util.Random(1337)
            val numDots = (result.width * result.height * 0.005f * adj.grain).toInt().coerceIn(100, 15000)
            for (i in 0 until numDots) {
                val rx = rng.nextFloat() * result.width
                val ry = rng.nextFloat() * result.height
                canvas.drawPoint(rx, ry, grainPaint)
            }
        }

        return result
    }
}
