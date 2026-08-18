package com.hypereditor.nativegallery.render.pipeline

import android.graphics.*
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.EditorDocument

class TextOverlayRenderStage : RenderStage {
    override val name: String = "TextOverlayRenderStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        if (document.textOverlays.isEmpty()) {
            return input
        }

        val result = input.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = input.width.toFloat()
        val height = input.height.toFloat()

        for (textItem in document.textOverlays) {
            if (textItem.text.isBlank()) continue

            val typeface = when (textItem.fontFamilyName.uppercase()) {
                "SERIF" -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
                "MONOSPACE" -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                "CURSIVE" -> Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                else -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = textItem.textSize.coerceAtLeast(14f)
                color = textItem.colorInt
                alpha = (textItem.opacity.coerceIn(0f, 1f) * 255).toInt()
                textAlign = when (textItem.alignment) {
                    1 -> Paint.Align.CENTER
                    2 -> Paint.Align.RIGHT
                    else -> Paint.Align.LEFT
                }
                // Sombra sutil para legibilidad
                setShadowLayer(4f, 2f, 2f, Color.argb(180, 0, 0, 0))
            }

            val px = textItem.posX * width
            val py = textItem.posY * height

            val lines = textItem.text.split("\n")
            var currentY = py
            for (line in lines) {
                canvas.drawText(line, px, currentY, paint)
                currentY += paint.textSize * 1.25f
            }
        }

        return result
    }
}
