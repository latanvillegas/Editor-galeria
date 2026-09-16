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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.EditOperation
import kotlin.math.hypot

enum class CloneMode {
    SELECT_ORIGIN,
    PAINT
}

@Composable
fun CloneStampInteractiveCanvas(
    bitmap: Bitmap?,
    cloneMode: CloneMode,
    onCloneModeChanged: (CloneMode) -> Unit,
    originNorm: Offset?,
    onOriginSelected: (Offset) -> Unit,
    stampRadius: Float,
    stampHardness: Float = 0.5f,
    stampOpacity: Float = 1.0f,
    stampFlow: Float = 1.0f,
    onApplyStamps: (List<EditOperation.CloneStampPoint>) -> Unit,
    onClearStamps: () -> Unit,
    stampsCount: Int,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Estado durante el trazo táctil de clonación
    var isDragging by remember { mutableStateOf(false) }
    var activeDestScreen by remember { mutableStateOf<Offset?>(null) }
    var activeSourceScreen by remember { mutableStateOf<Offset?>(null) }
    var firstDestNorm by remember { mutableStateOf<Offset?>(null) }
    var lastRecordedNorm by remember { mutableStateOf<Offset?>(null) }
    val currentStroke = remember { mutableStateListOf<EditOperation.CloneStampPoint>() }
    var warningMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && containerSize.width > 0 && containerSize.height > 0) {
            val imgW = bitmap.width.toFloat()
            val imgH = bitmap.height.toFloat()
            val imgRatio = imgW / imgH

            // Ajuste del área visible conservando relación de aspecto
            val padding = 32f
            val maxDisplayW = (containerSize.width - padding).coerceAtLeast(100f)
            val maxDisplayH = (containerSize.height - padding).coerceAtLeast(100f)

            val displayW: Float
            val displayH: Float
            if (maxDisplayW / maxDisplayH > imgRatio) {
                displayH = maxDisplayH
                displayW = displayH * imgRatio
            } else {
                displayW = maxDisplayW
                displayH = displayW / imgRatio
            }

            val density = LocalDensity.current
            val displayWDp = with(density) { displayW.toDp() }
            val displayHDp = with(density) { displayH.toDp() }

            val totalW = containerSize.width.toFloat()
            val totalH = containerSize.height.toFloat()
            val imgScreenLeft = (totalW - displayW) / 2f
            val imgScreenTop = (totalH - displayH) / 2f

            // Radio en píxeles de pantalla correspondiente al radio en píxeles de imagen
            val screenRadius = (stampRadius * (displayW / imgW)).coerceAtLeast(8f)

            // 1. Imagen base renderizada (con los parches del pipeline ya aplicados)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Canvas de Retoque Tampón",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(displayWDp, displayHDp)
                    .clip(RoundedCornerShape(4.dp))
            )

            // 2. Overlay interactivo Compose para toques táctiles y cursores
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(cloneMode, originNorm, stampRadius, stampHardness, stampOpacity, stampFlow, displayW, displayH, imgScreenLeft, imgScreenTop) {
                        if (cloneMode == CloneMode.SELECT_ORIGIN) {
                            // Modo "Elegir origen": un toque fija manualmente el punto de origen sin alterar píxeles
                            detectTapGestures { tapPos ->
                                val normX = ((tapPos.x - imgScreenLeft) / displayW).coerceIn(0f, 1f)
                                val normY = ((tapPos.y - imgScreenTop) / displayH).coerceIn(0f, 1f)
                                onOriginSelected(Offset(normX, normY))
                                warningMessage = null
                            }
                        } else {
                            // Modo "Clonar/Pintar"
                            if (originNorm == null) {
                                detectTapGestures {
                                    warningMessage = "Primero selecciona un origen"
                                }
                            } else {
                                detectDragGestures(
                                    onDragStart = { startPos ->
                                        warningMessage = null
                                        val destNormX = ((startPos.x - imgScreenLeft) / displayW).coerceIn(0f, 1f)
                                        val destNormY = ((startPos.y - imgScreenTop) / displayH).coerceIn(0f, 1f)
                                        val dest0 = Offset(destNormX, destNormY)
                                        firstDestNorm = dest0
                                        lastRecordedNorm = dest0
                                        isDragging = true

                                        val origin = originNorm
                                        // Offset inicial: offset = puntoOrigen - primerPuntoDestino
                                        // sourcePointActual = puntoDelTrazoActual - (primerPuntoDestino - puntoOrigen)
                                        val srcX = (dest0.x - (dest0.x - origin.x)).coerceIn(0f, 1f)
                                        val srcY = (dest0.y - (dest0.y - origin.y)).coerceIn(0f, 1f)

                                        activeDestScreen = startPos
                                        activeSourceScreen = Offset(
                                            imgScreenLeft + srcX * displayW,
                                            imgScreenTop + srcY * displayH
                                        )

                                        currentStroke.clear()
                                        currentStroke.add(
                                            EditOperation.CloneStampPoint(
                                                sourceX = srcX,
                                                sourceY = srcY,
                                                targetX = dest0.x,
                                                targetY = dest0.y,
                                                radius = stampRadius,
                                                hardness = stampHardness,
                                                opacity = stampOpacity,
                                                flow = stampFlow
                                            )
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val currPos = change.position
                                        val destNormX = ((currPos.x - imgScreenLeft) / displayW).coerceIn(0f, 1f)
                                        val destNormY = ((currPos.y - imgScreenTop) / displayH).coerceIn(0f, 1f)
                                        val currDest = Offset(destNormX, destNormY)

                                        val origin = originNorm
                                        val first = firstDestNorm ?: currDest

                                        // Muestreo con offset sincronizado en tiempo real
                                        val srcX = (currDest.x - (first.x - origin.x)).coerceIn(0f, 1f)
                                        val srcY = (currDest.y - (first.y - origin.y)).coerceIn(0f, 1f)

                                        activeDestScreen = currPos
                                        activeSourceScreen = Offset(
                                            imgScreenLeft + srcX * displayW,
                                            imgScreenTop + srcY * displayH
                                        )

                                        // Interpolación suave a lo largo del trazo
                                        val last = lastRecordedNorm ?: currDest
                                        val dx = (currDest.x - last.x) * displayW
                                        val dy = (currDest.y - last.y) * displayH
                                        val dist = hypot(dx, dy)
                                        val step = (screenRadius * 0.35f).coerceAtLeast(6f)

                                        if (dist >= step) {
                                            val steps = (dist / step).toInt()
                                            for (i in 1..steps) {
                                                val t = i.toFloat() / steps
                                                val interpDestX = last.x + (currDest.x - last.x) * t
                                                val interpDestY = last.y + (currDest.y - last.y) * t
                                                val interpSrcX = (interpDestX - (first.x - origin.x)).coerceIn(0f, 1f)
                                                val interpSrcY = (interpDestY - (first.y - origin.y)).coerceIn(0f, 1f)
                                                currentStroke.add(
                                                    EditOperation.CloneStampPoint(
                                                        sourceX = interpSrcX,
                                                        sourceY = interpSrcY,
                                                        targetX = interpDestX,
                                                        targetY = interpDestY,
                                                        radius = stampRadius,
                                                        hardness = stampHardness,
                                                        opacity = stampOpacity,
                                                        flow = stampFlow
                                                    )
                                                )
                                            }
                                            lastRecordedNorm = currDest
                                        }
                                    },
                                    onDragEnd = {
                                        if (currentStroke.isNotEmpty()) {
                                            onApplyStamps(currentStroke.toList())
                                            currentStroke.clear()
                                        }
                                        isDragging = false
                                        activeDestScreen = null
                                        activeSourceScreen = null
                                        firstDestNorm = null
                                        lastRecordedNorm = null
                                    },
                                    onDragCancel = {
                                        currentStroke.clear()
                                        isDragging = false
                                        activeDestScreen = null
                                        activeSourceScreen = null
                                        firstDestNorm = null
                                        lastRecordedNorm = null
                                    }
                                )
                            }
                        }
                    }
            ) {
                // Dibujo de cursores, cruz de origen y líneas sincronizadas en el Canvas de Compose
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 1. Dibujar el punto de origen seleccionado fijo si existe
                    if (originNorm != null) {
                        val originScreenX = imgScreenLeft + originNorm.x * displayW
                        val originScreenY = imgScreenTop + originNorm.y * displayH
                        val originCenter = Offset(originScreenX, originScreenY)

                        // Círculo de origen con radio exacto del tampón
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            radius = screenRadius,
                            center = originCenter
                        )
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = screenRadius,
                            center = originCenter,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )

                        // Cruz visible en el centro del origen
                        val crossArm = (screenRadius * 0.8f).coerceIn(12.dp.toPx(), 28.dp.toPx())
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(originCenter.x - crossArm, originCenter.y),
                            end = Offset(originCenter.x + crossArm, originCenter.y),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(originCenter.x, originCenter.y - crossArm),
                            end = Offset(originCenter.x, originCenter.y + crossArm),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = originCenter
                        )
                    }

                    // 2. Si se está arrastrando en Clonar/Pintar, dibujar cursor destino y origen dinámico
                    if (isDragging) {
                        val destCenter = activeDestScreen
                        val srcCenter = activeSourceScreen

                        if (destCenter != null && srcCenter != null) {
                            // Línea discontinua indicadora de enlace entre origen sincronizado y destino
                            drawLine(
                                color = Color.White.copy(alpha = 0.75f),
                                start = srcCenter,
                                end = destCenter,
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )

                            // Cursor del origen dinámico muestreado
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                                radius = screenRadius,
                                center = srcCenter
                            )
                            drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = screenRadius,
                                center = srcCenter,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                )
                            )
                            val srcArm = (screenRadius * 0.6f).coerceIn(10.dp.toPx(), 22.dp.toPx())
                            drawLine(
                                color = Color(0xFF00E5FF),
                                start = Offset(srcCenter.x - srcArm, srcCenter.y),
                                end = Offset(srcCenter.x + srcArm, srcCenter.y),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawLine(
                                color = Color(0xFF00E5FF),
                                start = Offset(srcCenter.x, srcCenter.y - srcArm),
                                end = Offset(srcCenter.x, srcCenter.y + srcArm),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Cursor en el destino donde pinta el dedo
                            drawCircle(
                                color = Color(0xFFFF9100).copy(alpha = 0.3f),
                                radius = screenRadius,
                                center = destCenter
                            )
                            drawCircle(
                                color = Color(0xFFFF9100),
                                radius = screenRadius,
                                center = destCenter,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5.dp.toPx(),
                                center = destCenter
                            )
                        }
                    }
                }
            }

            // 3. Barra flotante superior interactiva: Selector de modo y Estado textual
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Selector táctil de los dos modos obligatorios
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = cloneMode == CloneMode.SELECT_ORIGIN,
                            onClick = {
                                onCloneModeChanged(CloneMode.SELECT_ORIGIN)
                                warningMessage = null
                            },
                            label = { Text("1. Elegir origen", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.FilterTiltShift,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
                                selectedLabelColor = Color(0xFF00E5FF),
                                selectedLeadingIconColor = Color(0xFF00E5FF)
                            )
                        )

                        FilterChip(
                            selected = cloneMode == CloneMode.PAINT,
                            onClick = {
                                onCloneModeChanged(CloneMode.PAINT)
                                if (originNorm == null) {
                                    warningMessage = "Primero selecciona un origen"
                                }
                            },
                            label = { Text("2. Clonar/Pintar", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Brush,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9100).copy(alpha = 0.25f),
                                selectedLabelColor = Color(0xFFFF9100),
                                selectedLeadingIconColor = Color(0xFFFF9100)
                            )
                        )
                    }

                    // Indicador textual del estado actual
                    val statusText = when {
                        warningMessage != null -> warningMessage!!
                        cloneMode == CloneMode.SELECT_ORIGIN && originNorm == null -> "Toca la imagen para fijar el origen"
                        cloneMode == CloneMode.SELECT_ORIGIN && originNorm != null -> {
                            val pxX = (originNorm.x * imgW).toInt()
                            val pxY = (originNorm.y * imgH).toInt()
                            "Origen seleccionado. Cambia a Clonar para pintar."
                        }
                        cloneMode == CloneMode.PAINT && originNorm == null -> "Primero selecciona un origen"
                        cloneMode == CloneMode.PAINT && isDragging -> "Clonando trazo continuo..."
                        else -> "Arrastra para clonar desde el origen"
                    }

                    val statusColor = when {
                        warningMessage != null || (cloneMode == CloneMode.PAINT && originNorm == null) -> Color(0xFFFF5252)
                        originNorm != null -> Color(0xFF00E5FF)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(statusColor, CircleShape)
                        )
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 4. Badge inferior con info de radio y parches
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Radio: ${stampRadius.toInt()}px",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (stampsCount > 0) {
                        Text(
                            text = "• Parches: $stampsCount",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
