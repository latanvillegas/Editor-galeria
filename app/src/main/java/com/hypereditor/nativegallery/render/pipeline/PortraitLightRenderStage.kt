package com.hypereditor.nativegallery.render.pipeline

import android.graphics.Bitmap
import android.graphics.Color
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.EditorDocument
import kotlin.math.*

/**
 * PortraitLightRenderStage
 * Implementa la herramienta Luz de Retrato Manual inspirada en retratos profesionales y Snapseed.
 * Crea una iluminación localizada (o viñeta/spotlight inverso) no destructiva con control
 * paramétrico de exposición, sombras, altas luces, temperatura, tamaño, feather y opacidad.
 */
class PortraitLightRenderStage : RenderStage {
    override val name: String = "PortraitLightRenderStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        val lights = document.portraitLights.filter { it.isEnabled && it.opacity > 0.001f }
        if (lights.isEmpty()) return input

        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)

        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        for (light in lights) {
            applySinglePortraitLight(pixels, width, height, light)
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applySinglePortraitLight(
        pixels: IntArray,
        width: Int,
        height: Int,
        light: EditOperation.PortraitLight
    ) {
        val cx = light.centerXNorm * width
        val cy = light.centerYNorm * height
        val rx = (light.radiusXNorm * width).coerceAtLeast(10f)
        val ry = (light.radiusYNorm * height).coerceAtLeast(10f)
        val rotRad = Math.toRadians(light.rotationDegrees.toDouble())
        val cosRot = cos(rotRad).toFloat()
        val sinRot = sin(rotRad).toFloat()

        val expFactor = 2.0.pow(light.exposure.toDouble()).toFloat() // Standard photographic EV scale
        val shadowLift = light.shadows
        val highlightAtten = light.highlights
        val tempShift = light.temperature * 32f // shift R and B
        val feather = light.feather.coerceIn(0.1f, 0.95f)
        val opacity = light.opacity.coerceIn(0f, 1f)

        // Bounding box en píxeles
        val maxR = max(rx, ry)
        val left = if (light.isInverted) 0 else max(0, (cx - maxR * 1.5f).toInt())
        val right = if (light.isInverted) width - 1 else min(width - 1, (cx + maxR * 1.5f).toInt())
        val top = if (light.isInverted) 0 else max(0, (cy - maxR * 1.5f).toInt())
        val bottom = if (light.isInverted) height - 1 else min(height - 1, (cy + maxR * 1.5f).toInt())

        for (y in top..bottom) {
            val dy = y - cy
            for (x in left..right) {
                val dx = x - cx

                // Rotación inversa alrededor del centro
                val rotX = dx * cosRot + dy * sinRot
                val rotY = -dx * sinRot + dy * cosRot

                // Distancia normalizada elíptica
                val normDist = sqrt(((rotX * rotX) / (rx * rx) + (rotY * rotY) / (ry * ry)).toDouble()).toFloat()

                // Cálculo de máscara con feather suave
                val baseWeight = if (normDist >= 1.0f) {
                    0f
                } else {
                    val innerCutoff = 1.0f - feather
                    if (normDist <= innerCutoff) {
                        1.0f
                    } else {
                        val t = (1.0f - normDist) / feather
                        t * t * (3f - 2f * t) // Smoothstep
                    }
                }

                val maskWeight = if (light.isInverted) (1.0f - baseWeight) * opacity else baseWeight * opacity
                if (maskWeight < 0.001f) continue

                val idx = y * width + x
                val origColor = pixels[idx]
                val a = Color.alpha(origColor)
                var r = Color.red(origColor).toFloat()
                var g = Color.green(origColor).toFloat()
                var b = Color.blue(origColor).toFloat()

                // Luminancia perceptual
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val lumNorm = lum / 255f

                // 1. Exposición general
                r *= expFactor
                g *= expFactor
                b *= expFactor

                // 2. Control localizado de sombras (sombras tienen lum < 0.5)
                if (shadowLift != 0f) {
                    val shadowWeight = (1.0f - lumNorm).coerceIn(0f, 1f)
                    val sDelta = shadowLift * 45f * shadowWeight
                    r += sDelta
                    g += sDelta
                    b += sDelta
                }

                // 3. Control de altas luces (altas luces tienen lum > 0.5)
                if (highlightAtten != 0f) {
                    val highWeight = lumNorm.coerceIn(0f, 1f)
                    val hDelta = highlightAtten * 45f * highWeight
                    r += hDelta
                    g += hDelta
                    b += hDelta
                }

                // 4. Temperatura de color localizada (cálida / fría en rostro)
                if (tempShift != 0f) {
                    r += tempShift
                    b -= tempShift
                }

                val adjR = r.coerceIn(0f, 255f)
                val adjG = g.coerceIn(0f, 255f)
                val adjB = b.coerceIn(0f, 255f)

                val origR = Color.red(origColor).toFloat()
                val origG = Color.green(origColor).toFloat()
                val origB = Color.blue(origColor).toFloat()

                val finalR = (origR * (1f - maskWeight) + adjR * maskWeight).roundToInt().coerceIn(0, 255)
                val finalG = (origG * (1f - maskWeight) + adjG * maskWeight).roundToInt().coerceIn(0, 255)
                val finalB = (origB * (1f - maskWeight) + adjB * maskWeight).roundToInt().coerceIn(0, 255)

                pixels[idx] = Color.argb(a, finalR, finalG, finalB)
            }
        }
    }
}
