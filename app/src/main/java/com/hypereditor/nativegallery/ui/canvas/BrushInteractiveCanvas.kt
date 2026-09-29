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

@Composable
fun BrushInteractiveCanvas(
    bitmap: Bitmap?, brushColor: Int, brushSize: Float, brushOpacity: Float,
    brushHardness: Float = 1f, brushFlow: Float = 1f, brushSmoothing: Float = 0.35f,
    isEraserMode: Boolean, onApplyStroke: (EditOperation.BrushDraw) -> Unit, modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val activeScreenPoints = remember { mutableStateListOf<Offset>() }
    var currentCursorScreen by remember { mutableStateOf<Offset?>(null) }
    var filteredPoint by remember { mutableStateOf<Offset?>(null) }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0D0E12)).onSizeChanged { containerSize = it }, contentAlignment = Alignment.Center) {
        if (bitmap == null || containerSize.width <= 0 || containerSize.height <= 0) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary); return@Box }
        val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(containerSize, bitmap)
        if (imgW <= 0f || imgH <= 0f) return@Box
        val density = LocalDensity.current
        val scaleFactor = minOf(imgW, imgH) / 1000f
        val screenStrokeWidth = (brushSize * scaleFactor).coerceAtLeast(2f)
        val previewAlpha = (brushOpacity * brushFlow).coerceIn(0.02f, 1f)

        fun screenToNorm(pt: Offset) = Pair(((pt.x - imgLeft) / imgW).coerceIn(0f, 1f), ((pt.y - imgTop) / imgH).coerceIn(0f, 1f))
        fun apply(points: List<Offset>) {
            if (points.isEmpty()) return
            val normPoints = if (points.size == 1) { val pt = points.first(); listOf(screenToNorm(pt), screenToNorm(Offset(pt.x + 0.5f, pt.y + 0.5f))) } else points.map(::screenToNorm)
            onApplyStroke(EditOperation.BrushDraw(points = normPoints, colorInt = brushColor, strokeWidth = brushSize, opacity = brushOpacity, hardness = brushHardness, flow = brushFlow, smoothing = brushSmoothing, isEraser = isEraserMode))
        }
        fun smooth(raw: Offset): Offset {
            val previous = filteredPoint ?: raw
            val smoothing = brushSmoothing.coerceIn(0f, 0.9f)
            val response = 1f - smoothing * 0.82f
            return Offset(previous.x + (raw.x - previous.x) * response, previous.y + (raw.y - previous.y) * response).also { filteredPoint = it }
        }

        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Canvas de Pincel", contentScale = ContentScale.FillBounds, modifier = Modifier.offset(x = with(density) { imgLeft.toDp() }, y = with(density) { imgTop.toDp() }).size(width = with(density) { imgW.toDp() }, height = with(density) { imgH.toDp() }).clip(RoundedCornerShape(4.dp)))
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(brushColor, brushSize, brushOpacity, brushHardness, brushFlow, brushSmoothing, isEraserMode, imgLeft, imgTop, imgW, imgH) {
            detectDragGestures(
                onDragStart = { activeScreenPoints.clear(); filteredPoint = it; activeScreenPoints.add(it); currentCursorScreen = it },
                onDragEnd = { apply(activeScreenPoints.toList()); activeScreenPoints.clear(); filteredPoint = null; currentCursorScreen = null },
                onDragCancel = { activeScreenPoints.clear(); filteredPoint = null; currentCursorScreen = null },
                onDrag = { change, _ -> change.consume(); currentCursorScreen = change.position; activeScreenPoints.add(smooth(change.position)) }
            )
        }) {
            val baseColor = if (isEraserMode) Color.White else Color(brushColor)
            val softAlpha = previewAlpha * (0.18f + brushHardness.coerceIn(0f, 1f) * 0.82f)
            if (activeScreenPoints.size >= 2) {
                val path = Path().apply { moveTo(activeScreenPoints.first().x, activeScreenPoints.first().y); for (i in 1 until activeScreenPoints.size) lineTo(activeScreenPoints[i].x, activeScreenPoints[i].y) }
                if (brushHardness < 0.98f) drawPath(path, baseColor.copy(alpha = previewAlpha * (1f - brushHardness) * 0.35f), Stroke(width = screenStrokeWidth * (1.35f + (1f - brushHardness) * 0.65f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, baseColor.copy(alpha = softAlpha), Stroke(width = screenStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else if (activeScreenPoints.size == 1) {
                val pt = activeScreenPoints.first(); if (brushHardness < 0.98f) drawCircle(baseColor.copy(alpha = previewAlpha * (1f - brushHardness) * 0.35f), screenStrokeWidth * (0.68f + (1f - brushHardness) * 0.32f), pt); drawCircle(baseColor.copy(alpha = softAlpha), screenStrokeWidth / 2f, pt)
            }
            currentCursorScreen?.let { cursor -> val r = screenStrokeWidth / 2f; drawCircle(Color.Black.copy(alpha = 0.5f), r + 1f, cursor, style = Stroke(width = 2.5f)); drawCircle(if (isEraserMode) Color.White else Color(brushColor), r, cursor, style = Stroke(width = 1.5f)) }
        }
    }
}
