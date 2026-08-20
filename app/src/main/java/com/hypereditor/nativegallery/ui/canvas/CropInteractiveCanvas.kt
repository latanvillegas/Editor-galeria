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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
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
            .background(Color(0xFF090A0D))
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && containerSize.width > 0 && containerSize.height > 0) {
            val originalRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val targetRatio = when (cropState.aspectRatio) {
                CropAspectRatio.ORIGINAL -> originalRatio
                CropAspectRatio.FREE -> originalRatio
                else -> cropState.aspectRatio.getCalculatedRatio(bitmap.width, bitmap.height)
            }

            // Animate target ratio change smoothly (Snapseed smooth morph)
            val animatedRatio by animateFloatAsState(
                targetValue = targetRatio,
                animationSpec = tween(durationMillis = 260),
                label = "CropAspectRatioAnimation"
            )

            val paddingHorizontal = 48f
            val paddingVertical = 48f
            val maxAvailW = (containerSize.width - paddingHorizontal).coerceAtLeast(100f)
            val maxAvailH = (containerSize.height - paddingVertical).coerceAtLeast(100f)

            val frameW: Float
            val frameH: Float

            if (maxAvailW / maxAvailH > animatedRatio) {
                frameH = maxAvailH
                frameW = frameH * animatedRatio
            } else {
                frameW = maxAvailW
                frameH = frameW / animatedRatio
            }

            // Re-clamp whenever frame dimensions, aspect ratio, scale or rotation change
            LaunchedEffect(frameW, frameH, bitmap.width, bitmap.height, cropState.aspectRatio, cropState.scale, cropState.rotation) {
                cropState.clampToBounds(
                    frameWidth = frameW,
                    frameHeight = frameH,
                    imgWidth = bitmap.width.toFloat(),
                    imgHeight = bitmap.height.toFloat()
                )
                onCropTransformChanged(cropState.toCropTransform(frameW, frameH))
            }

            val density = LocalDensity.current
            val frameWDp = with(density) { frameW.toDp() }
            val frameHDp = with(density) { frameH.toDp() }

            // Container Box holding the interactive image inside the active framing aperture
            Box(
                modifier = Modifier
                    .size(width = frameWDp, height = frameHDp)
                    .clipToBounds()
                    .pointerInput(cropState.aspectRatio, frameW, frameH) {
                        detectTapGestures(
                            onDoubleTap = {
                                cropState.onDoubleTap()
                                cropState.clampToBounds(frameW, frameH, bitmap.width.toFloat(), bitmap.height.toFloat())
                                onCropTransformChanged(cropState.toCropTransform(frameW, frameH))
                            }
                        )
                    }
                    .pointerInput(cropState.aspectRatio, frameW, frameH) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            cropState.isInteracting = true
                            cropState.updateTransformWithBounds(
                                pan = pan,
                                zoomFactor = zoom,
                                frameWidth = frameW,
                                frameHeight = frameH,
                                imgWidth = bitmap.width.toFloat(),
                                imgHeight = bitmap.height.toFloat()
                            )
                            onCropTransformChanged(cropState.toCropTransform(frameW, frameH))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Interactive Transformed Image
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Imagen de Recorte Snapseed",
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
            }

            // Snapseed Darkened Mask Overlay + Professional Crop Aperture Lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val totalW = size.width
                val totalH = size.height

                val cropLeft = (totalW - frameW) / 2f
                val cropTop = (totalH - frameH) / 2f
                val cropRight = cropLeft + frameW
                val cropBottom = cropTop + frameH

                // 1. Dark Vignette / Snapseed Mask outside the crop aperture
                val outerPath = Path().apply {
                    addRect(Rect(0f, 0f, totalW, totalH))
                }
                val innerPath = Path().apply {
                    addRect(Rect(cropLeft, cropTop, cropRight, cropBottom))
                }
                val maskPath = Path().apply {
                    op(outerPath, innerPath, androidx.compose.ui.graphics.PathOperation.Difference)
                }
                drawPath(
                    path = maskPath,
                    color = Color(0xCC050608)
                )

                // 2. High-contrast Frame Border
                drawRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(cropLeft, cropTop),
                    size = Size(frameW, frameH),
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // 3. Rule of Thirds Grid (Always visible when active or while rotating/fine-straightening)
                val isFineRotating = Math.abs(cropState.rotation % 90f) > 0.05f
                if (cropState.showRuleOfThirds || isFineRotating) {
                    val gridColor = Color.White.copy(alpha = if (isFineRotating) 0.65f else 0.40f)
                    val gridStroke = (if (isFineRotating) 1.2.dp else 1.0.dp).toPx()

                    // Vertical lines
                    val x1 = cropLeft + frameW / 3f
                    val x2 = cropLeft + frameW * 2f / 3f
                    drawLine(gridColor, Offset(x1, cropTop), Offset(x1, cropBottom), strokeWidth = gridStroke)
                    drawLine(gridColor, Offset(x2, cropTop), Offset(x2, cropBottom), strokeWidth = gridStroke)

                    // Horizontal lines
                    val y1 = cropTop + frameH / 3f
                    val y2 = cropTop + frameH * 2f / 3f
                    drawLine(gridColor, Offset(cropLeft, y1), Offset(cropRight, y1), strokeWidth = gridStroke)
                    drawLine(gridColor, Offset(cropLeft, y2), Offset(cropRight, y2), strokeWidth = gridStroke)

                    // Dense Snapseed Straighten grid if fine rotating
                    if (isFineRotating) {
                        val denseGridColor = Color.White.copy(alpha = 0.22f)
                        val subStroke = 0.75.dp.toPx()
                        for (i in 1..5) {
                            if (i % 2 != 0) {
                                val subX = cropLeft + frameW * (i / 6f)
                                val subY = cropTop + frameH * (i / 6f)
                                drawLine(denseGridColor, Offset(subX, cropTop), Offset(subX, cropBottom), strokeWidth = subStroke)
                                drawLine(denseGridColor, Offset(cropLeft, subY), Offset(cropRight, subY), strokeWidth = subStroke)
                            }
                        }
                    }
                }

                // 4. Snapseed / Pro Corner Brackets (Thick accents at the 4 corners)
                val cornerLength = 22.dp.toPx()
                val cornerStroke = 3.8.dp.toPx()
                val cornerColor = Color(0xFF00ADB5)

                // Top-Left
                drawLine(cornerColor, Offset(cropLeft - 1f, cropTop), Offset(cropLeft + cornerLength, cropTop), strokeWidth = cornerStroke)
                drawLine(cornerColor, Offset(cropLeft, cropTop - 1f), Offset(cropLeft, cropTop + cornerLength), strokeWidth = cornerStroke)

                // Top-Right
                drawLine(cornerColor, Offset(cropRight - cornerLength, cropTop), Offset(cropRight + 1f, cropTop), strokeWidth = cornerStroke)
                drawLine(cornerColor, Offset(cropRight, cropTop - 1f), Offset(cropRight, cropTop + cornerLength), strokeWidth = cornerStroke)

                // Bottom-Left
                drawLine(cornerColor, Offset(cropLeft - 1f, cropBottom), Offset(cropLeft + cornerLength, cropBottom), strokeWidth = cornerStroke)
                drawLine(cornerColor, Offset(cropLeft, cropBottom - cornerLength), Offset(cropLeft, cropBottom + 1f), strokeWidth = cornerStroke)

                // Bottom-Right
                drawLine(cornerColor, Offset(cropRight - cornerLength, cropBottom), Offset(cropRight + 1f, cropBottom), strokeWidth = cornerStroke)
                drawLine(cornerColor, Offset(cropRight, cropBottom - cornerLength), Offset(cropRight, cropBottom + 1f), strokeWidth = cornerStroke)
            }

            // HUD / Floating Feedback Pill
            AnimatedVisibility(
                visible = cropState.isModified || cropState.scale > 1.02f || Math.abs(cropState.rotation) > 0.05f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xEE16181F),
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262933))
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
                            text = "Giro: ${(cropState.rotation * 10).toInt() / 10f}°",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "Proporción: ${cropState.aspectRatio.displayName.split(" ").first()}",
                            color = Color.LightGray,
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
                                contentDescription = "Reiniciar Recorte",
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
