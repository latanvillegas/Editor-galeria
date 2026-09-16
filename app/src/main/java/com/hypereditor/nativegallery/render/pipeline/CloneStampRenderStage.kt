package com.hypereditor.nativegallery.render.pipeline

import android.graphics.*
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.EditorDocument

class CloneStampRenderStage : RenderStage {
    override val name: String = "CloneStampRenderStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        if (document.cloneStamps.isEmpty()) {
            return input
        }

        val result = input.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = input.width.toFloat()
        val height = input.height.toFloat()

        for (stamp in document.cloneStamps) {
            applyStamp(canvas, input, stamp, width, height)
        }

        return result
    }

    private fun applyStamp(
        canvas: Canvas,
        sourceBmp: Bitmap,
        stamp: EditOperation.CloneStampPoint,
        imgWidth: Float,
        imgHeight: Float
    ) {
        val srcPxX = (stamp.sourceX * imgWidth).toInt()
        val srcPxY = (stamp.sourceY * imgHeight).toInt()
        val targetPxX = stamp.targetX * imgWidth
        val targetPxY = stamp.targetY * imgHeight
        val radius = stamp.radius.coerceIn(10f, 200f)

        val diameter = (radius * 2).toInt()
        if (diameter <= 0) return

        val srcLeft = (srcPxX - radius.toInt()).coerceIn(0, sourceBmp.width - 1)
        val srcTop = (srcPxY - radius.toInt()).coerceIn(0, sourceBmp.height - 1)
        val srcRight = (srcPxX + radius.toInt()).coerceIn(srcLeft + 1, sourceBmp.width)
        val srcBottom = (srcPxY + radius.toInt()).coerceIn(srcTop + 1, sourceBmp.height)

        val srcRect = Rect(srcLeft, srcTop, srcRight, srcBottom)
        val w = srcRect.width()
        val h = srcRect.height()
        if (w <= 0 || h <= 0) return

        // Extraer parche de origen
        val patchBmp = Bitmap.createBitmap(sourceBmp, srcRect.left, srcRect.top, w, h)

        val hardness = stamp.hardness.coerceIn(0.05f, 1.0f)
        val opacity = stamp.opacity.coerceIn(0.01f, 1.0f)
        val flow = stamp.flow.coerceIn(0.01f, 1.0f)
        val combinedAlpha = (opacity * flow).coerceIn(0.01f, 1.0f)

        // Crear máscara circular con bordes difuminados según dureza (hardness)
        val maskedPatch = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val patchCanvas = Canvas(maskedPatch)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            if (hardness < 0.95f) {
                val blurRadius = (radius * (1.0f - hardness) * 0.5f).coerceAtLeast(1f)
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
        }
        val innerCircleRadius = (radius * hardness).coerceIn(4f, radius)
        patchCanvas.drawCircle(w / 2f, h / 2f, innerCircleRadius, maskPaint)

        val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        patchCanvas.drawBitmap(patchBmp, 0f, 0f, drawPaint)

        // Dibujar el parche en el destino aplicando opacidad y flujo
        val finalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (combinedAlpha * 255).toInt().coerceIn(1, 255)
        }
        canvas.drawBitmap(maskedPatch, targetPxX - w / 2f, targetPxY - h / 2f, finalPaint)
    }
}
