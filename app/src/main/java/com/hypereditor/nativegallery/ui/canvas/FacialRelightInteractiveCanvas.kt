package com.hypereditor.nativegallery.ui.canvas

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

@Composable
fun FacialRelightInteractiveCanvas(
    bitmap: Bitmap?,
    zones: List<EditOperation.FacialRelightZone>,
    selectedZoneType: EditOperation.FacialZoneType,
    onZoneSelected: (EditOperation.FacialZoneType) -> Unit,
    onZoneChanged: (EditOperation.FacialRelightZone) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

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
                contentDescription = "Facial Relight Canvas",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zones, selectedZoneType) {
                    detectTapGestures { tapOffset ->
                        val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)
                        if (imgW <= 0 || imgH <= 0) return@detectTapGestures

                        val normX = (tapOffset.x - imgLeft) / imgW
                        val normY = (tapOffset.y - imgTop) / imgH

                        // Buscar si tocó alguna zona
                        val tappedZone = zones.minByOrNull { z ->
                            hypot(z.centerXNorm - normX, z.centerYNorm - normY)
                        }
                        if (tappedZone != null) {
                            val distPx = hypot((tappedZone.centerXNorm - normX) * imgW, (tappedZone.centerYNorm - normY) * imgH)
                            if (distPx < 80f) {
                                onZoneSelected(tappedZone.zoneType)
                            }
                        }
                    }
                }
                .pointerInput(zones, selectedZoneType) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)
                        if (imgW <= 0 || imgH <= 0) return@detectDragGestures

                        val targetZone = zones.find { it.zoneType == selectedZoneType } ?: return@detectDragGestures
                        val newCx = (targetZone.centerXNorm + dragAmount.x / imgW).coerceIn(0.05f, 0.95f)
                        val newCy = (targetZone.centerYNorm + dragAmount.y / imgH).coerceIn(0.05f, 0.95f)
                        onZoneChanged(targetZone.copy(centerXNorm = newCx, centerYNorm = newCy))
                    }
                }
        ) {
            val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)
            if (imgW <= 0 || imgH <= 0) return@Canvas

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

            // Dibujar todas las zonas anatómicas faciales
            for (zone in zones) {
                val isSelected = zone.zoneType == selectedZoneType
                val centerPx = Offset(
                    imgLeft + zone.centerXNorm * imgW,
                    imgTop + zone.centerYNorm * imgH
                )
                val rxPx = (zone.radiusXNorm * imgW).coerceAtLeast(8f)
                val ryPx = (zone.radiusYNorm * imgH).coerceAtLeast(8f)

                val zoneColor = if (isSelected) Color(0xFF64B5F6) else Color(0x88FFFFFF)
                val strokeW = if (isSelected) 2.5f else 1.2f

                drawOval(
                    color = zoneColor,
                    topLeft = Offset(centerPx.x - rxPx, centerPx.y - ryPx),
                    size = Size(rxPx * 2f, ryPx * 2f),
                    style = Stroke(width = strokeW, pathEffect = if (!isSelected) dashEffect else null)
                )

                drawCircle(
                    color = zoneColor,
                    radius = if (isSelected) 6f else 3.5f,
                    center = centerPx
                )
            }
        }

        // HUD informativo superior
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
                    Icon(Icons.Default.Face, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp))
                    Text(
                        text = "Zona activa: ${selectedZoneType.name} (Toca o arrastra)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        }
    }
}
