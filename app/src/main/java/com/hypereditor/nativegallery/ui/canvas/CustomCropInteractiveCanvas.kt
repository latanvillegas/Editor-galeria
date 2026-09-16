package com.hypereditor.nativegallery.ui.canvas

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.EditOperation
import java.util.Locale

/**
 * State holder for the "Recorte personalizado" (Freeform Custom Crop) tool.
 * Manages normalized crop rectangle coordinates [0..1] relative to the displayed image,
 * ensuring complete immunity to screen resolution, viewport zooming, and panning.
 */
class CustomCropState(
    initialLeftNorm: Float = 0f,
    initialTopNorm: Float = 0f,
    initialRightNorm: Float = 1f,
    initialBottomNorm: Float = 1f,
    initialLockRatio: Boolean = false,
    initialShowGrid: Boolean = true
) {
    var cropLeftNorm by mutableFloatStateOf(initialLeftNorm.coerceIn(0f, 0.95f))
    var cropTopNorm by mutableFloatStateOf(initialTopNorm.coerceIn(0f, 0.95f))
    var cropRightNorm by mutableFloatStateOf(initialRightNorm.coerceIn(0.05f, 1f))
    var cropBottomNorm by mutableFloatStateOf(initialBottomNorm.coerceIn(0.05f, 1f))

    var isAspectRatioLocked by mutableStateOf(initialLockRatio)
    var lockedRatio by mutableFloatStateOf(1f)
    var showGrid by mutableStateOf(initialShowGrid)
    var activeHandle by mutableIntStateOf(-1)

    // Viewport zoom & pan for precision corner framing
    var zoomScale by mutableFloatStateOf(1.0f)
    var panOffsetX by mutableFloatStateOf(0f)
    var panOffsetY by mutableFloatStateOf(0f)

    val isModified: Boolean
        get() = cropLeftNorm > 0.001f || cropTopNorm > 0.001f ||
                cropRightNorm < 0.999f || cropBottomNorm < 0.999f ||
                isAspectRatioLocked || zoomScale != 1.0f

    fun resetToFull() {
        cropLeftNorm = 0f
        cropTopNorm = 0f
        cropRightNorm = 1f
        cropBottomNorm = 1f
        zoomScale = 1.0f
        panOffsetX = 0f
        panOffsetY = 0f
    }

    fun toggleLockAspectRatio(imgW: Float, imgH: Float) {
        if (!isAspectRatioLocked) {
            val curPixelW = (cropRightNorm - cropLeftNorm) * imgW
            val curPixelH = (cropBottomNorm - cropTopNorm) * imgH
            lockedRatio = if (curPixelH > 0f) curPixelW / curPixelH else 1f
            isAspectRatioLocked = true
        } else {
            isAspectRatioLocked = false
        }
    }

    fun syncFrom(transform: EditOperation.CropTransform) {
        cropLeftNorm = transform.cropLeftNorm.coerceIn(0f, 0.95f)
        cropTopNorm = transform.cropTopNorm.coerceIn(0f, 0.95f)
        cropRightNorm = transform.cropRightNorm.coerceIn(0.05f, 1f)
        cropBottomNorm = transform.cropBottomNorm.coerceIn(0.05f, 1f)
    }

    /**
     * Resizes or moves the crop rectangle in normalized coordinates [0..1].
     * Handles:
     * 0: Top-Left corner
     * 1: Top-Right corner
     * 2: Bottom-Left corner
     * 3: Bottom-Right corner
     * 4: Top edge
     * 5: Bottom edge
     * 6: Left edge
     * 7: Right edge
     * 8: Inside body (move entire selection)
     */
    fun onDrag(
        handle: Int,
        deltaNormX: Float,
        deltaNormY: Float,
        imgW: Float,
        imgH: Float,
        displayW: Float,
        displayH: Float
    ) {
        val minPixel = 44f
        val minNormW = (minPixel / displayW.coerceAtLeast(100f)).coerceIn(0.02f, 0.4f)
        val minNormH = (minPixel / displayH.coerceAtLeast(100f)).coerceIn(0.02f, 0.4f)

        if (handle == 8) {
            // Interior translation (move entire selection while keeping size constant)
            val curW = cropRightNorm - cropLeftNorm
            val curH = cropBottomNorm - cropTopNorm

            var newL = cropLeftNorm + deltaNormX
            var newT = cropTopNorm + deltaNormY

            if (newL < 0f) newL = 0f
            if (newL + curW > 1f) newL = 1f - curW
            if (newT < 0f) newT = 0f
            if (newT + curH > 1f) newT = 1f - curH

            cropLeftNorm = newL
            cropRightNorm = newL + curW
            cropTopNorm = newT
            cropBottomNorm = newT + curH
            return
        }

        if (!isAspectRatioLocked) {
            // Mode A: Freeform Crop (independent width and height)
            when (handle) {
                0 -> { // Top-Left: modify left and top
                    cropLeftNorm = (cropLeftNorm + deltaNormX).coerceIn(0f, cropRightNorm - minNormW)
                    cropTopNorm = (cropTopNorm + deltaNormY).coerceIn(0f, cropBottomNorm - minNormH)
                }
                1 -> { // Top-Right: modify right and top
                    cropRightNorm = (cropRightNorm + deltaNormX).coerceIn(cropLeftNorm + minNormW, 1f)
                    cropTopNorm = (cropTopNorm + deltaNormY).coerceIn(0f, cropBottomNorm - minNormH)
                }
                2 -> { // Bottom-Left: modify left and bottom
                    cropLeftNorm = (cropLeftNorm + deltaNormX).coerceIn(0f, cropRightNorm - minNormW)
                    cropBottomNorm = (cropBottomNorm + deltaNormY).coerceIn(cropTopNorm + minNormH, 1f)
                }
                3 -> { // Bottom-Right: modify right and bottom
                    cropRightNorm = (cropRightNorm + deltaNormX).coerceIn(cropLeftNorm + minNormW, 1f)
                    cropBottomNorm = (cropBottomNorm + deltaNormY).coerceIn(cropTopNorm + minNormH, 1f)
                }
                4 -> { // Top edge: modify height only (top boundary)
                    cropTopNorm = (cropTopNorm + deltaNormY).coerceIn(0f, cropBottomNorm - minNormH)
                }
                5 -> { // Bottom edge: modify height only (bottom boundary)
                    cropBottomNorm = (cropBottomNorm + deltaNormY).coerceIn(cropTopNorm + minNormH, 1f)
                }
                6 -> { // Left edge: modify width only (left boundary)
                    cropLeftNorm = (cropLeftNorm + deltaNormX).coerceIn(0f, cropRightNorm - minNormW)
                }
                7 -> { // Right edge: modify width only (right boundary)
                    cropRightNorm = (cropRightNorm + deltaNormX).coerceIn(cropLeftNorm + minNormW, 1f)
                }
            }
        } else {
            // Mode B: Locked Aspect Ratio
            val imgRatio = if (imgH > 0f) imgW / imgH else 1f
            val normAspect = (lockedRatio / imgRatio).coerceIn(0.1f, 10f)

            when (handle) {
                0 -> { // Top-Left (anchor BR)
                    val curW = cropRightNorm - cropLeftNorm
                    val delta = if (Math.abs(deltaNormX) > Math.abs(deltaNormY)) deltaNormX else deltaNormY * normAspect
                    var newW = (curW - delta).coerceAtLeast(minNormW)
                    var newH = newW / normAspect
                    if (cropRightNorm - newW < 0f) {
                        newW = cropRightNorm
                        newH = newW / normAspect
                    }
                    if (cropBottomNorm - newH < 0f) {
                        newH = cropBottomNorm
                        newW = newH * normAspect
                    }
                    cropLeftNorm = (cropRightNorm - newW).coerceIn(0f, cropRightNorm - minNormW)
                    cropTopNorm = (cropBottomNorm - newH).coerceIn(0f, cropBottomNorm - minNormH)
                }
                1 -> { // Top-Right (anchor BL)
                    val curW = cropRightNorm - cropLeftNorm
                    val delta = if (Math.abs(deltaNormX) > Math.abs(deltaNormY)) deltaNormX else -deltaNormY * normAspect
                    var newW = (curW + delta).coerceAtLeast(minNormW)
                    var newH = newW / normAspect
                    if (cropLeftNorm + newW > 1f) {
                        newW = 1f - cropLeftNorm
                        newH = newW / normAspect
                    }
                    if (cropBottomNorm - newH < 0f) {
                        newH = cropBottomNorm
                        newW = newH * normAspect
                    }
                    cropRightNorm = (cropLeftNorm + newW).coerceIn(cropLeftNorm + minNormW, 1f)
                    cropTopNorm = (cropBottomNorm - newH).coerceIn(0f, cropBottomNorm - minNormH)
                }
                2 -> { // Bottom-Left (anchor TR)
                    val curW = cropRightNorm - cropLeftNorm
                    val delta = if (Math.abs(deltaNormX) > Math.abs(deltaNormY)) deltaNormX else -deltaNormY * normAspect
                    var newW = (curW - delta).coerceAtLeast(minNormW)
                    var newH = newW / normAspect
                    if (cropRightNorm - newW < 0f) {
                        newW = cropRightNorm
                        newH = newW / normAspect
                    }
                    if (cropTopNorm + newH > 1f) {
                        newH = 1f - cropTopNorm
                        newW = newH * normAspect
                    }
                    cropLeftNorm = (cropRightNorm - newW).coerceIn(0f, cropRightNorm - minNormW)
                    cropBottomNorm = (cropTopNorm + newH).coerceIn(cropTopNorm + minNormH, 1f)
                }
                3 -> { // Bottom-Right (anchor TL)
                    val curW = cropRightNorm - cropLeftNorm
                    val delta = if (Math.abs(deltaNormX) > Math.abs(deltaNormY)) deltaNormX else deltaNormY * normAspect
                    var newW = (curW + delta).coerceAtLeast(minNormW)
                    var newH = newW / normAspect
                    if (cropLeftNorm + newW > 1f) {
                        newW = 1f - cropLeftNorm
                        newH = newW / normAspect
                    }
                    if (cropTopNorm + newH > 1f) {
                        newH = 1f - cropTopNorm
                        newW = newH * normAspect
                    }
                    cropRightNorm = (cropLeftNorm + newW).coerceIn(cropLeftNorm + minNormW, 1f)
                    cropBottomNorm = (cropTopNorm + newH).coerceIn(cropTopNorm + minNormH, 1f)
                }
                4, 5 -> { // Top or Bottom Edge
                    val delta = if (handle == 4) -deltaNormY else deltaNormY
                    val curH = cropBottomNorm - cropTopNorm
                    var newH = (curH + delta).coerceAtLeast(minNormH)
                    var newW = newH * normAspect
                    if (newW > 1f) {
                        newW = 1f
                        newH = newW / normAspect
                    }
                    val centerX = (cropLeftNorm + cropRightNorm) / 2f
                    var newL = centerX - newW / 2f
                    var newR = centerX + newW / 2f
                    if (newL < 0f) { newL = 0f; newR = newW }
                    if (newR > 1f) { newR = 1f; newL = 1f - newW }
                    if (handle == 4) {
                        cropTopNorm = (cropBottomNorm - newH).coerceIn(0f, cropBottomNorm - minNormH)
                    } else {
                        cropBottomNorm = (cropTopNorm + newH).coerceIn(cropTopNorm + minNormH, 1f)
                    }
                    cropLeftNorm = newL.coerceIn(0f, 1f)
                    cropRightNorm = newR.coerceIn(0f, 1f)
                }
                6, 7 -> { // Left or Right Edge
                    val delta = if (handle == 6) -deltaNormX else deltaNormX
                    val curW = cropRightNorm - cropLeftNorm
                    var newW = (curW + delta).coerceAtLeast(minNormW)
                    var newH = newW / normAspect
                    if (newH > 1f) {
                        newH = 1f
                        newW = newH * normAspect
                    }
                    val centerY = (cropTopNorm + cropBottomNorm) / 2f
                    var newT = centerY - newH / 2f
                    var newB = centerY + newH / 2f
                    if (newT < 0f) { newT = 0f; newB = newH }
                    if (newB > 1f) { newB = 1f; newT = 1f - newH }
                    if (handle == 6) {
                        cropLeftNorm = (cropRightNorm - newW).coerceIn(0f, cropRightNorm - minNormW)
                    } else {
                        cropRightNorm = (cropLeftNorm + newW).coerceIn(cropLeftNorm + minNormW, 1f)
                    }
                    cropTopNorm = newT.coerceIn(0f, 1f)
                    cropBottomNorm = newB.coerceIn(0f, 1f)
                }
            }
        }
    }
}

