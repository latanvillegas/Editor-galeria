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

        // Crear máscara circular con bordes difuminados
        val maskedPatch = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val patchCanvas = Canvas(maskedPatch)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(radius * 0.25f, BlurMaskFilter.Blur.NORMAL)
        }
        patchCanvas.drawCircle(w / 2f, h / 2f, (radius * 0.85f).coerceAtLeast(4f), maskPaint)

        val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        patchCanvas.drawBitmap(patchBmp, 0f, 0f, drawPaint)

        // Dibujar el parche en el destino
        canvas.drawBitmap(maskedPatch, targetPxX - w / 2f, targetPxY - h / 2f, null)
    }
}
