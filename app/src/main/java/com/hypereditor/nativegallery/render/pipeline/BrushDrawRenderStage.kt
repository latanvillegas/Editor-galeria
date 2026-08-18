package com.hypereditor.nativegallery.render.pipeline

import android.graphics.*
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.EditorDocument

class BrushDrawRenderStage : RenderStage {
    override val name: String = "BrushDrawRenderStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        if (document.brushStrokes.isEmpty()) {
            return input
        }

        val result = input.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = input.width.toFloat()
        val height = input.height.toFloat()

        for (stroke in document.brushStrokes) {
            if (stroke.points.size < 2) continue

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = stroke.strokeWidth.coerceAtLeast(2f)
                if (stroke.isEraser) {
                    // Borrador sobre imagen
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                } else {
                    color = stroke.colorInt
                    alpha = (stroke.opacity.coerceIn(0f, 1f) * 255).toInt()
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

        return result
    }
}
