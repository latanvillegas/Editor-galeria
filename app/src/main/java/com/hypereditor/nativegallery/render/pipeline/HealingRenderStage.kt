package com.hypereditor.nativegallery.render.pipeline

import android.graphics.Bitmap
import android.graphics.Color
import com.hypereditor.nativegallery.domain.model.EditorDocument
import com.hypereditor.nativegallery.domain.model.EditOperation
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Etapa de renderizado no destructivo para Pincel Corrector (Healing Brush / Spot Healing).
 *
 * A diferencia del Tampón de Clonar (que copia píxeles exactos creando bordes o diferencias de tono),
 * Healing extrae la textura y micro-detalle de alta frecuencia de la zona de muestreo y la sintetiza
 * sobre el campo de color, luminosidad y gradiente del entorno perimetral inmediato del área destino.
 */
class HealingRenderStage : RenderStage {
    override val name: String = "HealingRenderStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        if (document.healingStrokes.isEmpty()) return input

        val result = input.copy(Bitmap.Config.ARGB_8888, true)
        val imgWidth = result.width.toFloat()
        val imgHeight = result.height.toFloat()

        for (stroke in document.healingStrokes) {
            applyHealingStroke(result, stroke, imgWidth, imgHeight)
        }

        return result
    }

    private fun applyHealingStroke(
        bitmap: Bitmap,
        stroke: EditOperation.HealingStroke,
        imgWidth: Float,
        imgHeight: Float
    ) {
        if (stroke.points.isEmpty()) return

        val radius = stroke.radius.coerceIn(6f, 250f)
        val feather = stroke.feather.coerceIn(0.1f, 1.0f)
        val strength = stroke.strength.coerceIn(0.1f, 1.0f)

        for (point in stroke.points) {
            val cx = (point.targetX * imgWidth).toInt()
            val cy = (point.targetY * imgHeight).toInt()

            healLocalSpot(bitmap, cx, cy, radius, feather, strength, stroke.manualSourceOffset, imgWidth, imgHeight)
        }
    }

    private fun healLocalSpot(
        bitmap: Bitmap,
        cx: Int,
        cy: Int,
        radius: Float,
        feather: Float,
        strength: Float,
        manualOffset: EditOperation.PointOffset?,
        imgWidth: Float,
        imgHeight: Float
    ) {
        val w = bitmap.width
        val h = bitmap.height

        // Si el centro cae completamente fuera de la imagen, no aplicar
        if (cx < 0 || cx >= w || cy < 0 || cy >= h) return

        val rInt = radius.toInt()
        val boundDist = (radius * 1.25f).toInt().coerceAtLeast(rInt + 2)

        // 1. Muestreo del anillo perimetral circundante (Boundary / Neighborhood)
        // Analizamos el color y luminosidad de los píxeles sanos que rodean la imperfección
        var ringSumR = 0f
        var ringSumG = 0f
        var ringSumB = 0f
        var ringCount = 0

        val ringSamples = 24
        for (i in 0 until ringSamples) {
            val angle = (2.0 * Math.PI * i / ringSamples).toFloat()
            val sampleDist = radius * 1.25f
            val bx = (cx + sampleDist * cos(angle)).toInt()
            val by = (cy + sampleDist * sin(angle)).toInt()
            if (bx in 0 until w && by in 0 until h) {
                val pixel = bitmap.getPixel(bx, by)
                ringSumR += Color.red(pixel)
                ringSumG += Color.green(pixel)
                ringSumB += Color.blue(pixel)
                ringCount++
            }
        }

        if (ringCount == 0) return // Sin información circundante válida

        val targetBgR = ringSumR / ringCount
        val targetBgG = ringSumG / ringCount
        val targetBgB = ringSumB / ringCount

        // 2. Determinación de la zona de origen de textura (Source)
        val (srcCenterX, srcCenterY) = if (manualOffset != null) {
            val sx = (cx + manualOffset.dx * imgWidth).toInt().coerceIn(rInt, w - 1 - rInt)
            val sy = (cy + manualOffset.dy * imgHeight).toInt().coerceIn(rInt, h - 1 - rInt)
            Pair(sx, sy)
        } else {
            // Muestreo automático local: buscamos en 8 direcciones contiguas un parche limpio
            findBestLocalSource(bitmap, cx, cy, radius, w, h, targetBgR, targetBgG, targetBgB)
        }

        // 3. Extracción de color medio de la fuente para calcular la textura de alta frecuencia
        var srcSumR = 0f
        var srcSumG = 0f
        var srcSumB = 0f
        var srcCount = 0

        val rSq = radius * radius
        for (dy in -rInt..rInt) {
            val sy = srcCenterY + dy
            if (sy !in 0 until h) continue
            for (dx in -rInt..rInt) {
                if (dx * dx + dy * dy > rSq) continue
                val sx = srcCenterX + dx
                if (sx !in 0 until w) continue
                val pixel = bitmap.getPixel(sx, sy)
                srcSumR += Color.red(pixel)
                srcSumG += Color.green(pixel)
                srcSumB += Color.blue(pixel)
                srcCount++
            }
        }

        if (srcCount == 0) return

        val srcMeanR = srcSumR / srcCount
        val srcMeanG = srcSumG / srcCount
        val srcMeanB = srcSumB / srcCount

        // 4. Síntesis y fusión armónica: textura de origen sobre iluminación y tono del destino
        val innerRadius = (radius * (1.0f - feather)).coerceAtLeast(0f)
        val featherWidth = (radius - innerRadius).coerceAtLeast(1f)

        for (dy in -rInt..rInt) {
            val py = cy + dy
            if (py !in 0 until h) continue
            for (dx in -rInt..rInt) {
                val px = cx + dx
                if (px !in 0 until w) continue

                val dist = hypot(dx.toFloat(), dy.toFloat())
                if (dist > radius) continue

                // Peso con desvanecimiento suavizado (Hermite / Smoothstep)
                val weight = if (dist <= innerRadius) {
                    1.0f
                } else {
                    val t = ((radius - dist) / featherWidth).coerceIn(0f, 1f)
                    t * t * (3f - 2f * t)
                } * strength

                if (weight <= 0.001f) continue

                // Píxel de textura correspondiente en la fuente
                val sx = (srcCenterX + dx).coerceIn(0, w - 1)
                val sy = (srcCenterY + dy).coerceIn(0, h - 1)
                val srcPixel = bitmap.getPixel(sx, sy)

                // Detalle de alta frecuencia: delta = pixel - promedio
                val deltaR = Color.red(srcPixel) - srcMeanR
                val deltaG = Color.green(srcPixel) - srcMeanG
                val deltaB = Color.blue(srcPixel) - srcMeanB

                // Reconstrucción con el tono y luminosidad del destino
                val healedR = (targetBgR + deltaR).coerceIn(0f, 255f)
                val healedG = (targetBgG + deltaG).coerceIn(0f, 255f)
                val healedB = (targetBgB + deltaB).coerceIn(0f, 255f)

                // Mezcla con el píxel original
                val origPixel = bitmap.getPixel(px, py)
                val finalR = (Color.red(origPixel) * (1f - weight) + healedR * weight).toInt().coerceIn(0, 255)
                val finalG = (Color.green(origPixel) * (1f - weight) + healedG * weight).toInt().coerceIn(0, 255)
                val finalB = (Color.blue(origPixel) * (1f - weight) + healedB * weight).toInt().coerceIn(0, 255)

                bitmap.setPixel(px, py, Color.argb(255, finalR, finalG, finalB))
            }
        }
    }

    /**
     * Busca la mejor zona vecina local fuera de la mancha candidata para transferir textura.
     * Evalúa 8 direcciones a distancia 2.2 * radio y elige la de menor desviación cromática.
     */
    private fun findBestLocalSource(
        bitmap: Bitmap,
        cx: Int,
        cy: Int,
        radius: Float,
        w: Int,
        h: Int,
        targetBgR: Float,
        targetBgG: Float,
        targetBgB: Float
    ): Pair<Int, Int> {
        val candidateDistance = radius * 2.2f
        var bestX = (cx + candidateDistance).toInt().coerceIn(0, w - 1)
        var bestY = cy.coerceIn(0, h - 1)
        var minDiff = Float.MAX_VALUE

        val angles = doubleArrayOf(
            0.0,
            Math.PI / 4.0,
            Math.PI / 2.0,
            3.0 * Math.PI / 4.0,
            Math.PI,
            5.0 * Math.PI / 4.0,
            3.0 * Math.PI / 2.0,
            7.0 * Math.PI / 4.0
        )

        for (angle in angles) {
            val candX = (cx + candidateDistance * cos(angle)).toInt()
            val candY = (cy + candidateDistance * sin(angle)).toInt()

            val rInt = radius.toInt()
            if (candX - rInt < 0 || candX + rInt >= w || candY - rInt < 0 || candY + rInt >= h) {
                continue
            }

            val p = bitmap.getPixel(candX, candY)
            val diff = hypot(
                (Color.red(p) - targetBgR),
                hypot((Color.green(p) - targetBgG), (Color.blue(p) - targetBgB))
            )

            if (diff < minDiff) {
                minDiff = diff
                bestX = candX
                bestY = candY
            }
        }

        return Pair(bestX, bestY)
    }
}
