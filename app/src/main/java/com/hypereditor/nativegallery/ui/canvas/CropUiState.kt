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

object CropBoundsCalculator {
    data class CropBounds(
        val maxPanX: Float,
        val maxPanY: Float,
        val renderedWidth: Float,
        val renderedHeight: Float,
        val baseScale: Float
    )

    fun computeCropBounds(
        imgWidth: Float,
        imgHeight: Float,
        frameWidth: Float,
        frameHeight: Float,
        scale: Float,
        rotation: Float = 0f
    ): CropBounds {
        if (imgWidth <= 0f || imgHeight <= 0f || frameWidth <= 0f || frameHeight <= 0f) {
            return CropBounds(0f, 0f, frameWidth.coerceAtLeast(0f), frameHeight.coerceAtLeast(0f), 1.0f)
        }

        // ContentScale.Crop base scale: image covers the crop frame aperture completely
        val baseScale = maxOf(frameWidth / imgWidth, frameHeight / imgHeight)
        val currentScale = scale.coerceIn(1.0f, 6.0f)

        // Account for 90° / 270° orthogonal rotation where dimensions swap
        val isOrthogonalSwapped = (Math.round(rotation / 90.0) % 2L != 0L)
        val renderedW = (if (isOrthogonalSwapped) imgHeight else imgWidth) * baseScale * currentScale
        val renderedH = (if (isOrthogonalSwapped) imgWidth else imgHeight) * baseScale * currentScale

        val maxPanX = ((renderedW - frameWidth) / 2f).coerceAtLeast(0f)
        val maxPanY = ((renderedH - frameHeight) / 2f).coerceAtLeast(0f)

        return CropBounds(
            maxPanX = maxPanX,
            maxPanY = maxPanY,
            renderedWidth = renderedW,
            renderedHeight = renderedH,
            baseScale = baseScale
        )
    }

    fun clampOffsetToBounds(
        offsetX: Float,
        offsetY: Float,
        bounds: CropBounds
    ): Offset {
        val clampedX = if (bounds.maxPanX <= 0f) 0f else offsetX.coerceIn(-bounds.maxPanX, bounds.maxPanX)
        val clampedY = if (bounds.maxPanY <= 0f) 0f else offsetY.coerceIn(-bounds.maxPanY, bounds.maxPanY)
        return Offset(clampedX, clampedY)
    }
}

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

        val bounds = CropBoundsCalculator.computeCropBounds(
            imgWidth = imgWidth,
            imgHeight = imgHeight,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            scale = scale,
            rotation = rotation
        )

        val clamped = CropBoundsCalculator.clampOffsetToBounds(
            offsetX = offsetX + pan.x,
            offsetY = offsetY + pan.y,
            bounds = bounds
        )
        offsetX = clamped.x
        offsetY = clamped.y
    }

    /**
     * Re-clamps existing offsets to the current frame bounds without adding new pan.
     */
    fun clampToBounds(
        frameWidth: Float,
        frameHeight: Float,
        imgWidth: Float,
        imgHeight: Float
    ) {
        if (frameWidth <= 0f || frameHeight <= 0f || imgWidth <= 0f || imgHeight <= 0f) return

        val bounds = CropBoundsCalculator.computeCropBounds(
            imgWidth = imgWidth,
            imgHeight = imgHeight,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            scale = scale,
            rotation = rotation
        )

        val clamped = CropBoundsCalculator.clampOffsetToBounds(
            offsetX = offsetX,
            offsetY = offsetY,
            bounds = bounds
        )
        offsetX = clamped.x
        offsetY = clamped.y
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

        val orthogonal90 = (((rotation / 90f).toInt() * 90) % 360 + 360) % 360

        return EditOperation.CropTransform(
            scale = scale,
            panXNorm = panXNorm,
            panYNorm = panYNorm,
            rotation90Degrees = orthogonal90,
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
