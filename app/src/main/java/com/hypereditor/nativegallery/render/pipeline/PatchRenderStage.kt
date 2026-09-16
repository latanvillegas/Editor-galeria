package com.hypereditor.nativegallery.render.pipeline

import android.graphics.Bitmap
import android.graphics.Color
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.EditorDocument
import kotlin.math.*

/**
 * PatchRenderStage
 * Implementa la herramienta Parche Manual (Patch Tool) inspirada en Photoshop.
 * Permite seleccionar un área defectuosa y arrastrarla a un área limpia de origen.
 * Mezcla la micro-textura y detalle de alta frecuencia de la fuente limpia con la
 * iluminación media, tono y gradiente perimetral del destino.
 */
class PatchRenderStage : RenderStage {
    override val name: String = "PatchRenderStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        val patches = document.patchOperations
        if (patches.isEmpty()) return input

        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)

        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        for (patch in patches) {
            applySinglePatch(pixels, width, height, patch)
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applySinglePatch(
        pixels: IntArray,
        width: Int,
        height: Int,
        patch: EditOperation.PatchOperation
    ) {
        val minDim = min(width, height)
        val radiusPx = (patch.radiusNorm * minDim).coerceIn(8f, minDim * 0.45f)
        val featherPx = (radiusPx * patch.feather.coerceIn(0.1f, 0.95f)).coerceAtLeast(2f)

        val targetCx = (patch.targetCenterXNorm * width).roundToInt().coerceIn(0, width - 1)
        val targetCy = (patch.targetCenterYNorm * height).roundToInt().coerceIn(0, height - 1)
        val sourceCx = (patch.sourceCenterXNorm * width).roundToInt().coerceIn(0, width - 1)
        val sourceCy = (patch.sourceCenterYNorm * height).roundToInt().coerceIn(0, height - 1)

        val offsetX = sourceCx - targetCx
        val offsetY = sourceCy - targetCy

        // 1. Muestreo del anillo perimetral destino (zona sana circundante)
        val outerRadius = radiusPx * 1.35f
        var targetRingR = 0.0
        var targetRingG = 0.0
        var targetRingB = 0.0
        var targetRingCount = 0

        val maxRadiusSq = (outerRadius * outerRadius)
        val innerRadiusSq = (radiusPx * radiusPx)
        val boundingBox = outerRadius.toInt() + 2

        for (dy in -boundingBox..boundingBox) {
            val py = targetCy + dy
            if (py !in 0 until height) continue
            val dySq = dy * dy
            for (dx in -boundingBox..boundingBox) {
                val px = targetCx + dx
                if (px !in 0 until width) continue
                val distSq = (dx * dx + dySq).toFloat()
                if (distSq in innerRadiusSq..maxRadiusSq) {
                    val color = pixels[py * width + px]
                    targetRingR += Color.red(color)
                    targetRingG += Color.green(color)
                    targetRingB += Color.blue(color)
                    targetRingCount++
                }
            }
        }

        if (targetRingCount == 0) return
        val tMeanR = targetRingR / targetRingCount
        val tMeanG = targetRingG / targetRingCount
        val tMeanB = targetRingB / targetRingCount

        // 2. Muestreo del área fuente para textura
        var sourceR = 0.0
        var sourceG = 0.0
        var sourceB = 0.0
        var sourceCount = 0
        val rBox = radiusPx.toInt() + 1

        for (dy in -rBox..rBox) {
            val sy = sourceCy + dy
            if (sy !in 0 until height) continue
            val dySq = dy * dy
            for (dx in -rBox..rBox) {
                val sx = sourceCx + dx
                if (sx !in 0 until width) continue
                if ((dx * dx + dySq) <= innerRadiusSq) {
                    val color = pixels[sy * width + sx]
                    sourceR += Color.red(color)
                    sourceG += Color.green(color)
                    sourceB += Color.blue(color)
                    sourceCount++
                }
            }
        }

        if (sourceCount == 0) return
        val sMeanR = sourceR / sourceCount
        val sMeanG = sourceG / sourceCount
        val sMeanB = sourceB / sourceCount

        // 3. Fusión de parche hacia el destino
        val strength = patch.strength.coerceIn(0.1f, 1.0f)
        val innerFeatherRadius = max(0f, radiusPx - featherPx)

        for (dy in -rBox..rBox) {
            val ty = targetCy + dy
            if (ty !in 0 until height) continue
            val dySq = dy * dy

            val sy = ty + offsetY
            if (sy !in 0 until height) continue

            for (dx in -rBox..rBox) {
                val tx = targetCx + dx
                if (tx !in 0 until width) continue

                val sx = tx + offsetX
                if (sx !in 0 until width) continue

                val dist = sqrt((dx * dx + dySq).toDouble()).toFloat()
                if (dist > radiusPx) continue

                // Factor de decaimiento por suavizado (Feather)
                val alphaWeight = if (dist <= innerFeatherRadius) {
                    1.0f
                } else {
                    val t = (radiusPx - dist) / (radiusPx - innerFeatherRadius).coerceAtLeast(0.001f)
                    // Smoothstep cubic interpolation
                    t * t * (3f - 2f * t)
                } * strength

                val origTarget = pixels[ty * width + tx]
                val origSource = pixels[sy * width + sx]

                val otR = Color.red(origTarget)
                val otG = Color.green(origTarget)
                val otB = Color.blue(origTarget)

                val osR = Color.red(origSource)
                val osG = Color.green(origSource)
                val osB = Color.blue(origSource)

                // Extraer componente de alta frecuencia / textura de la fuente
                val deltaR = osR - sMeanR
                val deltaG = osG - sMeanG
                val deltaB = osB - sMeanB

                // Sintetizar nuevo píxel sobre el campo de iluminación de destino
                val blendedR = (tMeanR + deltaR).coerceIn(0.0, 255.0)
                val blendedG = (tMeanG + deltaG).coerceIn(0.0, 255.0)
                val blendedB = (tMeanB + deltaB).coerceIn(0.0, 255.0)

                // Mezclar con el destino original según peso
                val finalR = (otR * (1f - alphaWeight) + blendedR * alphaWeight).roundToInt().coerceIn(0, 255)
                val finalG = (otG * (1f - alphaWeight) + blendedG * alphaWeight).roundToInt().coerceIn(0, 255)
                val finalB = (otB * (1f - alphaWeight) + blendedB * alphaWeight).roundToInt().coerceIn(0, 255)

                pixels[ty * width + tx] = Color.argb(Color.alpha(origTarget), finalR, finalG, finalB)
            }
        }
    }
}
