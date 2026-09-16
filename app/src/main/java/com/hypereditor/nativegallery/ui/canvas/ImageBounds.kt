package com.hypereditor.nativegallery.ui.canvas

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntSize

data class ImageBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

fun computeImageBounds(canvasSize: IntSize, bitmap: Bitmap?): ImageBounds {
    if (bitmap == null || canvasSize.width <= 0 || canvasSize.height <= 0) {
        return ImageBounds(0f, 0f, 0f, 0f)
    }
    val imgW = bitmap.width.toFloat()
    val imgH = bitmap.height.toFloat()
    if (imgW <= 0f || imgH <= 0f) {
        return ImageBounds(0f, 0f, 0f, 0f)
    }
    val imgRatio = imgW / imgH
    val canvasRatio = canvasSize.width.toFloat() / canvasSize.height.toFloat()

    val displayW: Float
    val displayH: Float
    if (canvasRatio > imgRatio) {
        displayH = canvasSize.height.toFloat()
        displayW = displayH * imgRatio
    } else {
        displayW = canvasSize.width.toFloat()
        displayH = displayW / imgRatio
    }
    val left = (canvasSize.width.toFloat() - displayW) / 2f
    val top = (canvasSize.height.toFloat() - displayH) / 2f
    return ImageBounds(left, top, displayW, displayH)
}
