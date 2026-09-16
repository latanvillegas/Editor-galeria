package com.hypereditor.nativegallery.ui.canvas

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.EditOperation
import kotlin.math.hypot

@Composable
fun PortraitLightInteractiveCanvas(
    bitmap: Bitmap?,
    portraitLight: EditOperation.PortraitLight,
    onLightChanged: (EditOperation.PortraitLight) -> Unit,
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
                contentDescription = "Portrait Light Canvas",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Overlay interactivo para arrastrar el foco de luz
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(portraitLight) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)
                        if (imgW <= 0 || imgH <= 0) return@detectDragGestures

                        val newCx = (portraitLight.centerXNorm + dragAmount.x / imgW).coerceIn(0.05f, 0.95f)
                        val newCy = (portraitLight.centerYNorm + dragAmount.y / imgH).coerceIn(0.05f, 0.95f)
                        onLightChanged(portraitLight.copy(centerXNorm = newCx, centerYNorm = newCy))
                    }
                }
        ) {
            val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)
            if (imgW <= 0 || imgH <= 0) return@Canvas

            val centerPx = Offset(
                imgLeft + portraitLight.centerXNorm * imgW,
                imgTop + portraitLight.centerYNorm * imgH
            )
            val rxPx = (portraitLight.radiusXNorm * imgW).coerceAtLeast(12f)
            val ryPx = (portraitLight.radiusYNorm * imgH).coerceAtLeast(12f)

            rotate(portraitLight.rotationDegrees, pivot = centerPx) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

                // Elipse exterior
                drawOval(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(centerPx.x - rxPx, centerPx.y - ryPx),
                    size = Size(rxPx * 2f, ryPx * 2f),
                    style = Stroke(width = 2.5f, pathEffect = dashEffect)
                )

                // Elipse de feather interior
                val innerCutoff = (1f - portraitLight.feather).coerceAtLeast(0.05f)
                val inRx = rxPx * innerCutoff
                val inRy = ryPx * innerCutoff
                drawOval(
                    color = Color(0xFFFFECB3).copy(alpha = 0.6f),
                    topLeft = Offset(centerPx.x - inRx, centerPx.y - inRy),
                    size = Size(inRx * 2f, inRy * 2f),
                    style = Stroke(width = 1.2f)
                )

                // Punto central de arrastre
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = 8f,
                    center = centerPx
                )
                drawCircle(
                    color = Color.Black,
                    radius = 4f,
                    center = centerPx
                )
            }
        }

        // HUD de orientación
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
                    Icon(Icons.Default.LightMode, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                    Text(
                        text = "Arrastra para posicionar el haz de luz sobre el sujeto",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        }
    }
}
