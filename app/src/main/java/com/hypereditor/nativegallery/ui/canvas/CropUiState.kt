package com.hypereditor.nativegallery.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.hypereditor.nativegallery.domain.model.CropAspectRatio
import com.hypereditor.nativegallery.domain.model.CropScaleMode
import com.hypereditor.nativegallery.domain.model.EditOperation

class CropUiState(
    initialScale: Float = 1.0f,
    initialPanXNorm: Float = 0f,
    initialPanYNorm: Float = 0f,
    initialRotation: Float = 0f,
    initialAspectRatio: CropAspectRatio = CropAspectRatio.ORIGINAL,
    initialShowGrid: Boolean = true,
    initialFlipH: Boolean = false,
    initialFlipV: Boolean = false,
    initialScaleMode: CropScaleMode = CropScaleMode.FIT
) {
    var scale by mutableFloatStateOf(initialScale)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var rotation by mutableFloatStateOf(initialRotation)
    var aspectRatio by mutableStateOf(initialAspectRatio)
    var showRuleOfThirds by mutableStateOf(initialShowGrid)
    var flipHorizontal by mutableStateOf(initialFlipH)
    var flipVertical by mutableStateOf(initialFlipV)
    var scaleMode by mutableStateOf(initialScaleMode)
    var isInteracting by mutableStateOf(false)

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
        scaleMode = CropScaleMode.FIT
    }

    fun onDoubleTap() {
        if (scale > 1.05f || Math.abs(offsetX) > 1f || Math.abs(offsetY) > 1f) {
            scale = 1.0f
            offsetX = 0f
            offsetY = 0f
        } else {
            scale = 1.75f
        }
    }

    /**
     * Updates transform with bounds clamping to avoid empty border areas.
     */
    fun updateTransformWithBounds(
        pan: Offset,
        zoomFactor: Float,
        frameWidth: Float,
        frameHeight: Float,
        imgWidth: Float,
        imgHeight: Float
    ) {
        val newScale = (scale * zoomFactor).coerceIn(1.0f, 6.0f)
        scale = newScale

        // Calculate maximum allowable pan so image edges cover the crop frame
        // When scale is 1.0, scaled image fits the frame, max pan is 0
        // When scale > 1.0, allowable pan is (scaledDim - frameDim) / 2
        val scaledW = frameWidth * scale
        val scaledH = frameHeight * scale
        val maxPanX = ((scaledW - frameWidth) / 2f).coerceAtLeast(0f)
        val maxPanY = ((scaledH - frameHeight) / 2f).coerceAtLeast(0f)

        offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
        offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
    }

    fun applyStraighten(angle: Float) {
        // Snapping: strong snap to 0° if within 0.8°
        val snappedAngle = if (Math.abs(angle) < 0.8f) 0f else angle
        val baseOrthogonal = ((rotation / 90f).toInt() * 90)
        rotation = baseOrthogonal.toFloat() + snappedAngle
    }

    fun setScaleModePreset(mode: CropScaleMode) {
        scaleMode = mode
        when (mode) {
            CropScaleMode.FIT -> {
                scale = 1.0f
                offsetX = 0f
                offsetY = 0f
            }
            CropScaleMode.FILL -> {
                scale = 1.35f
                offsetX = 0f
                offsetY = 0f
            }
            CropScaleMode.CENTER -> {
                scale = 1.0f
                offsetX = 0f
                offsetY = 0f
            }
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
            showRuleOfThirds = showRuleOfThirds,
            scaleMode = scaleMode
        )
    }

    fun syncFrom(transform: EditOperation.CropTransform, frameWidth: Float, frameHeight: Float) {
        scale = transform.scale
        rotation = transform.rotation90Degrees.toFloat() + transform.fineStraightenAngle
        aspectRatio = transform.aspectRatio
        showRuleOfThirds = transform.showRuleOfThirds
        flipHorizontal = transform.flipHorizontal
        flipVertical = transform.flipVertical
        scaleMode = transform.scaleMode
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
            initialFlipV = initialTransform.flipVertical,
            initialScaleMode = initialTransform.scaleMode
        )
    }
}
