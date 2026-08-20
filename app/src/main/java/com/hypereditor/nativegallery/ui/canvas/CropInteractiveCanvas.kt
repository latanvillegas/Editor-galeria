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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

    // Active drag target: -1 = None, 0..3 = Corners (TL, TR, BL, BR), 4..7 = Edges (T, B, L, R), 8 = Inside Body (Move)
    var activeHandle by remember { mutableIntStateOf(-1) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A0D))
            .onSizeChanged { containerSize = it },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && containerSize.width > 0 && containerSize.height > 0) {
            val imgW = bitmap.width.toFloat()
            val imgH = bitmap.height.toFloat()
            val imgRatio = imgW / imgH

            // Display bounds for the full base image inside container with margin
            val padding = 40f
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

            // Sync aspect ratio initialization whenever ratio changes
            LaunchedEffect(cropState.aspectRatio, imgW, imgH) {
                cropState.resetCropRectForRatio(imgW, imgH)
                onCropTransformChanged(cropState.toCropTransform(displayW, displayH))
            }

            // Broadcast changes to pipeline whenever crop state coordinates update
            LaunchedEffect(
                cropState.cropLeftNorm,
                cropState.cropTopNorm,
                cropState.cropRightNorm,
                cropState.cropBottomNorm,
                cropState.rotation,
                cropState.flipHorizontal,
                cropState.flipVertical
            ) {
                onCropTransformChanged(cropState.toCropTransform(displayW, displayH))
            }

            // 1. Full Base Image Container
            Box(
                modifier = Modifier
                    .size(width = displayWDp, height = displayHDp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Imagen de Recorte Snapseed",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = if (cropState.flipHorizontal) -1f else 1f
                            scaleY = if (cropState.flipVertical) -1f else 1f
                            rotationZ = cropState.rotation
                        }
                )
            }

            // 2. Interactive Gesture Layer + Dark Vignette Overlay + Handles
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(cropState.aspectRatio, displayW, displayH, imgScreenLeft, imgScreenTop) {
                        detectTapGestures(
                            onDoubleTap = {
                                cropState.onDoubleTap()
                                onCropTransformChanged(cropState.toCropTransform(displayW, displayH))
                            }
                        )
                    }
                    .pointerInput(cropState.aspectRatio, displayW, displayH, imgScreenLeft, imgScreenTop) {
                        val touchRadius = 36.dp.toPx()
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val cropL = imgScreenLeft + cropState.cropLeftNorm * displayW
                                val cropT = imgScreenTop + cropState.cropTopNorm * displayH
                                val cropR = imgScreenLeft + cropState.cropRightNorm * displayW
                                val cropB = imgScreenTop + cropState.cropBottomNorm * displayH

                                val px = startOffset.x
                                val py = startOffset.y

                                // Check corners first (high priority)
                                val distTL = Math.hypot((px - cropL).toDouble(), (py - cropT).toDouble())
                                val distTR = Math.hypot((px - cropR).toDouble(), (py - cropT).toDouble())
                                val distBL = Math.hypot((px - cropL).toDouble(), (py - cropB).toDouble())
                                val distBR = Math.hypot((px - cropR).toDouble(), (py - cropB).toDouble())

                                when {
                                    distTL <= touchRadius -> activeHandle = 0
                                    distTR <= touchRadius -> activeHandle = 1
                                    distBL <= touchRadius -> activeHandle = 2
                                    distBR <= touchRadius -> activeHandle = 3
                                    // Check horizontal edges (Top & Bottom)
                                    px in (cropL + 10f)..(cropR - 10f) && Math.abs(py - cropT) <= touchRadius -> activeHandle = 4
                                    px in (cropL + 10f)..(cropR - 10f) && Math.abs(py - cropB) <= touchRadius -> activeHandle = 5
                                    // Check vertical edges (Left & Right)
                                    py in (cropT + 10f)..(cropB - 10f) && Math.abs(px - cropL) <= touchRadius -> activeHandle = 6
                                    py in (cropT + 10f)..(cropB - 10f) && Math.abs(px - cropR) <= touchRadius -> activeHandle = 7
                                    // Inside rectangle (Body Move)
                                    px in cropL..cropR && py in cropT..cropB -> activeHandle = 8
                                    else -> activeHandle = -1
                                }
                            },
                            onDragEnd = {
                                activeHandle = -1
                            },
                            onDragCancel = {
                                activeHandle = -1
                            },
                            onDrag = { _, dragAmount ->
                                if (activeHandle == -1) return@detectDragGestures

                                val deltaNormX = dragAmount.x / displayW
                                val deltaNormY = dragAmount.y / displayH

                                if (activeHandle == 8) {
                                    // Move entire selection rectangle
                                    cropState.moveCropRect(deltaNormX, deltaNormY)
                                } else {
                                    // Resize rectangle from handle
                                    cropState.resizeCropRect(
                                        handle = activeHandle,
                                        deltaNormX = deltaNormX,
                                        deltaNormY = deltaNormY,
                                        imgWidth = imgW,
                                        imgHeight = imgH
                                    )
                                }
                                onCropTransformChanged(cropState.toCropTransform(displayW, displayH))
                            }
                        )
                    }
            ) {
                val cropLeft = imgScreenLeft + cropState.cropLeftNorm * displayW
                val cropTop = imgScreenTop + cropState.cropTopNorm * displayH
                val cropRight = imgScreenLeft + cropState.cropRightNorm * displayW
                val cropBottom = imgScreenTop + cropState.cropBottomNorm * displayH
                val frameW = (cropRight - cropLeft).coerceAtLeast(1f)
                val frameH = (cropBottom - cropTop).coerceAtLeast(1f)

                // 1. Dark Vignette / Snapseed Mask outside the active crop rectangle
                val outerPath = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                }
                val innerPath = Path().apply {
                    addRect(Rect(cropLeft, cropTop, cropRight, cropBottom))
                }
                val maskPath = Path().apply {
                    op(outerPath, innerPath, androidx.compose.ui.graphics.PathOperation.Difference)
                }
                drawPath(
                    path = maskPath,
                    color = Color(0xD9050609)
                )

                // 2. High-contrast Frame Border
                drawRect(
                    color = Color.White.copy(alpha = 0.92f),
                    topLeft = Offset(cropLeft, cropTop),
                    size = Size(frameW, frameH),
                    style = Stroke(width = 1.6.dp.toPx())
                )

                // 3. Rule of Thirds Grid inside the crop aperture
                if (cropState.showRuleOfThirds || activeHandle != -1) {
                    val gridColor = Color.White.copy(alpha = if (activeHandle != -1) 0.55f else 0.35f)
                    val gridStroke = 1.0.dp.toPx()

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
                }

                // 4. Snapseed Corner Brackets (Thick accents at the 4 corners)
                val cornerLength = 20.dp.toPx().coerceAtMost(minOf(frameW, frameH) / 3f)
                val cornerStroke = 3.6.dp.toPx()
                val cornerColor = if (activeHandle in 0..3) Color(0xFF00E5FF) else Color(0xFF00ADB5)

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

                // 5. Edge Handles (Midpoint indicators for edge dragging)
                val edgeLength = 16.dp.toPx().coerceAtMost(minOf(frameW, frameH) / 4f)
                val edgeStroke = 2.8.dp.toPx()
                val edgeColor = Color.White.copy(alpha = 0.85f)

                // Top midpoint
                val midX = cropLeft + frameW / 2f
                val midY = cropTop + frameH / 2f
                drawLine(edgeColor, Offset(midX - edgeLength / 2f, cropTop), Offset(midX + edgeLength / 2f, cropTop), strokeWidth = edgeStroke)
                // Bottom midpoint
                drawLine(edgeColor, Offset(midX - edgeLength / 2f, cropBottom), Offset(midX + edgeLength / 2f, cropBottom), strokeWidth = edgeStroke)
                // Left midpoint
                drawLine(edgeColor, Offset(cropLeft, midY - edgeLength / 2f), Offset(cropLeft, midY + edgeLength / 2f), strokeWidth = edgeStroke)
                // Right midpoint
                drawLine(edgeColor, Offset(cropRight, midY - edgeLength / 2f), Offset(cropRight, midY + edgeLength / 2f), strokeWidth = edgeStroke)
            }

            // Floating Snapseed HUD / Feedback Pill
            AnimatedVisibility(
                visible = cropState.isModified,
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
                        val cropPercentW = ((cropState.cropRightNorm - cropState.cropLeftNorm) * 100).toInt()
                        val cropPercentH = ((cropState.cropBottomNorm - cropState.cropTopNorm) * 100).toInt()

                        Text(
                            text = "Área: ${cropPercentW}% × ${cropPercentH}%",
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "Modo: ${cropState.aspectRatio.displayName.split(" ").first()}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )

                        IconButton(
                            onClick = {
                                cropState.reset()
                                onCropTransformChanged(cropState.toCropTransform(displayW, displayH))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Restablecer recorte",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
