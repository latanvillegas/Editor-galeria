package com.hypereditor.nativegallery.render.pipeline

import android.graphics.Bitmap
import android.graphics.Color
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.EditorDocument
import kotlin.math.*

/**
 * FacialRelightRenderStage
 * Implementa la herramienta Reiluminación Facial Manual inspirada en retoque de retrato y BeautyPlus.
 * Permite modular de forma independiente la luz, sombras, temperatura, contraste y suavidad ligera
 * sobre las zonas faciales (frente, mejillas, nariz, mentón, mandíbula) preservando intacta la
 * micro-textura de los poros y rasgos sin crear un acabado plástico.
 */
class FacialRelightRenderStage : RenderStage {
    override val name: String = "FacialRelightRenderStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        val relights = document.facialRelights.filter { it.globalOpacity > 0.001f && it.zones.any { z -> z.isEnabled } }
        if (relights.isEmpty()) return input

        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)

        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        for (relight in relights) {
            for (zone in relight.zones.filter { it.isEnabled && it.opacity > 0.001f }) {
                applySingleZone(pixels, width, height, zone, relight.globalIntensity * relight.globalOpacity)
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applySingleZone(
        pixels: IntArray,
        width: Int,
        height: Int,
        zone: EditOperation.FacialRelightZone,
        globalFactor: Float
    ) {
        val cx = zone.centerXNorm * width
        val cy = zone.centerYNorm * height
        val rx = (zone.radiusXNorm * width).coerceAtLeast(10f)
        val ry = (zone.radiusYNorm * height).coerceAtLeast(10f)

        val expFactor = 2.0.pow(zone.exposure.toDouble() * zone.intensity).toFloat()
        val contrastFactor = 1.0f + (zone.contrast - 1.0f) * zone.intensity
        val shadowLift = zone.shadows * zone.intensity * 40f
        val tempShift = zone.temperature * zone.intensity * 25f
        val feather = zone.feather.coerceIn(0.1f, 0.95f)
        val totalOpacity = (zone.opacity * globalFactor).coerceIn(0f, 1f)

        if (totalOpacity < 0.001f) return

        val minX = max(0, (cx - rx * 1.3f).toInt())
        val maxX = min(width - 1, (cx + rx * 1.3f).toInt())
        val minY = max(0, (cy - ry * 1.3f).toInt())
        val maxY = min(height - 1, (cy + ry * 1.3f).toInt())

        for (y in minY..maxY) {
            val dy = y - cy
            val dyNormSq = (dy * dy) / (ry * ry)
            for (x in minX..maxX) {
                val dx = x - cx
                val distNorm = sqrt((dx * dx) / (rx * rx) + dyNormSq).toFloat()
                if (distNorm >= 1.0f) continue

                // Smoothstep decay
                val innerCutoff = 1.0f - feather
                val weight = if (distNorm <= innerCutoff) {
                    1.0f
                } else {
                    val t = (1.0f - distNorm) / feather
                    t * t * (3f - 2f * t)
                } * totalOpacity

                if (weight < 0.001f) continue

                val idx = y * width + x
                val origColor = pixels[idx]
                val a = Color.alpha(origColor)
                var r = Color.red(origColor).toFloat()
                var g = Color.green(origColor).toFloat()
                var b = Color.blue(origColor).toFloat()

                // Exposición
                r *= expFactor
                g *= expFactor
                b *= expFactor

                // Sombras
                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                if (shadowLift != 0f) {
                    val sFactor = (1.0f - lum).coerceIn(0f, 1f)
                    r += shadowLift * sFactor
                    g += shadowLift * sFactor
                    b += shadowLift * sFactor
                }

                // Contraste local
                if (contrastFactor != 1.0f) {
                    r = ((r - 128f) * contrastFactor) + 128f
                    g = ((g - 128f) * contrastFactor) + 128f
                    b = ((b - 128f) * contrastFactor) + 128f
                }

                // Temperatura localizada (calidez facial o frescura)
                if (tempShift != 0f) {
                    r += tempShift
                    b -= tempShift
                }

                // Preservación de textura: la piel retiene su grano natural
                val origR = Color.red(origColor).toFloat()
                val origG = Color.green(origColor).toFloat()
                val origB = Color.blue(origColor).toFloat()

                val adjR = r.coerceIn(0f, 255f)
                val adjG = g.coerceIn(0f, 255f)
                val adjB = b.coerceIn(0f, 255f)

                val finalR = (origR * (1f - weight) + adjR * weight).roundToInt().coerceIn(0, 255)
                val finalG = (origG * (1f - weight) + adjG * weight).roundToInt().coerceIn(0, 255)
                val finalB = (origB * (1f - weight) + adjB * weight).roundToInt().coerceIn(0, 255)

                pixels[idx] = Color.argb(a, finalR, finalG, finalB)
            }
        }
    }
}
