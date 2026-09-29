package com.hypereditor.nativegallery.render.pipeline

import android.graphics.*
import com.hypereditor.nativegallery.domain.model.EditorDocument

class BrushDrawRenderStage : RenderStage {
    override val name: String = "BrushDrawRenderStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        if (document.brushStrokes.isEmpty()) return input
        val result = input.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = input.width.toFloat()
        val height = input.height.toFloat()
        val scaleFactor = minOf(width, height) / 1000f

        for (stroke in document.brushStrokes) {
            if (stroke.points.size < 2) continue
            val strokeWidthPx = (stroke.strokeWidth * scaleFactor).coerceAtLeast(2f)
            val hardness = stroke.hardness.coerceIn(0f, 1f)
            val flow = stroke.flow.coerceIn(0.05f, 1f)
            val effectiveAlpha = (stroke.opacity.coerceIn(0f, 1f) * flow).coerceIn(0f, 1f)
            val path = Path().apply {
                val first = stroke.points.first(); moveTo(first.first * width, first.second * height)
                for (i in 1 until stroke.points.size) { val pt = stroke.points[i]; lineTo(pt.first * width, pt.second * height) }
            }

            fun paint(strokeWidth: Float, alpha: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; this.strokeWidth = strokeWidth
                if (stroke.isEraser) { this.alpha = (alpha * 255).toInt(); xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
                else { color = stroke.colorInt; this.alpha = (alpha * 255).toInt() }
            }

            if (hardness < 0.98f) {
                val softness = 1f - hardness
                canvas.drawPath(path, paint(strokeWidthPx * (1.35f + softness * 0.65f), effectiveAlpha * softness * 0.18f))
                canvas.drawPath(path, paint(strokeWidthPx * (1.12f + softness * 0.28f), effectiveAlpha * softness * 0.24f))
            }
            val coreAlpha = effectiveAlpha * (0.22f + hardness * 0.78f)
            canvas.drawPath(path, paint(strokeWidthPx, coreAlpha))
        }
        return result
    }
}
