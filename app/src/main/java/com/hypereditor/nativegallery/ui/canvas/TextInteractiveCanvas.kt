package com.hypereditor.nativegallery.ui.canvas

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.EditOperation
import java.io.File
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun TextInteractiveCanvas(
    bitmap: Bitmap?,
    textOverlays: List<EditOperation.TextOverlay>,
    selectedTextId: String?,
    onSelectText: (String?) -> Unit,
    onUpdateText: (EditOperation.TextOverlay) -> Unit,
    onDeleteText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val (imgLeft, imgTop, imgW, imgH) = remember(containerSize, bitmap) {
        computeImageBounds(containerSize, bitmap)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures {
                    // Tocar fuera de cualquier texto deselecciona
                    onSelectText(null)
                }
            }
    ) {
        if (bitmap != null && imgW > 0f && imgH > 0f) {
            // Capa base de la imagen
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Vista Previa de Imagen",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset {
                        IntOffset(imgLeft.roundToInt(), imgTop.roundToInt())
                    }
                    .size(
                        width = with(density) { imgW.toDp() },
                        height = with(density) { imgH.toDp() }
                    )
                    .clip(RoundedCornerShape(4.dp))
            )

            // Capas de texto interactivas
            val minDim = minOf(imgW, imgH)
            val fontScale = minDim / 1000f

            for (textItem in textOverlays) {
                val isSelected = (textItem.id == selectedTextId)
                Key(textItem.id) {
                    InteractiveTextLayer(
                        textItem = textItem,
                        isSelected = isSelected,
                        imgLeft = imgLeft,
                        imgTop = imgTop,
                        imgW = imgW,
                        imgH = imgH,
                        fontScale = fontScale,
                        onSelect = { onSelectText(textItem.id) },
                        onCommitUpdate = { updated -> onUpdateText(updated) },
                        onDelete = { onDeleteText(textItem.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveTextLayer(
    textItem: EditOperation.TextOverlay,
    isSelected: Boolean,
    imgLeft: Float,
    imgTop: Float,
    imgW: Float,
    imgH: Float,
    fontScale: Float,
    onSelect: () -> Unit,
    onCommitUpdate: (EditOperation.TextOverlay) -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current

    // Estado local durante el arrastre/transformación interactiva
    var livePosX by remember(textItem.id, textItem.posX) { mutableFloatStateOf(textItem.posX) }
    var livePosY by remember(textItem.id, textItem.posY) { mutableFloatStateOf(textItem.posY) }
    var liveScale by remember(textItem.id, textItem.scale) { mutableFloatStateOf(textItem.scale) }
    var liveRotation by remember(textItem.id, textItem.rotationDegrees) { mutableFloatStateOf(textItem.rotationDegrees) }

    // Tipografía
    val customFontFamily = remember(textItem.customFontPath) {
        if (!textItem.customFontPath.isNullOrBlank()) {
            try {
                val f = File(textItem.customFontPath)
                if (f.exists() && f.canRead()) {
                    val tf = Typeface.createFromFile(f)
                    if (tf != null) FontFamily(androidx.compose.ui.text.font.Typeface(tf)) else null
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val effectiveFontFamily = customFontFamily ?: when (textItem.fontFamilyName.uppercase()) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        "CURSIVE" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }

    val effectiveSp = (textItem.textSize * liveScale * fontScale).coerceAtLeast(10f).sp
    val textColor = Color(textItem.colorInt).copy(alpha = textItem.opacity.coerceIn(0f, 1f))

    // Posición central en pantalla
    val screenCenterX = imgLeft + (livePosX * imgW)
    val screenCenterY = imgTop + (livePosY * imgH)

    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val halfWidth = (boxSize.width / 2f)
    val halfHeight = (boxSize.height / 2f)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (screenCenterX - halfWidth).roundToInt(),
                    (screenCenterY - halfHeight).roundToInt()
                )
            }
            .onSizeChanged { boxSize = it }
            .graphicsLayer {
                rotationZ = liveRotation
            }
    ) {
        // Contenedor del texto con detección de gestos
        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(if (isSelected) 16.dp else 4.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                    } else Modifier
                )
                .pointerInput(textItem.id, isSelected) {
                    if (!isSelected) {
                        detectTapGestures {
                            onSelect()
                        }
                    } else {
                        // Soporta arrastre con un dedo y transformación con dos dedos (zoom/rotación)
                        detectTransformGestures { _, pan, zoom, rotation ->
                            // 1. Mover
                            val deltaXNorm = pan.x / imgW
                            val deltaYNorm = pan.y / imgH
                            livePosX = (livePosX + deltaXNorm).coerceIn(0.02f, 0.98f)
                            livePosY = (livePosY + deltaYNorm).coerceIn(0.02f, 0.98f)

                            // 2. Escalar (zoom)
                            if (zoom != 1.0f) {
                                liveScale = (liveScale * zoom).coerceIn(0.2f, 5.0f)
                            }

                            // 3. Rotar
                            if (rotation != 0.0f) {
                                liveRotation = (liveRotation + rotation) % 360f
                            }
                        }
                    }
                }
                .pointerInput(textItem.id, isSelected) {
                    if (isSelected) {
                        // Detectar cuando se levanta el dedo para guardar atómicamente en Undo/Redo
                        detectDragGestures(
                            onDragEnd = {
                                onCommitUpdate(
                                    textItem.copy(
                                        posX = livePosX,
                                        posY = livePosY,
                                        scale = liveScale,
                                        rotationDegrees = liveRotation
                                    )
                                )
                            },
                            onDrag = { _, dragAmount ->
                                val deltaXNorm = dragAmount.x / imgW
                                val deltaYNorm = dragAmount.y / imgH
                                livePosX = (livePosX + deltaXNorm).coerceIn(0.02f, 0.98f)
                                livePosY = (livePosY + deltaYNorm).coerceIn(0.02f, 0.98f)
                            }
                        )
                    }
                }
        ) {
            Text(
                text = textItem.text,
                fontSize = effectiveSp,
                color = textColor,
                fontFamily = effectiveFontFamily,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Handles de edición (solo cuando está seleccionado)
        if (isSelected && boxSize.width > 0 && boxSize.height > 0) {
            // Handle de rotación superior
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-28).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .pointerInput(textItem.id) {
                        detectDragGestures(
                            onDragEnd = {
                                onCommitUpdate(
                                    textItem.copy(
                                        posX = livePosX,
                                        posY = livePosY,
                                        scale = liveScale,
                                        rotationDegrees = liveRotation
                                    )
                                )
                            },
                            onDrag = { change, _ ->
                                val touchPos = change.position
                                // Calcular ángulo respecto al centro
                                val rad = atan2(
                                    (touchPos.y - halfHeight).toDouble(),
                                    (touchPos.x - halfWidth).toDouble()
                                )
                                val deg = Math.toDegrees(rad).toFloat() + 90f
                                liveRotation = (deg % 360f + 360f) % 360f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.RotateRight,
                    contentDescription = "Rotar texto",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Handle de eliminar (esquina superior derecha)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar texto",
                    tint = Color.WHITE,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Handle de escala (esquina inferior derecha)
            var initialDragHypot by remember { mutableFloatStateOf(1f) }
            var baseScaleOnDragStart by remember { mutableFloatStateOf(liveScale) }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(textItem.id) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                initialDragHypot = hypot(offset.x, offset.y).coerceAtLeast(10f)
                                baseScaleOnDragStart = liveScale
                            },
                            onDragEnd = {
                                onCommitUpdate(
                                    textItem.copy(
                                        posX = livePosX,
                                        posY = livePosY,
                                        scale = liveScale,
                                        rotationDegrees = liveRotation
                                    )
                                )
                            },
                            onDrag = { change, dragAmount ->
                                val currentHypot = hypot(change.position.x, change.position.y)
                                val ratio = (currentHypot / initialDragHypot).coerceIn(0.2f, 5.0f)
                                liveScale = (baseScaleOnDragStart * ratio).coerceIn(0.2f, 5.0f)
                            }
                        )
                    }
            )
        }
    }
}
