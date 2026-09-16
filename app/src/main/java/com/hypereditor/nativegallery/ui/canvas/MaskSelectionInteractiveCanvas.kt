package com.hypereditor.nativegallery.ui.canvas

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.*
import kotlin.math.hypot

private enum class HandleType {
    NONE,
    CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
}

@Composable
fun MaskSelectionInteractiveCanvas(
    bitmap: Bitmap?,
    activeMask: MaskModel?,
    onUpdateRectBounds: (RectNorm) -> Unit,
    onUpdateEllipseBounds: (RectNorm) -> Unit,
    onUpdateLassoPoints: (List<Pair<Float, Float>>) -> Unit,
    onAddBrushStroke: (MaskBrushStroke) -> Unit,
    onClearSelection: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    brushSizeNorm: Float = 0.05f,
    isEraserMode: Boolean = false,
    onToggleEraserMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var activeHandle by remember { mutableStateOf(HandleType.NONE) }
    var currentCursorScreen by remember { mutableStateOf<Offset?>(null) }
    val currentBrushPoints = remember { mutableStateListOf<Offset>() }
    val currentLassoPoints = remember { mutableStateListOf<Offset>() }
    var isDrawingLasso by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            return@Box
        }

        val cW = containerSize.width.toFloat()
        val cH = containerSize.height.toFloat()
        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()

        if (cW <= 0f || cH <= 0f || imgW <= 0f || imgH <= 0f) {
            return@Box
        }

        val scale = minOf(cW / imgW, cH / imgH)
        val renderedW = imgW * scale
        val renderedH = imgH * scale
        val offsetX = (cW - renderedW) / 2f
        val offsetY = (cH - renderedH) / 2f

        fun screenToNorm(pt: Offset): Offset {
            val nx = ((pt.x - offsetX) / renderedW).coerceIn(0f, 1f)
            val ny = ((pt.y - offsetY) / renderedH).coerceIn(0f, 1f)
            return Offset(nx, ny)
        }

        fun normToScreen(nx: Float, ny: Float): Offset {
            return Offset(offsetX + nx * renderedW, offsetY + ny * renderedH)
        }

        // Imagen de Fondo
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Lienzo de Selección",
            modifier = Modifier
                .offset(
                    x = (offsetX / androidx.compose.ui.platform.LocalDensity.current.density).dp,
                    y = (offsetY / androidx.compose.ui.platform.LocalDensity.current.density).dp
                )
                .size(
                    width = (renderedW / androidx.compose.ui.platform.LocalDensity.current.density).dp,
                    height = (renderedH / androidx.compose.ui.platform.LocalDensity.current.density).dp
                ),
            contentScale = ContentScale.FillBounds
        )

        // Canvas de Visualización e Interacción
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeMask?.id, activeMask?.selectionType, isEraserMode, brushSizeNorm) {
                    if (activeMask == null) return@pointerInput

                    when (activeMask.selectionType) {
                        SelectionToolType.RECTANGLE -> {
                            detectDragGestures(
                                onDragStart = { startPt ->
                                    val r = activeMask.rectBounds
                                    val tl = normToScreen(r.left, r.top)
                                    val tr = normToScreen(r.right, r.top)
                                    val bl = normToScreen(r.left, r.bottom)
                                    val br = normToScreen(r.right, r.bottom)
                                    val center = normToScreen((r.left + r.right) / 2f, (r.top + r.bottom) / 2f)

                                    val threshold = 36f
                                    activeHandle = when {
                                        hypot(startPt.x - tl.x, startPt.y - tl.y) <= threshold -> HandleType.TOP_LEFT
                                        hypot(startPt.x - tr.x, startPt.y - tr.y) <= threshold -> HandleType.TOP_RIGHT
                                        hypot(startPt.x - bl.x, startPt.y - bl.y) <= threshold -> HandleType.BOTTOM_LEFT
                                        hypot(startPt.x - br.x, startPt.y - br.y) <= threshold -> HandleType.BOTTOM_RIGHT
                                        hypot(startPt.x - center.x, startPt.y - center.y) <= threshold * 1.5f -> HandleType.CENTER
                                        else -> HandleType.CENTER
                                    }
                                },
                                onDragEnd = { activeHandle = HandleType.NONE },
                                onDragCancel = { activeHandle = HandleType.NONE },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val r = activeMask.rectBounds
                                    val dxNorm = dragAmount.x / renderedW
                                    val dyNorm = dragAmount.y / renderedH

                                    when (activeHandle) {
                                        HandleType.CENTER -> {
                                            val w = r.right - r.left
                                            val h = r.bottom - r.top
                                            val newLeft = (r.left + dxNorm).coerceIn(0f, 1f - w)
                                            val newTop = (r.top + dyNorm).coerceIn(0f, 1f - h)
                                            onUpdateRectBounds(RectNorm(newLeft, newTop, newLeft + w, newTop + h))
                                        }
                                        HandleType.TOP_LEFT -> {
                                            val newLeft = (r.left + dxNorm).coerceIn(0f, r.right - 0.05f)
                                            val newTop = (r.top + dyNorm).coerceIn(0f, r.bottom - 0.05f)
                                            onUpdateRectBounds(RectNorm(newLeft, newTop, r.right, r.bottom))
                                        }
                                        HandleType.TOP_RIGHT -> {
                                            val newRight = (r.right + dxNorm).coerceIn(r.left + 0.05f, 1f)
                                            val newTop = (r.top + dyNorm).coerceIn(0f, r.bottom - 0.05f)
                                            onUpdateRectBounds(RectNorm(r.left, newTop, newRight, r.bottom))
                                        }
                                        HandleType.BOTTOM_LEFT -> {
                                            val newLeft = (r.left + dxNorm).coerceIn(0f, r.right - 0.05f)
                                            val newBottom = (r.bottom + dyNorm).coerceIn(r.top + 0.05f, 1f)
                                            onUpdateRectBounds(RectNorm(newLeft, r.top, r.right, newBottom))
                                        }
                                        HandleType.BOTTOM_RIGHT -> {
                                            val newRight = (r.right + dxNorm).coerceIn(r.left + 0.05f, 1f)
                                            val newBottom = (r.bottom + dyNorm).coerceIn(r.top + 0.05f, 1f)
                                            onUpdateRectBounds(RectNorm(r.left, r.top, newRight, newBottom))
                                        }
                                        else -> {}
                                    }
                                }
                            )
                        }
                        SelectionToolType.ELLIPSE -> {
                            detectDragGestures(
                                onDragStart = { startPt ->
                                    val e = activeMask.ellipseBounds
                                    val center = normToScreen((e.left + e.right) / 2f, (e.top + e.bottom) / 2f)
                                    val top = normToScreen((e.left + e.right) / 2f, e.top)
                                    val bottom = normToScreen((e.left + e.right) / 2f, e.bottom)
                                    val left = normToScreen(e.left, (e.top + e.bottom) / 2f)
                                    val right = normToScreen(e.right, (e.top + e.bottom) / 2f)

                                    val threshold = 36f
                                    activeHandle = when {
                                        hypot(startPt.x - top.x, startPt.y - top.y) <= threshold -> HandleType.TOP
                                        hypot(startPt.x - bottom.x, startPt.y - bottom.y) <= threshold -> HandleType.BOTTOM
                                        hypot(startPt.x - left.x, startPt.y - left.y) <= threshold -> HandleType.LEFT
                                        hypot(startPt.x - right.x, startPt.y - right.y) <= threshold -> HandleType.RIGHT
                                        hypot(startPt.x - center.x, startPt.y - center.y) <= threshold * 1.5f -> HandleType.CENTER
                                        else -> HandleType.CENTER
                                    }
                                },
                                onDragEnd = { activeHandle = HandleType.NONE },
                                onDragCancel = { activeHandle = HandleType.NONE },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val e = activeMask.ellipseBounds
                                    val dxNorm = dragAmount.x / renderedW
                                    val dyNorm = dragAmount.y / renderedH

                                    when (activeHandle) {
                                        HandleType.CENTER -> {
                                            val w = e.right - e.left
                                            val h = e.bottom - e.top
                                            val newLeft = (e.left + dxNorm).coerceIn(0f, 1f - w)
                                            val newTop = (e.top + dyNorm).coerceIn(0f, 1f - h)
                                            onUpdateEllipseBounds(RectNorm(newLeft, newTop, newLeft + w, newTop + h))
                                        }
                                        HandleType.TOP -> {
                                            val newTop = (e.top + dyNorm).coerceIn(0f, e.bottom - 0.05f)
                                            onUpdateEllipseBounds(RectNorm(e.left, newTop, e.right, e.bottom))
                                        }
                                        HandleType.BOTTOM -> {
                                            val newBottom = (e.bottom + dyNorm).coerceIn(e.top + 0.05f, 1f)
                                            onUpdateEllipseBounds(RectNorm(e.left, e.top, e.right, newBottom))
                                        }
                                        HandleType.LEFT -> {
                                            val newLeft = (e.left + dxNorm).coerceIn(0f, e.right - 0.05f)
                                            onUpdateEllipseBounds(RectNorm(newLeft, e.top, e.right, e.bottom))
                                        }
                                        HandleType.RIGHT -> {
                                            val newRight = (e.right + dxNorm).coerceIn(e.left + 0.05f, 1f)
                                            onUpdateEllipseBounds(RectNorm(e.left, e.top, newRight, e.bottom))
                                        }
                                        else -> {}
                                    }
                                }
                            )
                        }
                        SelectionToolType.LASSO -> {
                            detectDragGestures(
                                onDragStart = { startPt ->
                                    isDrawingLasso = true
                                    currentLassoPoints.clear()
                                    currentLassoPoints.add(startPt)
                                },
                                onDragEnd = {
                                    isDrawingLasso = false
                                    if (currentLassoPoints.size >= 3) {
                                        val normList = currentLassoPoints.map {
                                            val norm = screenToNorm(it)
                                            Pair(norm.x, norm.y)
                                        }
                                        onUpdateLassoPoints(normList)
                                    }
                                    currentLassoPoints.clear()
                                },
                                onDragCancel = {
                                    isDrawingLasso = false
                                    currentLassoPoints.clear()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentLassoPoints.add(change.position)
                                }
                            )
                        }
                        SelectionToolType.BRUSH -> {
                            detectDragGestures(
                                onDragStart = { startPt ->
                                    currentCursorScreen = startPt
                                    currentBrushPoints.clear()
                                    currentBrushPoints.add(startPt)
                                },
                                onDragEnd = {
                                    currentCursorScreen = null
                                    if (currentBrushPoints.size >= 2) {
                                        val normPoints = currentBrushPoints.map {
                                            val norm = screenToNorm(it)
                                            Pair(norm.x, norm.y)
                                        }
                                        onAddBrushStroke(
                                            MaskBrushStroke(
                                                points = normPoints,
                                                strokeWidthNorm = brushSizeNorm,
                                                isEraser = isEraserMode
                                            )
                                        )
                                    }
                                    currentBrushPoints.clear()
                                },
                                onDragCancel = {
                                    currentCursorScreen = null
                                    currentBrushPoints.clear()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentCursorScreen = change.position
                                    currentBrushPoints.add(change.position)
                                }
                            )
                        }
                    }
                }
        ) {
            if (activeMask == null) return@Canvas

            val rubylithColor = if (activeMask.selectionMode == SelectionMode.ADD) {
                Color(0x55E91E63) // Rosa translúcido (Añadir)
            } else {
                Color(0x552196F3) // Azul translúcido (Quitar)
            }
            val outlineColor = if (activeMask.selectionMode == SelectionMode.ADD) {
                Color(0xFFFF4081)
            } else {
                Color(0xFF64B5F6)
            }
            val handleFill = Color.White

            when (activeMask.selectionType) {
                SelectionToolType.RECTANGLE -> {
                    val r = activeMask.rectBounds
                    val tl = normToScreen(r.left, r.top)
                    val br = normToScreen(r.right, r.bottom)
                    val rect = Rect(tl.x, tl.y, br.x, br.y)

                    // Área seleccionada
                    drawRect(color = rubylithColor, topLeft = rect.topLeft, size = rect.size)

                    // Borde punteado
                    drawRect(
                        color = outlineColor,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )

                    // Handles en esquinas y centro
                    val tr = normToScreen(r.right, r.top)
                    val bl = normToScreen(r.left, r.bottom)
                    val center = normToScreen((r.left + r.right) / 2f, (r.top + r.bottom) / 2f)

                    listOf(tl, tr, bl, br).forEach { pos ->
                        drawCircle(color = outlineColor, radius = 9f, center = pos)
                        drawCircle(color = handleFill, radius = 6f, center = pos)
                    }
                    drawCircle(color = outlineColor, radius = 12f, center = center)
                    drawCircle(color = outlineColor.copy(alpha = 0.4f), radius = 16f, center = center)
                    drawCircle(color = handleFill, radius = 7f, center = center)
                }
                SelectionToolType.ELLIPSE -> {
                    val e = activeMask.ellipseBounds
                    val tl = normToScreen(e.left, e.top)
                    val br = normToScreen(e.right, e.bottom)
                    val rect = Rect(tl.x, tl.y, br.x, br.y)

                    // Área seleccionada
                    drawOval(color = rubylithColor, topLeft = rect.topLeft, size = rect.size)

                    // Borde punteado
                    drawOval(
                        color = outlineColor,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )

                    // Handles cardinales y centro
                    val center = normToScreen((e.left + e.right) / 2f, (e.top + e.bottom) / 2f)
                    val top = normToScreen((e.left + e.right) / 2f, e.top)
                    val bottom = normToScreen((e.left + e.right) / 2f, e.bottom)
                    val left = normToScreen(e.left, (e.top + e.bottom) / 2f)
                    val right = normToScreen(e.right, (e.top + e.bottom) / 2f)

                    listOf(top, bottom, left, right).forEach { pos ->
                        drawCircle(color = outlineColor, radius = 9f, center = pos)
                        drawCircle(color = handleFill, radius = 6f, center = pos)
                    }
                    drawCircle(color = outlineColor, radius = 12f, center = center)
                    drawCircle(color = handleFill, radius = 7f, center = center)
                }
                SelectionToolType.LASSO -> {
                    // Puntos del lazo guardados
                    val pts = if (isDrawingLasso) currentLassoPoints else {
                        activeMask.lassoPoints.map { normToScreen(it.first, it.second) }
                    }

                    if (pts.size >= 2) {
                        val path = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 1 until pts.size) {
                                lineTo(pts[i].x, pts[i].y)
                            }
                            if (!isDrawingLasso) {
                                close()
                            }
                        }

                        if (!isDrawingLasso && pts.size >= 3) {
                            drawPath(path = path, color = rubylithColor)
                        }

                        drawPath(
                            path = path,
                            color = outlineColor,
                            style = Stroke(
                                width = 3f,
                                pathEffect = if (!isDrawingLasso) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
                            )
                        )
                    }
                }
                SelectionToolType.BRUSH -> {
                    // Trazos ya existentes
                    for (stroke in activeMask.brushStrokes) {
                        if (stroke.points.size >= 2) {
                            val path = Path().apply {
                                val p0 = normToScreen(stroke.points.first().first, stroke.points.first().second)
                                moveTo(p0.x, p0.y)
                                for (i in 1 until stroke.points.size) {
                                    val pi = normToScreen(stroke.points[i].first, stroke.points[i].second)
                                    lineTo(pi.x, pi.y)
                                }
                            }
                            val sColor = if (stroke.isEraser) Color(0x66000000) else rubylithColor
                            val sWidth = (stroke.strokeWidthNorm * minOf(renderedW, renderedH)).coerceAtLeast(6f)
                            drawPath(
                                path = path,
                                color = sColor,
                                style = Stroke(width = sWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }

                    // Trazo en curso
                    if (currentBrushPoints.size >= 2) {
                        val path = Path().apply {
                            moveTo(currentBrushPoints.first().x, currentBrushPoints.first().y)
                            for (i in 1 until currentBrushPoints.size) {
                                lineTo(currentBrushPoints[i].x, currentBrushPoints[i].y)
                            }
                        }
                        val liveWidth = (brushSizeNorm * minOf(renderedW, renderedH)).coerceAtLeast(6f)
                        val liveColor = if (isEraserMode) Color(0x88000000) else rubylithColor
                        drawPath(
                            path = path,
                            color = liveColor,
                            style = Stroke(width = liveWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // Indicador circular de pincel bajo el cursor
                    currentCursorScreen?.let { cursor ->
                        val radiusPx = (brushSizeNorm * minOf(renderedW, renderedH) / 2f).coerceAtLeast(4f)
                        drawCircle(
                            color = if (isEraserMode) Color.White else outlineColor,
                            radius = radiusPx,
                            center = cursor,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }

        // Barra flotante de control rápido sobre el canvas
        if (activeMask != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val toolName = when (activeMask.selectionType) {
                        SelectionToolType.RECTANGLE -> "Rectángulo"
                        SelectionToolType.ELLIPSE -> "Óvalo"
                        SelectionToolType.LASSO -> "Lazo"
                        SelectionToolType.BRUSH -> "Pincel"
                    }
                    Text(
                        text = "$toolName",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    // Modo Añadir / Quitar
                    OutlinedButton(
                        onClick = onToggleSelectionMode,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (activeMask.selectionMode == SelectionMode.ADD) {
                                Color(0xFFE91E63).copy(alpha = 0.2f)
                            } else {
                                Color(0xFF2196F3).copy(alpha = 0.2f)
                            }
                        ),
                        modifier = Modifier.height(28.dp)
                    ) {
                        val modeLabel = if (activeMask.selectionMode == SelectionMode.ADD) "+ Añadir" else "- Quitar"
                        Text(text = modeLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // En modo Pincel: botón de borrar trazo vs pintar
                    if (activeMask.selectionType == SelectionToolType.BRUSH) {
                        IconButton(
                            onClick = onToggleEraserMode,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isEraserMode) Icons.Default.AutoFixNormal else Icons.Default.Brush,
                                contentDescription = if (isEraserMode) "Goma" else "Pincel",
                                tint = if (isEraserMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Botón Borrar Selección
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar Selección",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
