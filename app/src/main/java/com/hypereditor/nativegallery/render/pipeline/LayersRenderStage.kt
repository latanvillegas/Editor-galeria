package com.hypereditor.nativegallery.render.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.hypereditor.nativegallery.domain.model.EditorDocument
import com.hypereditor.nativegallery.domain.model.LayerModel
import com.hypereditor.nativegallery.domain.model.LayerType
import com.hypereditor.nativegallery.render.BlendModeMapper

class LayersRenderStage : RenderStage {
    override val name: String = "LayersRenderStage"

    override fun process(input: Bitmap, document: EditorDocument): Bitmap {
        val visibleLayers = document.layers.filter { it.isVisible && it.opacity > 0.001f }
        if (visibleLayers.isEmpty()) {
            return input
        }

        val result = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw base image first
        canvas.drawBitmap(input, 0f, 0f, null)

        // Draw each layer on top with its blend mode and opacity
        for (layer in visibleLayers) {
            renderSingleLayer(canvas, layer, input)
        }

        return result
    }

    private fun renderSingleLayer(canvas: Canvas, layer: LayerModel, baseInput: Bitmap) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
            xfermode = BlendModeMapper.toPorterDuffXfermode(layer.blendMode)
        }

        when (layer.layerType) {
            LayerType.COLOR_FILL -> {
                paint.color = layer.colorHex.toInt()
                paint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
            }
            LayerType.IMAGE_DUPLICATE -> {
                val bmp = layer.bitmap ?: baseInput
                val matrix = Matrix().apply {
                    postScale(layer.scale, layer.scale, bmp.width / 2f, bmp.height / 2f)
                    postRotate(layer.rotationDegrees, bmp.width / 2f, bmp.height / 2f)
                    postTranslate(layer.offsetX, layer.offsetY)
                }
                canvas.drawBitmap(bmp, matrix, paint)
            }
        }
    }
}
