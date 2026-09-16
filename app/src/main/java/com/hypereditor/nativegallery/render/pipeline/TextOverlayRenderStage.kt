package com.hypereditor.nativegallery.render.pipeline

import android.graphics.*
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.EditorDocument
import java.io.File

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
        val minDim = minOf(width, height)
        val scaleFactor = minDim / 1000f

        for (textItem in document.textOverlays) {
            if (textItem.text.isBlank()) continue

            val typeface = if (!textItem.customFontPath.isNullOrBlank()) {
                try {
                    val fontFile = File(textItem.customFontPath)
                    if (fontFile.exists() && fontFile.canRead()) {
                        Typeface.createFromFile(fontFile) ?: getSystemTypeface(textItem.fontFamilyName)
                    } else {
                        getSystemTypeface(textItem.fontFamilyName)
                    }
                } catch (_: Exception) {
                    getSystemTypeface(textItem.fontFamilyName)
                }
            } else {
                getSystemTypeface(textItem.fontFamilyName)
            }

            val effectiveSize = (textItem.textSize * textItem.scale * scaleFactor).coerceAtLeast(10f)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = effectiveSize
                color = textItem.colorInt
                alpha = (textItem.opacity.coerceIn(0f, 1f) * 255).toInt()
                textAlign = Paint.Align.CENTER
                setShadowLayer(effectiveSize * 0.08f, 2f * scaleFactor, 2f * scaleFactor, Color.argb(180, 0, 0, 0))
            }

            val cx = textItem.posX * width
            val cy = textItem.posY * height

            canvas.save()
            if (textItem.rotationDegrees != 0f) {
                canvas.rotate(textItem.rotationDegrees, cx, cy)
            }

            val lines = textItem.text.split("\n")
            val lineHeight = paint.fontSpacing
            val totalHeight = lines.size * lineHeight

            var currentY = cy - (totalHeight / 2f) + (effectiveSize * 0.8f)
            for (line in lines) {
                canvas.drawText(line, cx, currentY, paint)
                currentY += lineHeight
            }
            canvas.restore()
        }

        return result
    }

    private fun getSystemTypeface(fontName: String): Typeface {
        return when (fontName.uppercase()) {
            "SERIF" -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            "MONOSPACE" -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            "CURSIVE" -> Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            else -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }
}

