package com.hypereditor.nativegallery.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.hypereditor.nativegallery.domain.model.CropAspectRatio
import com.hypereditor.nativegallery.domain.model.EditOperation

class CropUiState(
    initialScale: Float = 1.0f,
    initialPanXNorm: Float = 0f,
    initialPanYNorm: Float = 0f,
    initialRotation: Float = 0f,
    initialAspectRatio: CropAspectRatio = CropAspectRatio.ORIGINAL,
    initialShowGrid: Boolean = true,
    initialFlipH: Boolean = false,
    initialFlipV: Boolean = false
) {
    var scale by mutableFloatStateOf(initialScale)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var rotation by mutableFloatStateOf(initialRotation)
    var aspectRatio by mutableStateOf(initialAspectRatio)
    var showRuleOfThirds by mutableStateOf(initialShowGrid)
    var flipHorizontal by mutableStateOf(initialFlipH)
    var flipVertical by mutableStateOf(initialFlipV)

    val isModified: Boolean
        get() = scale != 1.0f || offsetX != 0f || offsetY != 0f || rotation != 0f ||
                aspectRatio != CropAspectRatio.ORIGINAL || flipHorizontal || flipVertical

    fun reset() {
        scale = 1.0f
        offsetX = 0f
        offsetY = 0f
        rotation = 0f
        aspectRatio = CropAspectRatio.ORIGINAL
        flipHorizontal = false
        flipVertical = false
    }

    fun onDoubleTap() {
        if (scale > 1.1f || offsetX != 0f || offsetY != 0f) {
            scale = 1.0f
            offsetX = 0f
            offsetY = 0f
        } else {
            scale = 2.0f
        }
    }

    fun updateTransform(pan: Offset, zoomFactor: Float, rotationDelta: Float = 0f) {
        val newScale = (scale * zoomFactor).coerceIn(1.0f, 8.0f)
        scale = newScale
        offsetX += pan.x
        offsetY += pan.y
        if (rotationDelta != 0f) {
            rotation = (rotation + rotationDelta) % 360f
        }
    }

    fun toCropTransform(frameWidth: Float, frameHeight: Float): EditOperation.CropTransform {
        val safeW = frameWidth.coerceAtLeast(1f)
        val safeH = frameHeight.coerceAtLeast(1f)
        val panXNorm = (offsetX / safeW).coerceIn(-2f, 2f)
        val panYNorm = (offsetY / safeH).coerceIn(-2f, 2f)

        return EditOperation.CropTransform(
            scale = scale,
            panXNorm = panXNorm,
            panYNorm = panYNorm,
            rotation90Degrees = ((rotation / 90f).toInt() * 90) % 360,
            fineStraightenAngle = rotation % 90f,
            flipHorizontal = flipHorizontal,
            flipVertical = flipVertical,
            aspectRatio = aspectRatio,
            showRuleOfThirds = showRuleOfThirds
        )
    }

    fun syncFrom(transform: EditOperation.CropTransform, frameWidth: Float, frameHeight: Float) {
        scale = transform.scale
        rotation = transform.rotation90Degrees.toFloat() + transform.fineStraightenAngle
        aspectRatio = transform.aspectRatio
        showRuleOfThirds = transform.showRuleOfThirds
        flipHorizontal = transform.flipHorizontal
        flipVertical = transform.flipVertical
        if (frameWidth > 0 && frameHeight > 0) {
            offsetX = transform.panXNorm * frameWidth
            offsetY = transform.panYNorm * frameHeight
        }
    }
}

@Composable
fun rememberCropUiState(
    initialTransform: EditOperation.CropTransform = EditOperation.CropTransform()
): CropUiState {
    return remember {
        CropUiState(
            initialScale = initialTransform.scale,
            initialPanXNorm = initialTransform.panXNorm,
            initialPanYNorm = initialTransform.panYNorm,
            initialRotation = initialTransform.rotation90Degrees.toFloat() + initialTransform.fineStraightenAngle,
            initialAspectRatio = initialTransform.aspectRatio,
            initialShowGrid = initialTransform.showRuleOfThirds,
            initialFlipH = initialTransform.flipHorizontal,
            initialFlipV = initialTransform.flipVertical
        )
    }
}
