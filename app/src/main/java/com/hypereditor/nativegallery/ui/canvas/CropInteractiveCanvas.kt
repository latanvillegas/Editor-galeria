package com.hypereditor.nativegallery.ui.canvas

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.CropAspectRatio
import com.hypereditor.nativegallery.domain.model.EditOperation

@Composable
fun CropInteractiveCanvas(
    bitmap: Bitmap?,
    cropState: CropUiState,
    onCropTransformChanged: (EditOperation.CropTransform) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B0E))
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && containerSize.width > 0 && containerSize.height > 0) {
            val originalRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val targetRatio = when (cropState.aspectRatio) {
                CropAspectRatio.ORIGINAL -> originalRatio
                else -> cropState.aspectRatio.getCalculatedRatio(bitmap.width, bitmap.height)
            }

            // Animate target ratio change smoothly
            val animatedRatio by animateFloatAsState(
                targetValue = targetRatio,
                animationSpec = tween(durationMillis = 280),
                label = "CropAspectRatioAnimation"
            )

            val maxAvailW = (containerSize.width - 48).toFloat().coerceAtLeast(100f)
            val maxAvailH = (containerSize.height - 48).toFloat().coerceAtLeast(100f)

            var frameW: Float
            var frameH: Float

            if (maxAvailW / maxAvailH > animatedRatio) {
                frameH = maxAvailH
                frameW = frameH * animatedRatio
            } else {
                frameW = maxAvailW
                frameH = frameW / animatedRatio
            }

            // Outer Canvas for darkened backdrop, aperture, rule of thirds, and corner brackets
            Box(
                modifier = Modifier
                    .size(
                        width = (frameW / containerSize.width.toFloat() * 100).let { frameW.dp },
                        height = (frameH / containerSize.height.toFloat() * 100).let { frameH.dp }
                    )
                    .width(androidx.compose.ui.platform.LocalDensity.current.run { frameW.toDp() })
                    .height(androidx.compose.ui.platform.LocalDensity.current.run { frameH.toDp() })
                    .clipToBounds()
                    .pointerInput(cropState.aspectRatio) {
                        detectTapGestures(
                            onDoubleTap = {
                                cropState.onDoubleTap()
                                onCropTransformChanged(cropState.toCropTransform(frameW, frameH))
                            }
                        )
                    }
                    .pointerInput(cropState.aspectRatio) {
                        detectTransformGestures { _, pan, zoom, rotationDelta ->
                            cropState.updateTransform(pan, zoom, rotationDelta)
                            onCropTransformChanged(cropState.toCropTransform(frameW, frameH))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Interactive Transform Image Layer
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Imagen de Recorte",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = cropState.scale * (if (cropState.flipHorizontal) -1f else 1f)
                            scaleY = cropState.scale * (if (cropState.flipVertical) -1f else 1f)
                            translationX = cropState.offsetX
                            translationY = cropState.offsetY
                            rotationZ = cropState.rotation
                        }
                )

                // Grid & Guides Overlay (Rule of Thirds + Framing Box)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // 1. Frame Border
                    drawRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset.Zero,
                        size = Size(w, h),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )

                    // 2. Rule of Thirds
                    if (cropState.showRuleOfThirds) {
                        val gridPaintColor = Color.White.copy(alpha = 0.35f)
                        val strokeW = 1.dp.toPx()

                        // Vertical lines
                        drawLine(gridPaintColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = strokeW)
                        drawLine(gridPaintColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = strokeW)

                        // Horizontal lines
                        drawLine(gridPaintColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = strokeW)
                        drawLine(gridPaintColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = strokeW)
                    }

                    // 3. Corner Brackets (Lightroom / Photoshop style)
                    val cornerLength = 20.dp.toPx()
                    val cornerStroke = 4.dp.toPx()
                    val cornerColor = Color(0xFF00ADB5)

                    // Top-Left
                    drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth = cornerStroke)
                    drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth = cornerStroke)

                    // Top-Right
                    drawLine(cornerColor, Offset(w - cornerLength, 0f), Offset(w, 0f), strokeWidth = cornerStroke)
                    drawLine(cornerColor, Offset(w, 0f), Offset(w, cornerLength), strokeWidth = cornerStroke)

                    // Bottom-Left
                    drawLine(cornerColor, Offset(0f, h), Offset(cornerLength, h), strokeWidth = cornerStroke)
                    drawLine(cornerColor, Offset(0f, h - cornerLength), Offset(0f, h), strokeWidth = cornerStroke)

                    // Bottom-Right
                    drawLine(cornerColor, Offset(w - cornerLength, h), Offset(w, h), strokeWidth = cornerStroke)
                    drawLine(cornerColor, Offset(w, h - cornerLength), Offset(w, h), strokeWidth = cornerStroke)
                }
            }

            // HUD / Status pill on bottom
            AnimatedVisibility(
                visible = cropState.isModified || cropState.scale > 1.05f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD181A20),
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Zoom: ${(cropState.scale * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "Aspecto: ${cropState.aspectRatio.displayName.split(" ").first()}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )

                        IconButton(
                            onClick = {
                                cropState.reset()
                                onCropTransformChanged(cropState.toCropTransform(frameW, frameH))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
