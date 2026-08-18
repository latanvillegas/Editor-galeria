package com.hypereditor.nativegallery.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.ui.canvas.CanvasViewportState
import com.hypereditor.nativegallery.ui.canvas.editorCanvasGestures
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState
import com.hypereditor.nativegallery.ui.tools.ToolType

@Composable
fun HyperEditorScreen(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    onClose: () -> Unit
) {
    val viewportState = remember { CanvasViewportState() }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Cargando imagen...", color = Color.White)
            }
        }
        return
    }

    if (state.errorMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = state.errorMessage, color = Color.Red, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClose) { Text("Cerrar") }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HyperEditor Pro", color = Color.White, fontSize = 18.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onIntent(EditorIntent.Undo) }, enabled = state.canUndo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer",
                            tint = if (state.canUndo) Color.White else Color.Gray
                        )
                    }
                    IconButton(onClick = { onIntent(EditorIntent.Redo) }, enabled = state.canRedo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Rehacer",
                            tint = if (state.canRedo) Color.White else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2B2F38))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onIntent(EditorIntent.SetCompareOriginalMode(true))
                                        tryAwaitRelease()
                                        onIntent(EditorIntent.SetCompareOriginalMode(false))
                                    }
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Comparar", color = Color.White, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { onIntent(EditorIntent.SaveAndExport) },
                        enabled = !state.isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar", color = Color.Black)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(84.dp).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ToolButton(Icons.Default.AutoFixHigh, "Ajustes", state.activeToolType == ToolType.ADJUSTMENTS) { onIntent(EditorIntent.SelectTool(ToolType.ADJUSTMENTS)) }
                    ToolButton(Icons.Default.Crop, "Recorte", state.activeToolType == ToolType.CROP_TRANSFORM) { onIntent(EditorIntent.SelectTool(ToolType.CROP_TRANSFORM)) }
                    ToolButton(Icons.Default.ColorLens, "Filtros", state.activeToolType == ToolType.FILTERS) { onIntent(EditorIntent.SelectTool(ToolType.FILTERS)) }
                    ToolButton(Icons.Default.Layers, "Capas", state.activeToolType == ToolType.LAYERS) { onIntent(EditorIntent.SelectTool(ToolType.LAYERS)) }
                    ToolButton(Icons.Default.Brush, "Pincel", state.activeToolType == ToolType.BRUSH) { onIntent(EditorIntent.SelectTool(ToolType.BRUSH)) }
                    ToolButton(Icons.Default.TextFields, "Texto", state.activeToolType == ToolType.TEXT) { onIntent(EditorIntent.SelectTool(ToolType.TEXT)) }
                    ToolButton(Icons.Default.Edit, "Retoque", state.activeToolType == ToolType.RETOUCH) { onIntent(EditorIntent.SelectTool(ToolType.RETOUCH)) }
                }
            }

            val bitmapToDisplay = if (state.isComparingOriginal) state.originalBitmap else state.previewBitmap
            val points = remember { mutableStateListOf<Pair<Float, Float>>() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF0C0D10))
                    .then(
                        if (state.activeToolType == ToolType.BRUSH) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        points.clear()
                                        points.add(offset.x to offset.y)
                                    },
                                    onDrag = { change, _ ->
                                        points.add(change.position.x to change.position.y)
                                    },
                                    onDragEnd = {
                                        if (points.size > 1) {
                                            onIntent(
                                                EditorIntent.AddBrushStroke(
                                                    EditOperation.BrushDraw(
                                                        points = points.toList(),
                                                        colorInt = AndroidColor.CYAN,
                                                        strokeWidth = 14f
                                                    )
                                                )
                                            )
                                        }
                                        points.clear()
                                    }
                                )
                            }
                        } else {
                            Modifier.editorCanvasGestures(viewportState)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (bitmapToDisplay != null) {
                    Image(
                        bitmap = bitmapToDisplay.asImageBitmap(),
                        contentDescription = "Canvas",
                        modifier = Modifier.graphicsLayer {
                            scaleX = viewportState.zoom
                            scaleY = viewportState.zoom
                            translationX = viewportState.panX
                            translationY = viewportState.panY
                            rotationZ = viewportState.rotationDegrees
                        }
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(320.dp).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (state.activeToolType) {
                        ToolType.ADJUSTMENTS -> {
                            val adj = state.document?.adjustments ?: EditOperation.Adjustments()
                            Text("Ajustes", color = Color.White, fontSize = 16.sp)
                            ControlSlider("Brillo", adj.brightness, -1f, 1f) { onIntent(EditorIntent.UpdateAdjustments(adj.copy(brightness = it))) }
                            ControlSlider("Contraste", adj.contrast, 0f, 2f) { onIntent(EditorIntent.UpdateAdjustments(adj.copy(contrast = it))) }
                            ControlSlider("Saturación", adj.saturation, 0f, 2f) { onIntent(EditorIntent.UpdateAdjustments(adj.copy(saturation = it))) }
                            ControlSlider("Exposición", adj.exposure, -2f, 2f) { onIntent(EditorIntent.UpdateAdjustments(adj.copy(exposure = it))) }
                            ControlSlider("Temperatura", adj.temperature, -1f, 1f) { onIntent(EditorIntent.UpdateAdjustments(adj.copy(temperature = it))) }
                            ControlSlider("Tinte", adj.tint, -1f, 1f) { onIntent(EditorIntent.UpdateAdjustments(adj.copy(tint = it))) }
                            ControlSlider("Viñeta", adj.vignette, 0f, 1f) { onIntent(EditorIntent.UpdateAdjustments(adj.copy(vignette = it))) }
                            ControlSlider("Grano", adj.grain, 0f, 1f) { onIntent(EditorIntent.UpdateAdjustments(adj.copy(grain = it))) }
                        }
                        ToolType.CROP_TRANSFORM -> {
                            val tr = state.document?.cropTransform ?: EditOperation.CropTransform()
                            Text("Transformar & Recortar", color = Color.White, fontSize = 16.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onIntent(EditorIntent.Rotate90Clockwise) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("90°", color = Color.White)
                                }
                                Button(
                                    onClick = { onIntent(EditorIntent.ToggleFlipHorizontal) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.Default.Flip, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Flip H", color = Color.White)
                                }
                            }
                            ControlSlider("Enderezar", tr.fineStraightenAngle, -45f, 45f) { onIntent(EditorIntent.UpdateStraightenAngle(it)) }
                            ControlSlider("Perspectiva H", tr.perspectiveHorizontal, -1f, 1f) { onIntent(EditorIntent.UpdatePerspective(it, tr.perspectiveVertical)) }
                        }
                        ToolType.FILTERS -> {
                            Text("Filtros Pro", color = Color.White, fontSize = 16.sp)
                            val presets = listOf("NONE" to "Original", "BW" to "B&N", "SEPIA" to "Sepia", "VIVID" to "Vívido", "COLD" to "Frío", "WARM" to "Cálido")
                            presets.forEach { (id, name) ->
                                Button(
                                    onClick = { onIntent(EditorIntent.ApplyPresetFilter(id, 1.0f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(name, color = Color.White)
                                }
                            }
                        }
                        ToolType.LAYERS -> {
                            Text("Capas", color = Color.White, fontSize = 16.sp)
                            Button(
                                onClick = { onIntent(EditorIntent.AddLayer("Capa ${(state.document?.layers?.size ?: 0) + 1}")) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("+ Añadir Capa")
                            }
                            state.document?.layers?.forEach { layer ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(layer.name, color = Color.White)
                                    IconButton(onClick = { onIntent(EditorIntent.ToggleLayerVisibility(layer.id)) }) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, tint = if (layer.isVisible) Color.White else Color.Gray)
                                    }
                                }
                            }
                        }
                        ToolType.BRUSH -> {
                            Text("Pincel", color = Color.White, fontSize = 16.sp)
                            Text("Dibuja directamente sobre el canvas con el dedo o stylus.", color = Color.Gray, fontSize = 13.sp)
                        }
                        ToolType.TEXT -> {
                            Text("Texto", color = Color.White, fontSize = 16.sp)
                            Button(
                                onClick = {
                                    onIntent(EditorIntent.AddTextOverlay(EditOperation.TextOverlay(text = "Texto", posX = 150f, posY = 150f)))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Insertar Texto")
                            }
                        }
                        ToolType.RETOUCH -> {
                            Text("Retoque (Clonación)", color = Color.White, fontSize = 16.sp)
                            Text("Toca y arrastra para corregir detalles de la imagen.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun ControlSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.LightGray, fontSize = 13.sp)
            Text(String.format("%.2f", value), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}
