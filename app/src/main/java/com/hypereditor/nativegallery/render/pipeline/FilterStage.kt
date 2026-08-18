package com.hypereditor.nativegallery.render.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.hypereditor.nativegallery.domain.model.EditorDocument

class FilterStage : RenderStage {
    override val name: String = "FilterStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        val filter = document.appliedFilter ?: return input
        if (filter.intensity <= 0.001f) return input

        val result = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val filterMatrix = getPresetColorMatrix(filter.filterName, filter.intensity)
        paint.colorFilter = ColorMatrixColorFilter(filterMatrix)
        canvas.drawBitmap(input, 0f, 0f, paint)

        return result
    }

    private fun getPresetColorMatrix(name: String, intensity: Float): ColorMatrix {
        val mat = ColorMatrix()
        val factor = intensity.coerceIn(0f, 1f)

        when (name.uppercase()) {
            "BW" -> {
                mat.setSaturation(1f - factor)
            }
            "SEPIA" -> {
                val rR = 0.393f * factor + (1f - factor)
                val rG = 0.769f * factor
                val rB = 0.189f * factor

                val gR = 0.349f * factor
                val gG = 0.686f * factor + (1f - factor)
                val gB = 0.168f * factor

                val bR = 0.272f * factor
                val bG = 0.534f * factor
                val bB = 0.131f * factor + (1f - factor)

                val sepia = ColorMatrix(
                    floatArrayOf(
                        rR, rG, rB, 0f, 0f,
                        gR, gG, gB, 0f, 0f,
                        bR, bG, bB, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                mat.postConcat(sepia)
            }
            "VIVID" -> {
                mat.setSaturation(1f + (0.9f * factor))
            }
            "CINE" -> {
                val cine = ColorMatrix(
                    floatArrayOf(
                        1f + (0.25f * factor), 0f, 0f, 0f, 20f * factor,
                        0f, 1f + (0.05f * factor), 0f, 0f, 5f * factor,
                        0f, 0f, 1f + (0.35f * factor), 0f, 25f * factor,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                mat.postConcat(cine)
            }
            "WARM" -> {
                val warm = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 38f * factor,
                        0f, 1f, 0f, 0f, 18f * factor,
                        0f, 0f, 1f, 0f, -22f * factor,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                mat.postConcat(warm)
            }
            "COLD" -> {
                val cold = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, -12f * factor,
                        0f, 1f, 0f, 0f, 8f * factor,
                        0f, 0f, 1f, 0f, 42f * factor,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                mat.postConcat(cold)
            }
            "DRAMATIC" -> {
                val c = 1f + (0.5f * factor)
                val cTrans = (-0.5f * c + 0.5f) * 255f
                val contrast = ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, cTrans,
                        0f, c, 0f, 0f, cTrans,
                        0f, 0f, c, 0f, cTrans,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                mat.postConcat(contrast)
                val sat = ColorMatrix()
                sat.setSaturation(1f - (0.3f * factor))
                mat.postConcat(sat)
            }
            "NOIR" -> {
                mat.setSaturation(0f)
                val c = 1f + (0.6f * factor)
                val cTrans = (-0.5f * c + 0.5f) * 255f
                val contrast = ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, cTrans,
                        0f, c, 0f, 0f, cTrans,
                        0f, 0f, c, 0f, cTrans,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                mat.postConcat(contrast)
            }
        }
        return mat
    }
}
