package com.hypereditor.nativegallery.render.pipeline

import android.graphics.*
import com.hypereditor.nativegallery.domain.model.EditorDocument
import com.hypereditor.nativegallery.domain.model.MaskBrushStroke
import com.hypereditor.nativegallery.domain.model.MaskModel
import com.hypereditor.nativegallery.domain.model.SelectionToolType

class MasksRenderStage : RenderStage {
    override val name: String = "MasksRenderStage"

    private val colorAdjustmentStage = ColorAdjustmentStage()

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        val activeMasks = document.masks.filter { it.isEnabled }
        if (activeMasks.isEmpty()) {
            return input
        }

        var result = input.copy(Bitmap.Config.ARGB_8888, true)

        for (mask in activeMasks) {
            result = applySingleMask(result, mask, document)
        }

        return result
    }

    private fun applySingleMask(base: Bitmap, mask: MaskModel, doc: EditorDocument): Bitmap {
        val width = base.width
        val height = base.height

        // 1. Crear bitmap con los ajustes locales aplicados
        val dummyDoc = doc.copy(adjustments = mask.localAdjustments)
        val adjustedBase = colorAdjustmentStage.process(base, dummyDoc)

        // 2. Generar el canal alfa de la máscara (0 = no afectado, 255 = afectado)
        val maskAlphaBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val maskCanvas = Canvas(maskAlphaBmp)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        when (mask.selectionType) {
            SelectionToolType.RECTANGLE -> {
                val r = mask.rectBounds
                val rectF = RectF(
                    r.left * width,
                    r.top * height,
                    r.right * width,
                    r.bottom * height
                )
                maskCanvas.drawRect(rectF, fillPaint)
            }
            SelectionToolType.ELLIPSE -> {
                val e = mask.ellipseBounds
                val ovalF = RectF(
                    e.left * width,
                    e.top * height,
                    e.right * width,
                    e.bottom * height
                )
                maskCanvas.drawOval(ovalF, fillPaint)
            }
            SelectionToolType.LASSO -> {
                if (mask.lassoPoints.size >= 3) {
                    val path = Path()
                    val first = mask.lassoPoints.first()
                    path.moveTo(first.first * width, first.second * height)
                    for (i in 1 until mask.lassoPoints.size) {
                        val pt = mask.lassoPoints[i]
                        path.lineTo(pt.first * width, pt.second * height)
                    }
                    path.close()
                    maskCanvas.drawPath(path, fillPaint)
                }
            }
            SelectionToolType.BRUSH -> {
                for (stroke in mask.brushStrokes) {
                    renderBrushStroke(maskCanvas, stroke, width, height)
                }
            }
        }

        // 3. Invertir máscara si se solicitó
        val finalMaskBmp = if (mask.isInverted) {
            invertAlphaBitmap(maskAlphaBmp)
        } else {
            maskAlphaBmp
        }

        // 4. Suavizado de bordes (Feather)
        val featheredMaskBmp = if (mask.featherRadius > 1f) {
            applyFeather(finalMaskBmp, mask.featherRadius)
        } else {
            finalMaskBmp
        }

        // 5. Componer: base + (adjustedBase enmascarado)
        val output = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // Crear capa temporal para la imagen ajustada recortada por la máscara
        val layerBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layerBmp)

        // Dibujar imagen ajustada
        layerCanvas.drawBitmap(adjustedBase, 0f, 0f, null)

        // Aplicar máscara como alfa
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        layerCanvas.drawBitmap(featheredMaskBmp, 0f, 0f, maskPaint)

        // Dibujar resultado enmascarado sobre la imagen base
        canvas.drawBitmap(layerBmp, 0f, 0f, null)

        return output
    }

    private fun renderBrushStroke(canvas: Canvas, stroke: MaskBrushStroke, width: Int, height: Int) {
        if (stroke.points.size < 2) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = (stroke.strokeWidthNorm * minOf(width, height)).coerceAtLeast(4f)
            if (stroke.isEraser) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                color = Color.BLACK
            }
        }

        val path = Path()
        val first = stroke.points.first()
        path.moveTo(first.first * width, first.second * height)
        for (i in 1 until stroke.points.size) {
            val pt = stroke.points[i]
            path.lineTo(pt.first * width, pt.second * height)
        }
        canvas.drawPath(path, paint)
    }

    private fun invertAlphaBitmap(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val inverted = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(inverted)

        // Rellenar todo con blanco (alfa 255)
        val fullPaint = Paint().apply { color = Color.BLACK }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fullPaint)

        // Restar la máscara original
        val subtractPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        canvas.drawBitmap(src, 0f, 0f, subtractPaint)

        return inverted
    }

    private fun applyFeather(src: Bitmap, radius: Float): Bitmap {
        val width = src.width
        val height = src.height
        val blurred = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(blurred)
        val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(radius.coerceIn(1f, 50f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawBitmap(src, 0f, 0f, blurPaint)
        return blurred
    }
}
