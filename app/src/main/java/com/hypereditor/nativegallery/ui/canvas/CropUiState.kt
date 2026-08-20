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
    initialScaleMode: CropScaleMode = CropScaleMode.FIT,
    initialCropLeftNorm: Float = 0f,
    initialCropTopNorm: Float = 0f,
    initialCropRightNorm: Float = 1f,
    initialCropBottomNorm: Float = 1f
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

    // Snapseed Crop Rect (normalized coordinates [0..1] relative to the displayed image)
    var cropLeftNorm by mutableFloatStateOf(initialCropLeftNorm.coerceIn(0f, 0.95f))
    var cropTopNorm by mutableFloatStateOf(initialCropTopNorm.coerceIn(0f, 0.95f))
    var cropRightNorm by mutableFloatStateOf(initialCropRightNorm.coerceIn(0.05f, 1f))
    var cropBottomNorm by mutableFloatStateOf(initialCropBottomNorm.coerceIn(0.05f, 1f))

    val isModified: Boolean
        get() = scale != 1.0f || offsetX != 0f || offsetY != 0f || rotation != 0f ||
                aspectRatio != CropAspectRatio.ORIGINAL || flipHorizontal || flipVertical ||
                cropLeftNorm > 0.001f || cropTopNorm > 0.001f || cropRightNorm < 0.999f || cropBottomNorm < 0.999f

    fun reset() {
        scale = 1.0f
        offsetX = 0f
        offsetY = 0f
        rotation = 0f
        aspectRatio = CropAspectRatio.ORIGINAL
        flipHorizontal = false
        flipVertical = false
        scaleMode = CropScaleMode.FIT
        cropLeftNorm = 0f
        cropTopNorm = 0f
        cropRightNorm = 1f
        cropBottomNorm = 1f
    }

    /**
     * Resets the crop rectangle to fill the image respecting the current aspect ratio.
     */
    fun resetCropRectForRatio(imgWidth: Float, imgHeight: Float) {
        if (imgWidth <= 0f || imgHeight <= 0f) {
            cropLeftNorm = 0f
            cropTopNorm = 0f
            cropRightNorm = 1f
            cropBottomNorm = 1f
            return
        }

        val imgRatio = imgWidth / imgHeight
        val targetRatio = when (aspectRatio) {
            CropAspectRatio.ORIGINAL -> imgRatio
            CropAspectRatio.FREE -> null
            else -> aspectRatio.getCalculatedRatio(imgWidth.toInt(), imgHeight.toInt())
        }

        if (targetRatio == null) {
            // Free mode defaults to full image initially
            cropLeftNorm = 0f
            cropTopNorm = 0f
            cropRightNorm = 1f
            cropBottomNorm = 1f
        } else {
            // Center a box with targetRatio inside the [0, 1] normalized image space
            if (targetRatio > imgRatio) {
                // Target is wider than image: constrained by width
                val normH = (imgRatio / targetRatio).coerceIn(0.05f, 1f)
                val top = (1f - normH) / 2f
                cropLeftNorm = 0f
                cropRightNorm = 1f
                cropTopNorm = top
                cropBottomNorm = top + normH
            } else {
                // Target is taller than image: constrained by height
                val normW = (targetRatio / imgRatio).coerceIn(0.05f, 1f)
                val left = (1f - normW) / 2f
                cropTopNorm = 0f
                cropBottomNorm = 1f
                cropLeftNorm = left
                cropRightNorm = left + normW
            }
        }
    }

    /**
     * Translates the entire crop selection rectangle by delta in normalized units.
     */
    fun moveCropRect(deltaNormX: Float, deltaNormY: Float) {
        val width = cropRightNorm - cropLeftNorm
        val height = cropBottomNorm - cropTopNorm

        var newLeft = cropLeftNorm + deltaNormX
        var newTop = cropTopNorm + deltaNormY

        if (newLeft < 0f) newLeft = 0f
        if (newLeft + width > 1f) newLeft = 1f - width
        if (newTop < 0f) newTop = 0f
        if (newTop + height > 1f) newTop = 1f - height

        cropLeftNorm = newLeft
        cropRightNorm = newLeft + width
        cropTopNorm = newTop
        cropBottomNorm = newTop + height
    }

    /**
     * Resizes the crop rectangle from a specific handle/edge (Snapseed interaction).
     * handle: 0=TopLeft, 1=TopRight, 2=BottomLeft, 3=BottomRight, 4=Top, 5=Bottom, 6=Left, 7=Right
     */
    fun resizeCropRect(
        handle: Int,
        deltaNormX: Float,
        deltaNormY: Float,
        imgWidth: Float,
        imgHeight: Float
    ) {
        val minNormSize = 0.05f
        val imgRatio = if (imgHeight > 0f) imgWidth / imgHeight else 1f
        val fixedRatio = when (aspectRatio) {
            CropAspectRatio.ORIGINAL -> imgRatio
            CropAspectRatio.FREE -> null
            else -> aspectRatio.getCalculatedRatio(imgWidth.toInt(), imgHeight.toInt())
        }

        var newLeft = cropLeftNorm
        var newTop = cropTopNorm
        var newRight = cropRightNorm
        var newBottom = cropBottomNorm

        if (fixedRatio == null) {
            // Free resizing (Snapseed libre)
            when (handle) {
                0 -> { // Top-Left
                    newLeft = (cropLeftNorm + deltaNormX).coerceIn(0f, newRight - minNormSize)
                    newTop = (cropTopNorm + deltaNormY).coerceIn(0f, newBottom - minNormSize)
                }
                1 -> { // Top-Right
                    newRight = (cropRightNorm + deltaNormX).coerceIn(newLeft + minNormSize, 1f)
                    newTop = (cropTopNorm + deltaNormY).coerceIn(0f, newBottom - minNormSize)
                }
                2 -> { // Bottom-Left
                    newLeft = (cropLeftNorm + deltaNormX).coerceIn(0f, newRight - minNormSize)
                    newBottom = (cropBottomNorm + deltaNormY).coerceIn(newTop + minNormSize, 1f)
                }
                3 -> { // Bottom-Right
                    newRight = (cropRightNorm + deltaNormX).coerceIn(newLeft + minNormSize, 1f)
                    newBottom = (cropBottomNorm + deltaNormY).coerceIn(newTop + minNormSize, 1f)
                }
                4 -> { // Top Edge
                    newTop = (cropTopNorm + deltaNormY).coerceIn(0f, newBottom - minNormSize)
                }
                5 -> { // Bottom Edge
                    newBottom = (cropBottomNorm + deltaNormY).coerceIn(newTop + minNormSize, 1f)
                }
                6 -> { // Left Edge
                    newLeft = (cropLeftNorm + deltaNormX).coerceIn(0f, newRight - minNormSize)
                }
                7 -> { // Right Edge
                    newRight = (cropRightNorm + deltaNormX).coerceIn(newLeft + minNormSize, 1f)
                }
            }
        } else {
            // Ratio-locked resizing
            // fixedRatio = (pixelW) / (pixelH) = (normW * imgW) / (normH * imgH) => normW / normH = fixedRatio / imgRatio
            val normAspect = fixedRatio / imgRatio

            when (handle) {
                0 -> { // Top-Left
                    var delta = if (Math.abs(deltaNormX) > Math.abs(deltaNormY)) deltaNormX else deltaNormY * normAspect
                    newLeft = (cropLeftNorm + delta).coerceIn(0f, newRight - minNormSize)
                    val width = newRight - newLeft
                    val height = width / normAspect
                    newTop = (newBottom - height).coerceIn(0f, newBottom - minNormSize)
                    newLeft = newRight - (newBottom - newTop) * normAspect
                }
                1 -> { // Top-Right
                    val delta = if (Math.abs(deltaNormX) > Math.abs(deltaNormY)) deltaNormX else -deltaNormY * normAspect
                    newRight = (cropRightNorm + delta).coerceIn(newLeft + minNormSize, 1f)
                    val width = newRight - newLeft
                    val height = width / normAspect
                    newTop = (newBottom - height).coerceIn(0f, newBottom - minNormSize)
                    newRight = newLeft + (newBottom - newTop) * normAspect
                }
                2 -> { // Bottom-Left
                    val delta = if (Math.abs(deltaNormX) > Math.abs(deltaNormY)) deltaNormX else -deltaNormY * normAspect
                    newLeft = (cropLeftNorm + delta).coerceIn(0f, newRight - minNormSize)
                    val width = newRight - newLeft
                    val height = width / normAspect
                    newBottom = (newTop + height).coerceIn(newTop + minNormSize, 1f)
                    newLeft = newRight - (newBottom - newTop) * normAspect
                }
                3 -> { // Bottom-Right
                    val delta = if (Math.abs(deltaNormX) > Math.abs(deltaNormY)) deltaNormX else deltaNormY * normAspect
                    newRight = (cropRightNorm + delta).coerceIn(newLeft + minNormSize, 1f)
                    val width = newRight - newLeft
                    val height = width / normAspect
                    newBottom = (newTop + height).coerceIn(newTop + minNormSize, 1f)
                    newRight = newLeft + (newBottom - newTop) * normAspect
                }
                4, 5, 6, 7 -> {
                    // For edges in fixed-ratio, adjust symmetrically according to dominant drag
                    val delta = if (handle == 6 || handle == 7) deltaNormX else deltaNormY * normAspect
                    if (handle == 6) {
                        newLeft = (cropLeftNorm + delta).coerceIn(0f, newRight - minNormSize)
                        val h = (newRight - newLeft) / normAspect
                        newBottom = (newTop + h).coerceIn(newTop + minNormSize, 1f)
                    } else if (handle == 7) {
                        newRight = (cropRightNorm + delta).coerceIn(newLeft + minNormSize, 1f)
                        val h = (newRight - newLeft) / normAspect
                        newBottom = (newTop + h).coerceIn(newTop + minNormSize, 1f)
                    } else if (handle == 4) {
                        newTop = (cropTopNorm + deltaNormY).coerceIn(0f, newBottom - minNormSize)
                        val w = (newBottom - newTop) * normAspect
                        newRight = (newLeft + w).coerceIn(newLeft + minNormSize, 1f)
                    } else {
                        newBottom = (cropBottomNorm + deltaNormY).coerceIn(newTop + minNormSize, 1f)
                        val w = (newBottom - newTop) * normAspect
                        newRight = (newLeft + w).coerceIn(newLeft + minNormSize, 1f)
                    }
                }
            }
        }

        cropLeftNorm = newLeft.coerceIn(0f, 1f)
        cropTopNorm = newTop.coerceIn(0f, 1f)
        cropRightNorm = newRight.coerceIn(0f, 1f)
        cropBottomNorm = newBottom.coerceIn(0f, 1f)
    }

    fun onDoubleTap() {
        if (cropLeftNorm > 0.01f || cropTopNorm > 0.01f || cropRightNorm < 0.99f || cropBottomNorm < 0.99f || scale > 1.05f) {
            cropLeftNorm = 0f
            cropTopNorm = 0f
            cropRightNorm = 1f
            cropBottomNorm = 1f
            scale = 1.0f
            offsetX = 0f
            offsetY = 0f
        } else {
            cropLeftNorm = 0.1f
            cropTopNorm = 0.1f
            cropRightNorm = 0.9f
            cropBottomNorm = 0.9f
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
            cropLeftNorm = cropLeftNorm,
            cropTopNorm = cropTopNorm,
            cropRightNorm = cropRightNorm,
            cropBottomNorm = cropBottomNorm,
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
        cropLeftNorm = transform.cropLeftNorm
        cropTopNorm = transform.cropTopNorm
        cropRightNorm = transform.cropRightNorm
        cropBottomNorm = transform.cropBottomNorm
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
            initialScaleMode = initialTransform.scaleMode,
            initialCropLeftNorm = initialTransform.cropLeftNorm,
            initialCropTopNorm = initialTransform.cropTopNorm,
            initialCropRightNorm = initialTransform.cropRightNorm,
            initialCropBottomNorm = initialTransform.cropBottomNorm
        )
    }
}