@Composable
fun rememberCustomCropState(
    initialTransform: EditOperation.CropTransform? = null
): CustomCropState {
    return remember {
        CustomCropState(
            initialLeftNorm = initialTransform?.cropLeftNorm ?: 0f,
            initialTopNorm = initialTransform?.cropTopNorm ?: 0f,
            initialRightNorm = initialTransform?.cropRightNorm ?: 1f,
            initialBottomNorm = initialTransform?.cropBottomNorm ?: 1f
        )
    }
}

/**
 * Interactive canvas for "Recorte personalizado" (Freeform Custom Crop).
 * Renders the full image, darkened outer scrim, high-contrast editable crop frame,
 * 4 corner bracket handles, 4 side midpoint handles, rule-of-thirds grid,
 * and HUD/action buttons (Cancelar, Restablecer, Aplicar, Bloquear Proporción).
 */
@Composable
fun CustomCropInteractiveCanvas(
    bitmap: Bitmap?,
    rotation: Float = 0f,
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false,
    cropState: CustomCropState,
    onApply: (cropLeftNorm: Float, cropTopNorm: Float, cropRightNorm: Float, cropBottomNorm: Float) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Pre-orient the bitmap to match the document's active rotation and flip
    val orientedBitmap = remember(bitmap, rotation, flipHorizontal, flipVertical) {
        if (bitmap == null) return@remember null
        val totalRotation = (rotation % 360f + 360f) % 360f
        val hasRotation = Math.abs(totalRotation) > 0.01f
        if (!hasRotation && !flipHorizontal && !flipVertical) {
            bitmap
        } else {
            val matrix = Matrix()
            if (flipHorizontal || flipVertical) {
                val sx = if (flipHorizontal) -1f else 1f
                val sy = if (flipVertical) -1f else 1f
                matrix.postScale(sx, sy, bitmap.width / 2f, bitmap.height / 2f)
            }
            if (hasRotation) {
                matrix.postRotate(totalRotation, bitmap.width / 2f, bitmap.height / 2f)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (orientedBitmap != null && containerSize.width > 0 && containerSize.height > 0) {
            val imgW = orientedBitmap.width.toFloat()
            val imgH = orientedBitmap.height.toFloat()
            val imgRatio = imgW / imgH

            val padding = 48f
            val maxDisplayW = (containerSize.width - padding * 2).coerceAtLeast(100f)
            val maxDisplayH = (containerSize.height - padding * 2).coerceAtLeast(100f)

            val baseDisplayW: Float
            val baseDisplayH: Float
            if (maxDisplayW / maxDisplayH > imgRatio) {
                baseDisplayH = maxDisplayH
                baseDisplayW = baseDisplayH * imgRatio
            } else {
                baseDisplayW = maxDisplayW
                baseDisplayH = baseDisplayW / imgRatio
            }

            // Apply viewport zoom if active
            val displayW = baseDisplayW * cropState.zoomScale
            val displayH = baseDisplayH * cropState.zoomScale

            val density = LocalDensity.current
            val displayWDp = with(density) { displayW.toDp() }
            val displayHDp = with(density) { displayH.toDp() }

            val totalW = containerSize.width.toFloat()
            val totalH = containerSize.height.toFloat()
            val imgScreenLeft = (totalW - displayW) / 2f + cropState.panOffsetX
            val imgScreenTop = (totalH - displayH) / 2f + cropState.panOffsetY

            // 1. Base Image Container (Full complete image)
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { ((totalW - displayW) / 2f + cropState.panOffsetX).toDp() },
                        y = with(density) { ((totalH - displayH) / 2f + cropState.panOffsetY).toDp() }
                    )
                    .size(width = displayWDp, height = displayHDp)
            ) {
                Image(
                    bitmap = orientedBitmap.asImageBitmap(),
                    contentDescription = "Imagen completa para recorte personalizado",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Interactive Gesture Canvas Overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(displayW, displayH, imgScreenLeft, imgScreenTop) {
                        val touchRadius = 38.dp.toPx()

                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val cropL = imgScreenLeft + cropState.cropLeftNorm * displayW
                                val cropT = imgScreenTop + cropState.cropTopNorm * displayH
                                val cropR = imgScreenLeft + cropState.cropRightNorm * displayW
                                val cropB = imgScreenTop + cropState.cropBottomNorm * displayH

                                val px = startOffset.x
                                val py = startOffset.y

                                // Hit test corners first
                                val distTL = Math.hypot((px - cropL).toDouble(), (py - cropT).toDouble())
                                val distTR = Math.hypot((px - cropR).toDouble(), (py - cropT).toDouble())
                                val distBL = Math.hypot((px - cropL).toDouble(), (py - cropB).toDouble())
                                val distBR = Math.hypot((px - cropR).toDouble(), (py - cropB).toDouble())

                                val cornerList = listOf(
                                    0 to distTL,
                                    1 to distTR,
                                    2 to distBL,
                                    3 to distBR
                                )
                                val closestCorner = cornerList.minByOrNull { it.second }

                                if (closestCorner != null && closestCorner.second <= touchRadius) {
                                    cropState.activeHandle = closestCorner.first
                                } else {
                                    val cornerMargin = touchRadius * 0.7f
                                    when {
                                        // Top edge
                                        px in (cropL + cornerMargin)..(cropR - cornerMargin) && Math.abs(py - cropT) <= touchRadius ->
                                            cropState.activeHandle = 4
                                        // Bottom edge
                                        px in (cropL + cornerMargin)..(cropR - cornerMargin) && Math.abs(py - cropB) <= touchRadius ->
                                            cropState.activeHandle = 5
                                        // Left edge
                                        py in (cropT + cornerMargin)..(cropB - cornerMargin) && Math.abs(px - cropL) <= touchRadius ->
                                            cropState.activeHandle = 6
                                        // Right edge
                                        py in (cropT + cornerMargin)..(cropB - cornerMargin) && Math.abs(px - cropR) <= touchRadius ->
                                            cropState.activeHandle = 7
                                        // Inside rectangle -> Body Move
                                        px in cropL..cropR && py in cropT..cropB ->
                                            cropState.activeHandle = 8
                                        else ->
                                            cropState.activeHandle = -1
                                    }
                                }
                            },
                            onDragEnd = {
                                cropState.activeHandle = -1
                            },
                            onDragCancel = {
                                cropState.activeHandle = -1
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (cropState.activeHandle != -1) {
                                    val deltaNormX = dragAmount.x / displayW
                                    val deltaNormY = dragAmount.y / displayH
                                    cropState.onDrag(
                                        handle = cropState.activeHandle,
                                        deltaNormX = deltaNormX,
                                        deltaNormY = deltaNormY,
                                        imgW = imgW,
                                        imgH = imgH,
                                        displayW = displayW,
                                        displayH = displayH
                                    )
                                } else {
                                    // Panning the viewport when dragging outside selection
                                    cropState.panOffsetX += dragAmount.x
                                    cropState.panOffsetY += dragAmount.y
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                cropState.resetToFull()
                            }
                        )
                    }
            ) {
                val cropLeft = imgScreenLeft + cropState.cropLeftNorm * displayW
                val cropTop = imgScreenTop + cropState.cropTopNorm * displayH
                val cropRight = imgScreenLeft + cropState.cropRightNorm * displayW
                val cropBottom = imgScreenTop + cropState.cropBottomNorm * displayH
                val frameW = (cropRight - cropLeft).coerceAtLeast(1f)
                val frameH = (cropBottom - cropTop).coerceAtLeast(1f)

                // 1. Darkened Vignette / Outer Scrim (Outside CropRect is darkened)
                val outerPath = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                }
                val innerPath = Path().apply {
                    addRect(Rect(cropLeft, cropTop, cropRight, cropBottom))
                }
                val scrimPath = Path().apply {
                    op(outerPath, innerPath, PathOperation.Difference)
                }
                drawPath(scrimPath, color = Color(0xD9050609))

                // 2. High-contrast Frame Border
                // Subtle black background stroke for contrast on pure white images
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(cropLeft - 1f, cropTop - 1f),
                    size = Size(frameW + 2f, frameH + 2f),
                    style = Stroke(width = 3.dp.toPx())
                )
                // Crisp white inner stroke
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cropLeft, cropTop),
                    size = Size(frameW, frameH),
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // 3. Optional Composition Grid (Rule of Thirds 3×3)
                if (cropState.showGrid || cropState.activeHandle != -1) {
                    val gridColor = Color.White.copy(alpha = if (cropState.activeHandle != -1) 0.55f else 0.35f)
                    val gridStroke = 1.0.dp.toPx()

                    val x1 = cropLeft + frameW / 3f
                    val x2 = cropLeft + frameW * 2f / 3f
                    drawLine(gridColor, Offset(x1, cropTop), Offset(x1, cropBottom), strokeWidth = gridStroke)
                    drawLine(gridColor, Offset(x2, cropTop), Offset(x2, cropBottom), strokeWidth = gridStroke)

                    val y1 = cropTop + frameH / 3f
                    val y2 = cropTop + frameH * 2f / 3f
                    drawLine(gridColor, Offset(cropLeft, y1), Offset(cropRight, y1), strokeWidth = gridStroke)
                    drawLine(gridColor, Offset(cropLeft, y2), Offset(cropRight, y2), strokeWidth = gridStroke)
                }

                // 4. Corner Bracket Handles (Bold L-shapes at the 4 corners)
                val cornerLength = 22.dp.toPx().coerceAtMost(minOf(frameW, frameH) / 3f)
                val cornerStroke = 4.0.dp.toPx()
                val cornerColor = if (cropState.activeHandle in 0..3) Color(0xFF00FFFF) else Color(0xFF00E5FF)

                // Top-Left corner
                drawLine(cornerColor, Offset(cropLeft - 1f, cropTop), Offset(cropLeft + cornerLength, cropTop), strokeWidth = cornerStroke)
                drawLine(cornerColor, Offset(cropLeft, cropTop - 1f), Offset(cropLeft, cropTop + cornerLength), strokeWidth = cornerStroke)

                // Top-Right corner
                drawLine(cornerColor, Offset(cropRight - cornerLength, cropTop), Offset(cropRight + 1f, cropTop), strokeWidth = cornerStroke)
                drawLine(cornerColor, Offset(cropRight, cropTop - 1f), Offset(cropRight, cropTop + cornerLength), strokeWidth = cornerStroke)

                // Bottom-Left corner
                drawLine(cornerColor, Offset(cropLeft - 1f, cropBottom), Offset(cropLeft + cornerLength, cropBottom), strokeWidth = cornerStroke)
                drawLine(cornerColor, Offset(cropLeft, cropBottom - cornerLength), Offset(cropLeft, cropBottom + 1f), strokeWidth = cornerStroke)

                // Bottom-Right corner
                drawLine(cornerColor, Offset(cropRight - cornerLength, cropBottom), Offset(cropRight + 1f, cropBottom), strokeWidth = cornerStroke)
                drawLine(cornerColor, Offset(cropRight, cropBottom - cornerLength), Offset(cropRight, cropBottom + 1f), strokeWidth = cornerStroke)

                // 5. Side Midpoint Handles (Horizontal & Vertical Bars for individual side resizing)
                val edgeLength = 18.dp.toPx().coerceAtMost(minOf(frameW, frameH) / 4f)
                val edgeStroke = 3.2.dp.toPx()
                val edgeColor = if (cropState.activeHandle in 4..7) Color(0xFF00FFFF) else Color.White

                val midX = cropLeft + frameW / 2f
                val midY = cropTop + frameH / 2f

                // Top midpoint (Handle 4)
                drawLine(edgeColor, Offset(midX - edgeLength / 2f, cropTop), Offset(midX + edgeLength / 2f, cropTop), strokeWidth = edgeStroke)
                // Bottom midpoint (Handle 5)
                drawLine(edgeColor, Offset(midX - edgeLength / 2f, cropBottom), Offset(midX + edgeLength / 2f, cropBottom), strokeWidth = edgeStroke)
                // Left midpoint (Handle 6)
                drawLine(edgeColor, Offset(cropLeft, midY - edgeLength / 2f), Offset(cropLeft, midY + edgeLength / 2f), strokeWidth = edgeStroke)
                // Right midpoint (Handle 7)
                drawLine(edgeColor, Offset(cropRight, midY - edgeLength / 2f), Offset(cropRight, midY + edgeLength / 2f), strokeWidth = edgeStroke)
            }

            // Top Status Pill (Real Pixel Readout & Ratio)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val realPixelW = ((cropState.cropRightNorm - cropState.cropLeftNorm) * imgW).toInt().coerceAtLeast(1)
                    val realPixelH = ((cropState.cropBottomNorm - cropState.cropTopNorm) * imgH).toInt().coerceAtLeast(1)
                    val ratioValue = realPixelW.toFloat() / realPixelH.toFloat()
                    val ratioFormatted = String.format(Locale.US, "%.2f:1", ratioValue)

                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = "$realPixelW × $realPixelH px",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "($ratioFormatted)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    AssistChip(
                        onClick = { cropState.toggleLockAspectRatio(imgW, imgH) },
                        label = {
                            Text(
                                text = if (cropState.isAspectRatioLocked) "Bloqueado" else "Libre",
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (cropState.isAspectRatioLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (cropState.isAspectRatioLocked)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.height(26.dp)
                    )
                }
            }

            // Bottom Action Bar (Cancelar, Restablecer, Cuadrícula, Bloquear, Aplicar)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón Cancelar
                    OutlinedButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancelar", fontSize = 12.sp)
                    }

                    // Botón Restablecer
                    IconButton(
                        onClick = { cropState.resetToFull() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Restablecer recorte",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Toggle Cuadrícula 3x3
                    IconButton(
                        onClick = { cropState.showGrid = !cropState.showGrid },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (cropState.showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                            contentDescription = "Cuadrícula de tercios",
                            tint = if (cropState.showGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botón Aplicar
                    Button(
                        onClick = {
                            onApply(
                                cropState.cropLeftNorm,
                                cropState.cropTopNorm,
                                cropState.cropRightNorm,
                                cropState.cropBottomNorm
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Aplicar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aplicar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
