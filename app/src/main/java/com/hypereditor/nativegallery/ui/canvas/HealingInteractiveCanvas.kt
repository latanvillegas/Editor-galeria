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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.EditOperation
import kotlin.math.hypot

enum class HealingToolMode {
    TAP,   // Modo Toque: imperfecciones puntuales (polvo, manchas, granos)
    BRUSH  // Modo Pincel: trazo continuo (cables, rayones, arrugas)
}

enum class HealingSamplingMode {
    AUTO,   // Muestreo automático del vecindario circundante
    MANUAL  // Muestreo manual con punto de referencia seleccionado
}

@Composable
fun HealingInteractiveCanvas(
    bitmap: Bitmap?,
    toolMode: HealingToolMode,
    samplingMode: HealingSamplingMode,
    onToolModeChanged: (HealingToolMode) -> Unit,
    onSamplingModeChanged: (HealingSamplingMode) -> Unit,
    radius: Float,
    feather: Float,
    strength: Float,
    manualSourceNorm: Offset?,
    onManualSourceSelected: (Offset) -> Unit,
    onApplyStroke: (EditOperation.HealingStroke) -> Unit,
    strokesCount: Int,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var isSelectingManualSource by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var activeCursorScreen by remember { mutableStateOf<Offset?>(null) }
    val currentBrushPoints = remember { mutableStateListOf<Offset>() }
    var lastTapPoint by remember { mutableStateOf<Offset?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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

        // Dimensiones del contenedor y de la imagen escalada
        val cW = containerSize.width.toFloat()
        val cH = containerSize.height.toFloat()
        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()

        if (cW <= 0f || cH <= 0f || imgW <= 0f || imgH <= 0f) {
            return@Box
        }

        val scale = minOf(cW / imgW, cH / imgH)
        val displayW = imgW * scale
        val displayH = imgH * scale
        val imgScreenLeft = (cW - displayW) / 2f
        val imgScreenTop = (cH - displayH) / 2f

        val screenRadius = (radius * (displayW / imgW)).coerceAtLeast(8f)

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Imagen base renderizada
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Canvas de Corrección",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Capa interactiva de captura de gestos
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                        toolMode,
                        samplingMode,
                        isSelectingManualSource,
                        radius,
                        feather,
                        strength,
                        manualSourceNorm,
                        displayW,
                        displayH,
                        imgScreenLeft,
                        imgScreenTop
                    ) {
                        if (isSelectingManualSource) {
                            // Modo de selección de fuente manual
                            detectTapGestures { tapPos ->
                                val normX = (tapPos.x - imgScreenLeft) / displayW
                                val normY = (tapPos.y - imgScreenTop) / displayH
                                if (normX in 0f..1f && normY in 0f..1f) {
                                    onManualSourceSelected(Offset(normX, normY))
                                    isSelectingManualSource = false
                                    statusMessage = "Origen manual fijado"
                                } else {
                                    statusMessage = "Toca dentro de la imagen para fijar el origen"
                                }
                            }
                        } else if (toolMode == HealingToolMode.TAP) {
                            // Modo Toque: repara imperfección puntual en un solo toque
                            detectTapGestures { tapPos ->
                                val normX = (tapPos.x - imgScreenLeft) / displayW
                                val normY = (tapPos.y - imgScreenTop) / displayH
                                if (normX in 0f..1f && normY in 0f..1f) {
                                    lastTapPoint = tapPos
                                    val manualOffset = if (samplingMode == HealingSamplingMode.MANUAL && manualSourceNorm != null) {
                                        EditOperation.PointOffset(
                                            dx = manualSourceNorm.x - normX,
                                            dy = manualSourceNorm.y - normY
                                        )
                                    } else null

                                    val stroke = EditOperation.HealingStroke(
                                        points = listOf(EditOperation.HealingPoint(normX, normY)),
                                        radius = radius,
                                        feather = feather,
                                        strength = strength,
                                        manualSourceOffset = manualOffset
                                    )
                                    onApplyStroke(stroke)
                                    statusMessage = null
                                } else {
                                    statusMessage = "Toca sobre una imperfección dentro de la imagen"
                                }
                            }
                        } else {
                            // Modo Pincel: arrastre continuo para cables, rayones o manchas alargadas
                            detectDragGestures(
                                onDragStart = { startPos ->
                                    isDragging = true
                                    currentBrushPoints.clear()
                                    activeCursorScreen = startPos
                                    val normX = (startPos.x - imgScreenLeft) / displayW
                                    val normY = (startPos.y - imgScreenTop) / displayH
                                    if (normX in 0f..1f && normY in 0f..1f) {
                                        currentBrushPoints.add(Offset(normX, normY))
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val pos = change.position
                                    activeCursorScreen = pos
                                    val normX = (pos.x - imgScreenLeft) / displayW
                                    val normY = (pos.y - imgScreenTop) / displayH
                                    if (normX in 0f..1f && normY in 0f..1f) {
                                        val last = currentBrushPoints.lastOrNull()
                                        if (last == null) {
                                            currentBrushPoints.add(Offset(normX, normY))
                                        } else {
                                            val dx = (normX - last.x) * imgW
                                            val dy = (normY - last.y) * imgH
                                            val dist = hypot(dx, dy)
                                            val step = (radius * 0.45f).coerceAtLeast(4f)
                                            if (dist >= step) {
                                                val steps = (dist / step).toInt().coerceAtLeast(1)
                                                for (s in 1..steps) {
                                                    val frac = s.toFloat() / steps
                                                    currentBrushPoints.add(
                                                        Offset(
                                                            last.x + (normX - last.x) * frac,
                                                            last.y + (normY - last.y) * frac
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    isDragging = false
                                    activeCursorScreen = null
                                    if (currentBrushPoints.isNotEmpty()) {
                                        val firstPoint = currentBrushPoints.first()
                                        val manualOffset = if (samplingMode == HealingSamplingMode.MANUAL && manualSourceNorm != null) {
                                            EditOperation.PointOffset(
                                                dx = manualSourceNorm.x - firstPoint.x,
                                                dy = manualSourceNorm.y - firstPoint.y
                                            )
                                        } else null

                                        val strokePoints = currentBrushPoints.map {
                                            EditOperation.HealingPoint(it.x, it.y)
                                        }
                                        val stroke = EditOperation.HealingStroke(
                                            points = strokePoints,
                                            radius = radius,
                                            feather = feather,
                                            strength = strength,
                                            manualSourceOffset = manualOffset
                                        )
                                        onApplyStroke(stroke)
                                        statusMessage = null
                                    }
                                    currentBrushPoints.clear()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    activeCursorScreen = null
                                    currentBrushPoints.clear()
                                }
                            )
                        }
                    }
            )

            // Canvas de superposición visual (no se exporta)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Indicador de fuente manual si existe
                if (samplingMode == HealingSamplingMode.MANUAL && manualSourceNorm != null) {
                    val srcX = imgScreenLeft + manualSourceNorm.x * displayW
                    val srcY = imgScreenTop + manualSourceNorm.y * displayH

                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                        radius = screenRadius,
                        center = Offset(srcX, srcY)
                    )
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = screenRadius,
                        center = Offset(srcX, srcY),
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                    )
                    // Cruz central
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(srcX - 10f, srcY),
                        end = Offset(srcX + 10f, srcY),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(srcX, srcY - 10f),
                        end = Offset(srcX, srcY + 10f),
                        strokeWidth = 2f
                    )
                }

                // Trazos en vivo durante el arrastre en Modo Pincel
                if (isDragging && currentBrushPoints.isNotEmpty()) {
                    for (pt in currentBrushPoints) {
                        val px = imgScreenLeft + pt.x * displayW
                        val py = imgScreenTop + pt.y * displayH
                        drawCircle(
                            color = Color(0xFFFFB300).copy(alpha = 0.35f),
                            radius = screenRadius,
                            center = Offset(px, py)
                        )
                    }
                }

                // Cursor activo
                activeCursorScreen?.let { cursor ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = screenRadius,
                        center = cursor,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.5f),
                        radius = screenRadius + 1f,
                        center = cursor,
                        style = Stroke(width = 1f)
                    )
                    // Círculo interior de feather
                    val innerR = screenRadius * (1f - feather)
                    if (innerR > 2f) {
                        drawCircle(
                            color = Color(0xFFFFB300).copy(alpha = 0.5f),
                            radius = innerR,
                            center = cursor,
                            style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                        )
                    }
                }
            }
        }

        // HUD informativo superior
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xE6121212),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Healing,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isSelectingManualSource) {
                            "Toca la imagen para fijar el origen manual"
                        } else if (toolMode == HealingToolMode.TAP) {
                            "Modo Toque: toca manchas o imperfecciones puntuales"
                        } else {
                            "Modo Pincel: arrastra sobre cables, rayones o detalles"
                        },
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }

            // Mensajes de advertencia / estado
            AnimatedVisibility(
                visible = statusMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                statusMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xDDFF7043),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Botón flotante para fijar origen manual (si está activado el modo manual)
        if (samplingMode == HealingSamplingMode.MANUAL) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        isSelectingManualSource = !isSelectingManualSource
                        statusMessage = if (isSelectingManualSource) "Toca la zona sana que servirá de fuente" else null
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSelectingManualSource) Color(0xFF00E5FF) else Color(0xCC1E1E1E),
                        contentColor = if (isSelectingManualSource) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isSelectingManualSource) "Tocando origen..." else "Fijar fuente",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
