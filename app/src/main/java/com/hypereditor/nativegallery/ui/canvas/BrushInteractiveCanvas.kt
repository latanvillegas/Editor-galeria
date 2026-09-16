package com.hypereditor.nativegallery.ui.canvas

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hypereditor.nativegallery.domain.model.EditOperation

/**
 * Interactive canvas for Brush & Eraser (Pincel y Borrador) in CREATIVE_TOOLS.
 *
 * Responsibilities:
 * 1. Computes exact image display bounds via [computeImageBounds].
 * 2. Captures touch gestures directly on top of the image (no intercepting elements).
 * 3. Draws the active stroke in real time on the Compose Canvas at 60+ FPS while dragging.
 * 4. Shows a circular brush cursor reflecting the active size and position under the finger.
 * 5. Accurately transforms screen coordinates to normalized image coordinates [0f, 1f].
 * 6. Dispatches a single atomic [EditOperation.BrushDraw] upon lifting the finger (onDragEnd)
 *    ensuring clean single-step Undo/Redo integration.
 */
@Composable
fun BrushInteractiveCanvas(
    bitmap: Bitmap?,
    brushColor: Int,
    brushSize: Float,
    brushOpacity: Float,
    isEraserMode: Boolean,
    onApplyStroke: (EditOperation.BrushDraw) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val activeScreenPoints = remember { mutableStateListOf<Offset>() }
    var currentCursorScreen by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null || containerSize.width <= 0 || containerSize.height <= 0) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            return@Box
        }

        val bounds = computeImageBounds(containerSize, bitmap)
        val (imgLeft, imgTop, imgW, imgH) = bounds

        if (imgW <= 0f || imgH <= 0f) {
            return@Box
        }

        val density = LocalDensity.current
        val minDim = minOf(imgW, imgH)
        val scaleFactor = minDim / 1000f
        val screenStrokeWidth = (brushSize * scaleFactor).coerceAtLeast(2f)

        fun screenToNorm(pt: Offset): Pair<Float, Float> {
            val nx = ((pt.x - imgLeft) / imgW).coerceIn(0f, 1f)
            val ny = ((pt.y - imgTop) / imgH).coerceIn(0f, 1f)
            return Pair(nx, ny)
        }

        // 1. Rendered base image (with all previous pipeline stages & strokes applied)
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Canvas de Pincel",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset(
                    x = with(density) { imgLeft.toDp() },
                    y = with(density) { imgTop.toDp() }
                )
                .size(
                    width = with(density) { imgW.toDp() },
                    height = with(density) { imgH.toDp() }
                )
                .clip(RoundedCornerShape(4.dp))
        )

        // 2. Interactive Gesture Canvas Overlay: captures touch and draws real-time stroke
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(brushColor, brushSize, brushOpacity, isEraserMode, imgLeft, imgTop, imgW, imgH) {
                    detectDragGestures(
                        onDragStart = { startPos ->
                            currentCursorScreen = startPos
                            activeScreenPoints.clear()
                            activeScreenPoints.add(startPos)
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            currentCursorScreen = null
                            if (activeScreenPoints.size >= 2) {
                                val normPoints = activeScreenPoints.map { screenToNorm(it) }
                                val stroke = EditOperation.BrushDraw(
                                    points = normPoints,
                                    colorInt = brushColor,
                                    strokeWidth = brushSize,
                                    opacity = brushOpacity,
                                    isEraser = isEraserMode
                                )
                                onApplyStroke(stroke)
                            } else if (activeScreenPoints.size == 1) {
                                val pt = activeScreenPoints.first()
                                val p1 = screenToNorm(pt)
                                val p2 = screenToNorm(Offset(pt.x + 0.5f, pt.y + 0.5f))
                                val stroke = EditOperation.BrushDraw(
                                    points = listOf(p1, p2),
                                    colorInt = brushColor,
                                    strokeWidth = brushSize,
                                    opacity = brushOpacity,
                                    isEraser = isEraserMode
                                )
                                onApplyStroke(stroke)
                            }
                            activeScreenPoints.clear()
                        },
                        onDragCancel = {
                            isDragging = false
                            currentCursorScreen = null
                            activeScreenPoints.clear()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentCursorScreen = change.position
                            activeScreenPoints.add(change.position)
                        }
                    )
                }
        ) {
            // Real-time active stroke drawing
            if (activeScreenPoints.size >= 2) {
                val strokePath = Path().apply {
                    moveTo(activeScreenPoints.first().x, activeScreenPoints.first().y)
                    for (i in 1 until activeScreenPoints.size) {
                        lineTo(activeScreenPoints[i].x, activeScreenPoints[i].y)
                    }
                }
                val strokeComposeColor = if (isEraserMode) {
                    Color.White.copy(alpha = brushOpacity * 0.75f)
                } else {
                    Color(brushColor).copy(alpha = brushOpacity.coerceIn(0f, 1f))
                }
                drawPath(
                    path = strokePath,
                    color = strokeComposeColor,
                    style = Stroke(
                        width = screenStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (activeScreenPoints.size == 1) {
                val pt = activeScreenPoints.first()
                val strokeComposeColor = if (isEraserMode) {
                    Color.White.copy(alpha = brushOpacity * 0.75f)
                } else {
                    Color(brushColor).copy(alpha = brushOpacity.coerceIn(0f, 1f))
                }
                drawCircle(
                    color = strokeComposeColor,
                    radius = screenStrokeWidth / 2f,
                    center = pt
                )
            }

            // Brush tip cursor indicator directly beneath finger/stylus
            currentCursorScreen?.let { cursor ->
                val cursorRadius = screenStrokeWidth / 2f
                // High-contrast double ring (black outer shadow + colored/white inner ring)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = cursorRadius + 1f,
                    center = cursor,
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = if (isEraserMode) Color.White else Color(brushColor),
                    radius = cursorRadius,
                    center = cursor,
                    style = Stroke(width = 1.5f)
                )
            }
        }
    }
}
