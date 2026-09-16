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

enum class PatchInteractionStep {
    SELECT_TARGET, // Paso 1: Definir o tocar imperfección
    DRAG_TO_SOURCE // Paso 2: Arrastrar a zona de muestreo limpia
}

@Composable
fun PatchInteractiveCanvas(
    bitmap: Bitmap?,
    radius: Float,
    feather: Float,
    strength: Float,
    onApplyPatch: (EditOperation.PatchOperation) -> Unit,
    patchesCount: Int,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var currentStep by remember { mutableStateOf(PatchInteractionStep.SELECT_TARGET) }

    var targetPointNorm by remember { mutableStateOf<Offset?>(null) }
    var currentSourceNorm by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { canvasSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Patch Tool Canvas",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Overlay de interacción táctil
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(radius, feather, strength, currentStep, targetPointNorm) {
                    detectTapGestures { tapOffset ->
                        val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)
                        if (imgW <= 0 || imgH <= 0) return@detectTapGestures

                        if (tapOffset.x in imgLeft..(imgLeft + imgW) && tapOffset.y in imgTop..(imgTop + imgH)) {
                            val normX = ((tapOffset.x - imgLeft) / imgW).coerceIn(0f, 1f)
                            val normY = ((tapOffset.y - imgTop) / imgH).coerceIn(0f, 1f)

                            if (targetPointNorm == null || currentStep == PatchInteractionStep.SELECT_TARGET) {
                                targetPointNorm = Offset(normX, normY)
                                currentStep = PatchInteractionStep.DRAG_TO_SOURCE
                            } else {
                                // Aplicar parche con fuente en el punto tocado
                                val target = targetPointNorm!!
                                val radNorm = (radius / minOf(imgW, imgH)).coerceIn(0.02f, 0.35f)
                                onApplyPatch(
                                    EditOperation.PatchOperation(
                                        targetCenterXNorm = target.x,
                                        targetCenterYNorm = target.y,
                                        sourceCenterXNorm = normX,
                                        sourceCenterYNorm = normY,
                                        radiusNorm = radNorm,
                                        feather = feather,
                                        strength = strength
                                    )
                                )
                                targetPointNorm = null
                                currentSourceNorm = null
                                currentStep = PatchInteractionStep.SELECT_TARGET
                            }
                        }
                    }
                }
                .pointerInput(radius, feather, strength, currentStep, targetPointNorm) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)
                            if (imgW <= 0 || imgH <= 0) return@detectDragGestures

                            val normX = ((startOffset.x - imgLeft) / imgW).coerceIn(0f, 1f)
                            val normY = ((startOffset.y - imgTop) / imgH).coerceIn(0f, 1f)

                            if (targetPointNorm == null) {
                                targetPointNorm = Offset(normX, normY)
                                currentSourceNorm = Offset(normX, normY)
                                currentStep = PatchInteractionStep.DRAG_TO_SOURCE
                            } else {
                                currentSourceNorm = Offset(normX, normY)
                            }
                            isDragging = true
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)
                            if (imgW <= 0 || imgH <= 0) return@detectDragGestures

                            val normX = ((change.position.x - imgLeft) / imgW).coerceIn(0f, 1f)
                            val normY = ((change.position.y - imgTop) / imgH).coerceIn(0f, 1f)
                            currentSourceNorm = Offset(normX, normY)
                        },
                        onDragEnd = {
                            isDragging = false
                            val target = targetPointNorm
                            val source = currentSourceNorm
                            val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)

                            if (target != null && source != null && imgW > 0 && imgH > 0) {
                                val distPx = hypot((source.x - target.x) * imgW, (source.y - target.y) * imgH)
                                if (distPx > 8f) { // Arrastre válido con desplazamiento
                                    val radNorm = (radius / minOf(imgW, imgH)).coerceIn(0.02f, 0.35f)
                                    onApplyPatch(
                                        EditOperation.PatchOperation(
                                            targetCenterXNorm = target.x,
                                            targetCenterYNorm = target.y,
                                            sourceCenterXNorm = source.x,
                                            sourceCenterYNorm = source.y,
                                            radiusNorm = radNorm,
                                            feather = feather,
                                            strength = strength
                                        )
                                    )
                                    targetPointNorm = null
                                    currentSourceNorm = null
                                    currentStep = PatchInteractionStep.SELECT_TARGET
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)
            if (imgW <= 0 || imgH <= 0) return@Canvas

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            // 1. Dibujar área Destino (Defecto)
            targetPointNorm?.let { target ->
                val targetPx = Offset(imgLeft + target.x * imgW, imgTop + target.y * imgH)

                // Círculo principal destino
                drawCircle(
                    color = Color(0xFFFF5252),
                    radius = radius,
                    center = targetPx,
                    style = Stroke(width = 2.5f, pathEffect = dashEffect)
                )

                // Anillo de feather interno
                val innerFeatherRadius = radius * (1f - feather)
                if (innerFeatherRadius > 4f) {
                    drawCircle(
                        color = Color(0xFFFF8A80).copy(alpha = 0.6f),
                        radius = innerFeatherRadius,
                        center = targetPx,
                        style = Stroke(width = 1.2f)
                    )
                }

                // Cruz central
                drawLine(
                    color = Color(0xFFFF5252),
                    start = Offset(targetPx.x - 8f, targetPx.y),
                    end = Offset(targetPx.x + 8f, targetPx.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0xFFFF5252),
                    start = Offset(targetPx.x, targetPx.y - 8f),
                    end = Offset(targetPx.x, targetPx.y + 8f),
                    strokeWidth = 2f
                )

                // 2. Dibujar área Fuente (Origen Limpio) y línea conectora
                currentSourceNorm?.let { source ->
                    val sourcePx = Offset(imgLeft + source.x * imgW, imgTop + source.y * imgH)

                    // Línea directriz entre destino y fuente
                    drawLine(
                        color = Color(0xFF00E676).copy(alpha = 0.85f),
                        start = targetPx,
                        end = sourcePx,
                        strokeWidth = 2f,
                        pathEffect = dashEffect
                    )

                    // Círculo fuente limpia
                    drawCircle(
                        color = Color(0xFF00E676),
                        radius = radius,
                        center = sourcePx,
                        style = Stroke(width = 2.5f)
                    )

                    // Cruz de la fuente
                    drawLine(
                        color = Color(0xFF00E676),
                        start = Offset(sourcePx.x - 8f, sourcePx.y),
                        end = Offset(sourcePx.x + 8f, sourcePx.y),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF00E676),
                        start = Offset(sourcePx.x, sourcePx.y - 8f),
                        end = Offset(sourcePx.x, sourcePx.y + 8f),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // HUD flotante de guía superior
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD121212),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (targetPointNorm == null) Color(0xFFFF5252) else Color(0xFF00E676)
                            )
                    )
                    Text(
                        text = if (targetPointNorm == null) {
                            "Paso 1: Toca o rodea la imperfección"
                        } else {
                            "Paso 2: Arrastra a la zona limpia para muestrear"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )

                    if (targetPointNorm != null) {
                        IconButton(
                            onClick = {
                                targetPointNorm = null
                                currentSourceNorm = null
                                currentStep = PatchInteractionStep.SELECT_TARGET
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
