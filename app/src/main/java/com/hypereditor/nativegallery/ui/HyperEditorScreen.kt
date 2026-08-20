package com.hypereditor.nativegallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.*
import com.hypereditor.nativegallery.ui.canvas.CropInteractiveCanvas
import com.hypereditor.nativegallery.ui.canvas.EditorCanvas
import com.hypereditor.nativegallery.ui.canvas.rememberCanvasViewportState
import com.hypereditor.nativegallery.ui.canvas.rememberCropUiState
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorSectionTab
import com.hypereditor.nativegallery.ui.state.EditorUiState

private data class FilterItem(val id: String, val name: String, val badgeColor: Color)

private val AVAILABLE_FILTERS = listOf(
    FilterItem("BW", "B&N", Color(0xFF8E8E93)),
    FilterItem("SEPIA", "Sepia", Color(0xFFC49A6C)),
    FilterItem("VIVID", "Vívido", Color(0xFFFF5252)),
    FilterItem("CINE", "Cine", Color(0xFF00ADB5)),
    FilterItem("WARM", "Cálido", Color(0xFFFFA726)),
    FilterItem("COLD", "Frío", Color(0xFF42A5F5)),
    FilterItem("DRAMATIC", "Dramático", Color(0xFFAB47BC)),
    FilterItem("NOIR", "Noir", Color(0xFF37474F))
)

private val BRUSH_PALETTE = listOf(
    Pair("Blanco", android.graphics.Color.WHITE),
    Pair("Amarillo", android.graphics.Color.YELLOW),
    Pair("Rojo", android.graphics.Color.RED),
    Pair("Cian", android.graphics.Color.CYAN),
    Pair("Verde", android.graphics.Color.GREEN),
    Pair("Negro", android.graphics.Color.BLACK)
)

@Composable
fun HyperEditorScreen(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    onClose: () -> Unit
) {
    val viewportState = rememberCanvasViewportState()
    val cropUiState = rememberCropUiState(state.document?.cropTransform ?: EditOperation.CropTransform())
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var showAddTextLayerDialog by remember { mutableStateOf(false) }
    var newTextLayerContent by remember { mutableStateOf("Texto de Capa") }
    var showAddStickerDialog by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Cargando imagen desde galería...", color = Color.White, fontSize = 14.sp)
            }
        }
        return
    }

    if (state.errorMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Error al abrir imagen", color = Color.White, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = state.errorMessage, color = Color(0xFFFF6B6B), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Regresar a la Galería", color = Color.White)
                    }
                }
            }
        }
        return
    }

    // Dialog para guardar preset del usuario
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Guardar como Preset", color = Color.White) },
            text = {
                Column {
                    Text("Guarda los ajustes y filtros actuales para reutilizarlos:", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        placeholder = { Text("Nombre del Preset (ej. Mi Estilo)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            onIntent(EditorIntent.SaveUserPreset(newPresetName))
                            newPresetName = ""
                            showSavePresetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog para agregar Capa de Texto
    if (showAddTextLayerDialog) {
        AlertDialog(
            onDismissRequest = { showAddTextLayerDialog = false },
            title = { Text("Nueva Capa de Texto", color = Color.White) },
            text = {
                Column {
                    Text("Ingresa el texto que deseas superponer como capa independiente:", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newTextLayerContent,
                        onValueChange = { newTextLayerContent = it },
                        placeholder = { Text("Escribe tu texto aquí") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTextLayerContent.isNotBlank()) {
                            onIntent(EditorIntent.AddTextLayer(newTextLayerContent))
                            newTextLayerContent = "Texto de Capa"
                            showAddTextLayerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Agregar Capa", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextLayerDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog para agregar Capa de Sticker
    if (showAddStickerDialog) {
        val stickers = listOf("⭐", "🔥", "❤️", "🚀", "💡", "👑", "✨", "📸", "⚡", "🎉", "💎", "👍")
        AlertDialog(
            onDismissRequest = { showAddStickerDialog = false },
            title = { Text("Seleccionar Sticker", color = Color.White) },
            text = {
                Column {
                    Text("Elige un sticker para agregar como capa:", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        stickers.take(6).forEach { emoji ->
                            TextButton(
                                onClick = {
                                    onIntent(EditorIntent.AddStickerLayer(emoji))
                                    showAddStickerDialog = false
                                }
                            ) {
                                Text(emoji, fontSize = 24.sp)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        stickers.drop(6).take(6).forEach { emoji ->
                            TextButton(
                                onClick = {
                                    onIntent(EditorIntent.AddStickerLayer(emoji))
                                    showAddStickerDialog = false
                                }
                            ) {
                                Text(emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddStickerDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TopBar / Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar y salir",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HyperEditor Pro", color = Color.White, fontSize = 17.sp)

                    state.originalBitmap?.let { bmp ->
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${bmp.width} × ${bmp.height} px",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Undo
                    IconButton(
                        onClick = { onIntent(EditorIntent.Undo) },
                        enabled = state.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer",
                            tint = if (state.canUndo) Color.White else Color.DarkGray
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { onIntent(EditorIntent.Redo) },
                        enabled = state.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Rehacer",
                            tint = if (state.canRedo) Color.White else Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Botón Comparar Original
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.isComparingOriginal) MaterialTheme.colorScheme.primary else Color(0xFF2B2F38))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onIntent(EditorIntent.SetCompareOriginalMode(true))
                                        tryAwaitRelease()
                                        onIntent(EditorIntent.SetCompareOriginalMode(false))
                                    }
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (state.isComparingOriginal) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isComparingOriginal) "ORIGINAL" else "Comparar",
                                color = if (state.isComparingOriginal) Color.Black else Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Reset Vista
                    if (viewportState.isModified) {
                        OutlinedButton(
                            onClick = { viewportState.reset() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Vista", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Guardar y Retornar
                    Button(
                        onClick = { onIntent(EditorIntent.SaveAndExport()) },
                        enabled = !state.isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardando...", color = Color.Black)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar", color = Color.Black)
                        }
                    }
                }
            }
        }

        // Main Editor Layout (Tablet Split: Canvas on Left + Modular Panel on Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val bitmapToDisplay = if (state.isComparingOriginal) {
                state.originalBitmap
            } else {
                state.previewBitmap ?: state.originalBitmap
            }

            // Viewport Canvas Area (Switches to Pro Interactive Crop in GEOMETRY_CROP tab)
            if (state.selectedTab == EditorSectionTab.GEOMETRY_CROP) {
                CropInteractiveCanvas(
                    bitmap = state.originalBitmap,
                    cropState = cropUiState,
                    onCropTransformChanged = { onIntent(EditorIntent.UpdateCropTransform(it)) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            } else {
                EditorCanvas(
                    bitmap = bitmapToDisplay,
                    viewportState = viewportState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Right Side: Modular Tools Panel with 6 Tabs
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .width(380.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Navigation Tabs
                    val currentTabIdx = when (state.selectedTab) {
                        EditorSectionTab.ADJUSTMENTS -> 0
                        EditorSectionTab.FILTERS_PRESETS -> 1
                        EditorSectionTab.MASKS_SELECTIONS -> 2
                        EditorSectionTab.CREATIVE_TOOLS -> 3
                        EditorSectionTab.LAYERS -> 4
                        EditorSectionTab.GEOMETRY_CROP -> 5
                    }

                    ScrollableTabRow(
                        selectedTabIndex = currentTabIdx,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 8.dp,
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
                    ) {
                        Tab(
                            selected = state.selectedTab == EditorSectionTab.ADJUSTMENTS,
                            onClick = { onIntent(EditorIntent.SelectTab(EditorSectionTab.ADJUSTMENTS)) },
                            text = { Text("Ajustes", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = state.selectedTab == EditorSectionTab.FILTERS_PRESETS,
                            onClick = { onIntent(EditorIntent.SelectTab(EditorSectionTab.FILTERS_PRESETS)) },
                            text = { Text("Filtros", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.FilterVintage, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = state.selectedTab == EditorSectionTab.MASKS_SELECTIONS,
                            onClick = { onIntent(EditorIntent.SelectTab(EditorSectionTab.MASKS_SELECTIONS)) },
                            text = { Text("Máscaras", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Highlight, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = state.selectedTab == EditorSectionTab.CREATIVE_TOOLS,
                            onClick = { onIntent(EditorIntent.SelectTab(EditorSectionTab.CREATIVE_TOOLS)) },
                            text = { Text("Retoque", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = state.selectedTab == EditorSectionTab.LAYERS,
                            onClick = { onIntent(EditorIntent.SelectTab(EditorSectionTab.LAYERS)) },
                            text = { Text("Capas", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = state.selectedTab == EditorSectionTab.GEOMETRY_CROP,
                            onClick = { onIntent(EditorIntent.SelectTab(EditorSectionTab.GEOMETRY_CROP)) },
                            text = { Text("Recorte", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    // Content based on Selected Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (state.selectedTab) {
                            EditorSectionTab.ADJUSTMENTS -> {
                                val adj = state.document?.adjustments ?: EditOperation.Adjustments()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Ajustes de Color", color = Color.White, fontSize = 15.sp)
                                    TextButton(
                                        onClick = {
                                            onIntent(EditorIntent.UpdateAdjustments(EditOperation.Adjustments()))
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Restablecer", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                }

                                AdjustmentSlider("Brillo", adj.brightness, -1.0f, 1.0f, 0.0f) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(brightness = it)))
                                }
                                AdjustmentSlider("Contraste", adj.contrast, 0.0f, 2.0f, 1.0f) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(contrast = it)))
                                }
                                AdjustmentSlider("Saturación", adj.saturation, 0.0f, 2.0f, 1.0f) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(saturation = it)))
                                }
                                AdjustmentSlider("Exposición", adj.exposure, -2.0f, 2.0f, 0.0f) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(exposure = it)))
                                }
                                AdjustmentSlider("Temperatura", adj.temperature, -1.0f, 1.0f, 0.0f) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(temperature = it)))
                                }
                                AdjustmentSlider("Tinte", adj.tint, -1.0f, 1.0f, 0.0f) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(tint = it)))
                                }
                            }

                            EditorSectionTab.FILTERS_PRESETS -> {
                                val activeFilter = state.document?.appliedFilter

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Filtros Predefinidos", color = Color.White, fontSize = 15.sp)
                                    if (activeFilter != null) {
                                        TextButton(
                                            onClick = { onIntent(EditorIntent.ClearFilter) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Quitar Filtro", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                                        }
                                    }
                                }

                                // Grid de Filtros
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val rows = AVAILABLE_FILTERS.chunked(4)
                                    for (row in rows) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (item in row) {
                                                val isSelected = activeFilter?.filterName.equals(item.id, ignoreCase = true)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .clickable {
                                                            onIntent(EditorIntent.ApplyFilter(item.id, activeFilter?.intensity ?: 1.0f))
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = item.name,
                                                            color = if (isSelected) Color.Black else Color.White,
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Slider de Intensidad de Filtro
                                if (activeFilter != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AdjustmentSlider(
                                        label = "Intensidad del Filtro",
                                        value = activeFilter.intensity,
                                        min = 0.0f,
                                        max = 1.0f,
                                        defaultValue = 1.0f,
                                        unitSuffix = "%",
                                        onValueChange = { onIntent(EditorIntent.UpdateFilterIntensity(it)) }
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                                // Sección de Presets del Usuario
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Presets Guardados", color = Color.White, fontSize = 15.sp)
                                    Button(
                                        onClick = { showSavePresetDialog = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Guardar Preset", color = Color.Black, fontSize = 11.sp)
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (preset in state.userPresets) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = preset.name, color = Color.White, fontSize = 13.sp)
                                                    Text(
                                                        text = if (preset.appliedFilter != null) "Filtro ${preset.appliedFilter.filterName} + Ajustes" else "Ajustes de Color",
                                                        color = Color.LightGray,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    OutlinedButton(
                                                        onClick = { onIntent(EditorIntent.ApplyUserPreset(preset)) },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("Aplicar", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    IconButton(
                                                        onClick = { onIntent(EditorIntent.DeleteUserPreset(preset.id)) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "Borrar preset",
                                                            tint = Color.Gray,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            EditorSectionTab.MASKS_SELECTIONS -> {
                                val masks = state.document?.masks ?: emptyList()

                                Text(text = "Máscaras y Ajustes Locales", color = Color.White, fontSize = 15.sp)

                                // Quick Add Mask Selection Tools
                                Text(text = "Crear Selección:", color = Color.LightGray, fontSize = 12.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MaskToolChip("Rect", Icons.Default.CropSquare, modifier = Modifier.weight(1f)) {
                                        onIntent(EditorIntent.AddMask(SelectionToolType.RECTANGLE))
                                    }
                                    MaskToolChip("Óvalo", Icons.Default.RadioButtonUnchecked, modifier = Modifier.weight(1f)) {
                                        onIntent(EditorIntent.AddMask(SelectionToolType.ELLIPSE))
                                    }
                                    MaskToolChip("Lazo", Icons.Default.Polyline, modifier = Modifier.weight(1f)) {
                                        onIntent(EditorIntent.AddMask(SelectionToolType.LASSO))
                                    }
                                    MaskToolChip("Pincel", Icons.Default.Brush, modifier = Modifier.weight(1f)) {
                                        onIntent(EditorIntent.AddMask(SelectionToolType.BRUSH))
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                                if (masks.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No hay máscaras activas.\nCrea una selección (Rect, Óvalo, Lazo o Pincel)\npara aplicar ajustes localizados.",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        masks.forEach { mask ->
                                            MaskCard(
                                                mask = mask,
                                                onToggleEnabled = { onIntent(EditorIntent.ToggleMaskEnabled(mask.id)) },
                                                onToggleInvert = { onIntent(EditorIntent.ToggleMaskInvert(mask.id)) },
                                                onFeatherChange = { onIntent(EditorIntent.UpdateMaskFeather(mask.id, it)) },
                                                onAdjustmentsChange = { onIntent(EditorIntent.UpdateMaskLocalAdjustments(mask.id, it)) },
                                                onDelete = { onIntent(EditorIntent.DeleteMask(mask.id)) }
                                            )
                                        }
                                    }
                                }
                            }

                            EditorSectionTab.CREATIVE_TOOLS -> {
                                var selectedCreativeTool by remember { mutableStateOf(0) } // 0: Pincel, 1: Texto, 2: Clone Stamp
                                var brushSize by remember { mutableFloatStateOf(24f) }
                                var brushColor by remember { mutableIntStateOf(android.graphics.Color.YELLOW) }
                                var brushOpacity by remember { mutableFloatStateOf(1.0f) }
                                var isEraserMode by remember { mutableStateOf(false) }

                                var newTextContent by remember { mutableStateOf("HyperEditor") }
                                var textSize by remember { mutableFloatStateOf(44f) }
                                var textFont by remember { mutableStateOf("SANS_SERIF") }

                                var stampRadius by remember { mutableFloatStateOf(45f) }

                                Text(text = "Herramientas Creativas y Retoque", color = Color.White, fontSize = 15.sp)

                                // Selector de sub-herramienta
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CreativeTabChip("Pincel", Icons.Default.Brush, selectedCreativeTool == 0, modifier = Modifier.weight(1f)) {
                                        selectedCreativeTool = 0
                                    }
                                    CreativeTabChip("Texto", Icons.Default.TextFields, selectedCreativeTool == 1, modifier = Modifier.weight(1f)) {
                                        selectedCreativeTool = 1
                                    }
                                    CreativeTabChip("Clonar", Icons.Default.AutoFixHigh, selectedCreativeTool == 2, modifier = Modifier.weight(1f)) {
                                        selectedCreativeTool = 2
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                                when (selectedCreativeTool) {
                                    0 -> {
                                        // Pincel y Borrador
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "Modo:", color = Color.LightGray, fontSize = 12.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = { isEraserMode = false },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (!isEraserMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        contentColor = if (!isEraserMode) Color.Black else Color.White
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Pincel", fontSize = 11.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { isEraserMode = true },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (isEraserMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        contentColor = if (isEraserMode) Color.Black else Color.White
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Borrador", fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        if (!isEraserMode) {
                                            Text(text = "Color de trazo:", color = Color.LightGray, fontSize = 12.sp)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                for ((name, cInt) in BRUSH_PALETTE) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(cInt))
                                                            .border(
                                                                width = if (brushColor == cInt) 2.dp else 1.dp,
                                                                color = if (brushColor == cInt) MaterialTheme.colorScheme.primary else Color.Gray,
                                                                shape = CircleShape
                                                            )
                                                            .clickable { brushColor = cInt }
                                                    )
                                                }
                                            }
                                        }

                                        AdjustmentSlider("Grosor del Trazo", brushSize, 4f, 80f, 24f, "px") {
                                            brushSize = it
                                        }

                                        AdjustmentSlider("Opacidad del Trazo", brushOpacity, 0.1f, 1f, 1f, "%") {
                                            brushOpacity = it
                                        }

                                        // Botón para trazar demostración / trazo rápido
                                        Button(
                                            onClick = {
                                                val startX = 0.2f + (Math.random().toFloat() * 0.2f)
                                                val startY = 0.3f + (Math.random().toFloat() * 0.4f)
                                                val stroke = EditOperation.BrushDraw(
                                                    points = listOf(
                                                        Pair(startX, startY),
                                                        Pair(startX + 0.25f, startY + 0.1f),
                                                        Pair(startX + 0.5f, startY - 0.05f)
                                                    ),
                                                    colorInt = brushColor,
                                                    strokeWidth = brushSize,
                                                    opacity = brushOpacity,
                                                    isEraser = isEraserMode
                                                )
                                                onIntent(EditorIntent.AddBrushStroke(stroke))
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(if (isEraserMode) Icons.Default.Delete else Icons.Default.Draw, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isEraserMode) "Aplicar Borrador" else "Añadir Trazo Pincel", color = Color.Black, fontSize = 12.sp)
                                        }

                                        if ((state.document?.brushStrokes?.size ?: 0) > 0) {
                                            TextButton(
                                                onClick = { onIntent(EditorIntent.ClearBrushStrokes) },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Limpiar Todos los Trazos", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    1 -> {
                                        // Texto Tipográfico
                                        Text(text = "Contenido del Texto:", color = Color.LightGray, fontSize = 12.sp)
                                        OutlinedTextField(
                                            value = newTextContent,
                                            onValueChange = { newTextContent = it },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )

                                        AdjustmentSlider("Tamaño de Fuente", textSize, 16f, 100f, 44f, "sp") {
                                            textSize = it
                                        }

                                        Text(text = "Tipografía:", color = Color.LightGray, fontSize = 12.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("SANS_SERIF", "SERIF", "MONOSPACE", "CURSIVE").forEach { font ->
                                                OutlinedButton(
                                                    onClick = { textFont = font },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (textFont == font) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        contentColor = if (textFont == font) Color.Black else Color.White
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(font.take(4), fontSize = 10.sp)
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (newTextContent.isNotBlank()) {
                                                    onIntent(
                                                        EditorIntent.AddTextOverlay(
                                                            text = newTextContent,
                                                            posX = 0.15f,
                                                            posY = 0.5f + ((state.document?.textOverlays?.size ?: 0) * 0.08f),
                                                            textSize = textSize,
                                                            fontFamilyName = textFont
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Insertar Texto", color = Color.Black, fontSize = 12.sp)
                                        }

                                        // Lista de textos añadidos
                                        state.document?.textOverlays?.forEach { txt ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = txt.text, color = Color.White, fontSize = 13.sp)
                                                        Text(text = "Fuente: ${txt.fontFamilyName} (${txt.textSize.toInt()}sp)", color = Color.LightGray, fontSize = 11.sp)
                                                    }
                                                    IconButton(
                                                        onClick = { onIntent(EditorIntent.DeleteTextOverlay(txt.id)) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Borrar", tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    2 -> {
                                        // Clone Stamp (Tampón de Clonar)
                                        Text(
                                            text = "Tampón de Clonar:",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Muestrea un área de origen y la estampa en el destino con bordes suavizados.",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )

                                        AdjustmentSlider("Radio del Parche", stampRadius, 15f, 120f, 45f, "px") {
                                            stampRadius = it
                                        }

                                        Button(
                                            onClick = {
                                                // Crear clonación de muestra
                                                val srcX = 0.3f
                                                val srcY = 0.4f
                                                val dstX = 0.65f
                                                val dstY = 0.55f
                                                onIntent(EditorIntent.AddCloneStamp(srcX, srcY, dstX, dstY, stampRadius))
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Aplicar Parche Clonado", color = Color.Black, fontSize = 12.sp)
                                        }

                                        if ((state.document?.cloneStamps?.size ?: 0) > 0) {
                                            TextButton(
                                                onClick = { onIntent(EditorIntent.ClearCloneStamps) },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Limpiar Parches Clonados", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            EditorSectionTab.LAYERS -> {
                                val layers = state.document?.layers ?: emptyList()

                                Text(text = "Gestor de Capas", color = Color.White, fontSize = 15.sp)

                                // Quick Add Layer Actions (Tinte, Duplicar, Texto, Sticker)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onIntent(
                                                EditorIntent.AddColorLayer(
                                                    name = "Capa Tinte ${layers.size + 1}",
                                                    colorHex = 0xFFFFB300,
                                                    blendMode = LayerBlendMode.OVERLAY,
                                                    opacity = 0.4f
                                                )
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("+ Tinte", color = Color.Black, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            onIntent(
                                                EditorIntent.AddDuplicateImageLayer(
                                                    name = "Duplicado ${layers.size + 1}",
                                                    blendMode = LayerBlendMode.SCREEN,
                                                    opacity = 0.5f
                                                )
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("+ Duplicar", color = Color.White, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { showAddTextLayerDialog = true },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("+ Texto", color = Color.White, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { showAddStickerDialog = true },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("+ Sticker", color = Color.White, fontSize = 11.sp)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                                if (layers.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No hay capas adicionales.\nAgrega un tinte, duplica la imagen, o añade textos/stickers.",
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                } else {
                                    // Render each Layer Card (stacked in visual reverse order: top layer first)
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        layers.asReversed().forEachIndexed { reversedIdx, layer ->
                                            val actualIndex = layers.size - 1 - reversedIdx
                                            LayerCard(
                                                layer = layer,
                                                isTop = actualIndex == layers.size - 1,
                                                isBottom = actualIndex == 0,
                                                onToggleVisibility = { onIntent(EditorIntent.ToggleLayerVisibility(layer.id)) },
                                                onOpacityChange = { onIntent(EditorIntent.UpdateLayerOpacity(layer.id, it)) },
                                                onBlendModeChange = { onIntent(EditorIntent.UpdateLayerBlendMode(layer.id, it)) },
                                                onTransformChange = { ox, oy, sc, rot ->
                                                    onIntent(EditorIntent.UpdateLayerTransform(layer.id, ox, oy, sc, rot))
                                                },
                                                onMoveUp = { onIntent(EditorIntent.MoveLayerUp(layer.id)) },
                                                onMoveDown = { onIntent(EditorIntent.MoveLayerDown(layer.id)) },
                                                onDelete = { onIntent(EditorIntent.DeleteLayer(layer.id)) }
                                            )
                                        }
                                    }
                                }
                            }

                            EditorSectionTab.GEOMETRY_CROP -> {
                                val crop = state.document?.cropTransform ?: EditOperation.CropTransform()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Recorte y Encuadre Snapseed", color = Color.White, fontSize = 15.sp)
                                    TextButton(
                                        onClick = {
                                            cropUiState.reset()
                                            onIntent(EditorIntent.ResetGeometry)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Restablecer Todo", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1B202A),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Arrastra la imagen para reubicarla, pellizca para zoom. Doble toque para centrar/ajustar.",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Modos de encuadre inicial (Auto-fit, Fill, Center)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RatioChip("Ajustar (Fit)", modifier = Modifier.weight(1f)) {
                                        cropUiState.setScaleModePreset(CropScaleMode.FIT)
                                        onIntent(EditorIntent.SetCropScaleMode(CropScaleMode.FIT))
                                    }
                                    RatioChip("Llenar (Fill)", modifier = Modifier.weight(1f)) {
                                        cropUiState.setScaleModePreset(CropScaleMode.FILL)
                                        onIntent(EditorIntent.SetCropScaleMode(CropScaleMode.FILL))
                                    }
                                    RatioChip("Centrar", modifier = Modifier.weight(1f)) {
                                        cropUiState.setScaleModePreset(CropScaleMode.CENTER)
                                        onIntent(EditorIntent.SetCropScaleMode(CropScaleMode.CENTER))
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                                // Botones de Rotación 90° y Flips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ActionButton(
                                        icon = Icons.Default.RotateLeft,
                                        label = "-90°",
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        onIntent(EditorIntent.Rotate90CounterClockwise)
                                    }
                                    ActionButton(
                                        icon = Icons.Default.RotateRight,
                                        label = "+90°",
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        onIntent(EditorIntent.Rotate90Clockwise)
                                    }
                                    ActionButton(
                                        icon = Icons.Default.Flip,
                                        label = "Flip H",
                                        isActive = crop.flipHorizontal,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        cropUiState.flipHorizontal = !cropUiState.flipHorizontal
                                        onIntent(EditorIntent.ToggleFlipHorizontal)
                                    }
                                    ActionButton(
                                        icon = Icons.Default.SwapVert,
                                        label = "Flip V",
                                        isActive = crop.flipVertical,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        cropUiState.flipVertical = !cropUiState.flipVertical
                                        onIntent(EditorIntent.ToggleFlipVertical)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                                // Slider de Enderezado Fino (Straighten con Snapping a 0°)
                                AdjustmentSlider(
                                    label = "Enderezar (Ángulo fino)",
                                    value = crop.fineStraightenAngle,
                                    min = -45.0f,
                                    max = 45.0f,
                                    defaultValue = 0.0f,
                                    unitSuffix = "°",
                                    onValueChange = {
                                        val angleWithSnap = if (Math.abs(it) < 0.8f) 0f else it
                                        cropUiState.applyStraighten(angleWithSnap)
                                        onIntent(EditorIntent.UpdateStraightenAngle(angleWithSnap))
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                                // Regla de Tercios Toggle & Reset
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Guías de composición (3×3)", color = Color.LightGray, fontSize = 13.sp)
                                    Switch(
                                        checked = cropUiState.showRuleOfThirds,
                                        onCheckedChange = {
                                            cropUiState.showRuleOfThirds = it
                                            onIntent(EditorIntent.ToggleCropRuleOfThirds(it))
                                        }
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                                // Presets de Relación de Aspecto
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Proporción de Recorte",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    TextButton(
                                        onClick = {
                                            cropUiState.reset()
                                            onIntent(EditorIntent.ResetCrop)
                                        }
                                    ) {
                                        Text("Reiniciar Marco", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        RatioChip("Original", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.ORIGINAL
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.ORIGINAL))
                                        }
                                        RatioChip("Libre", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.FREE
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.FREE))
                                        }
                                        RatioChip("1:1 Cuadrado", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.RATIO_1_1
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_1_1))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        RatioChip("4:5 Retrato / IG", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.RATIO_4_5
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_4_5))
                                        }
                                        RatioChip("3:4 Retrato", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.RATIO_3_4
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_3_4))
                                        }
                                        RatioChip("5:4 Clásico", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.RATIO_5_4
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_5_4))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        RatioChip("2:3 Fotografía", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.RATIO_2_3
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_2_3))
                                        }
                                        RatioChip("16:9 Panorámico", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.RATIO_16_9
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_16_9))
                                        }
                                        RatioChip("9:16 Historia / Reels", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.RATIO_9_16
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_9_16))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        RatioChip("4:3 Estándar", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.RATIO_4_3
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_4_3))
                                        }
                                        RatioChip("3:2 Clásico", modifier = Modifier.weight(1f)) {
                                            cropUiState.aspectRatio = CropAspectRatio.RATIO_3_2
                                            onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_3_2))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreativeTabChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .height(38.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(14.dp),
                tint = if (isSelected) Color.Black else Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MaskToolChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .height(38.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MaskCard(
    mask: MaskModel,
    onToggleEnabled: () -> Unit,
    onToggleInvert: () -> Unit,
    onFeatherChange: (Float) -> Unit,
    onAdjustmentsChange: (EditOperation.Adjustments) -> Unit,
    onDelete: () -> Unit
) {
    val adj = mask.localAdjustments

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Status + Title + Invert + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleEnabled, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (mask.isEnabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Activar",
                            tint = if (mask.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = mask.name, color = Color.White, fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onToggleInvert,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (mask.isInverted) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (mask.isInverted) Color.Black else Color.White
                        )
                    ) {
                        Text("Invertir", fontSize = 10.sp)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Feather / Suavizado de bordes
            AdjustmentSlider(
                label = "Suavizado de borde (Feather)",
                value = mask.featherRadius,
                min = 0f,
                max = 40f,
                defaultValue = 0f,
                unitSuffix = "px",
                onValueChange = onFeatherChange
            )

            HorizontalDivider(color = Color(0xFF1E2128))

            // Ajustes Locales de la Máscara
            Text(text = "Ajustes en el área enmascarada:", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)

            AdjustmentSlider("Brillo Local", adj.brightness, -1.0f, 1.0f, 0.0f) {
                onAdjustmentsChange(adj.copy(brightness = it))
            }
            AdjustmentSlider("Contraste Local", adj.contrast, 0.0f, 2.0f, 1.0f) {
                onAdjustmentsChange(adj.copy(contrast = it))
            }
            AdjustmentSlider("Saturación Local", adj.saturation, 0.0f, 2.0f, 1.0f) {
                onAdjustmentsChange(adj.copy(saturation = it))
            }
            AdjustmentSlider("Exposición Local", adj.exposure, -2.0f, 2.0f, 0.0f) {
                onAdjustmentsChange(adj.copy(exposure = it))
            }
        }
    }
}

@Composable
private fun LayerCard(
    layer: LayerModel,
    isTop: Boolean,
    isBottom: Boolean,
    onToggleVisibility: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlendModeChange: (LayerBlendMode) -> Unit,
    onTransformChange: (offsetX: Float, offsetY: Float, scale: Float, rotation: Float) -> Unit = { _, _, _, _ -> },
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedBlendMenu by remember { mutableStateOf(false) }
    var showTransformControls by remember { mutableStateOf(false) }

    val typeLabel = when (layer.layerType) {
        LayerType.COLOR_FILL -> "Tinte"
        LayerType.IMAGE_DUPLICATE -> "Imagen"
        LayerType.TEXT -> "Texto"
        LayerType.STICKER -> "Sticker"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Visibility + Title + Type Badge + Reorder + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleVisibility, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Visibilidad",
                            tint = if (layer.isVisible) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = layer.name,
                            color = if (layer.isVisible) Color.White else Color.Gray,
                            fontSize = 13.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = typeLabel,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isTop,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Mover arriba",
                            tint = if (!isTop) Color.White else Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isBottom,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Mover abajo",
                            tint = if (!isBottom) Color.White else Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar capa",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Blend Mode Selector & Opacity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Fusión:", color = Color.LightGray, fontSize = 12.sp)

                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E2128),
                        modifier = Modifier
                            .clickable { expandedBlendMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = layer.blendMode.name, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = expandedBlendMenu,
                        onDismissRequest = { expandedBlendMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        LayerBlendMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name, color = if (layer.blendMode == mode) MaterialTheme.colorScheme.primary else Color.White, fontSize = 12.sp) },
                                onClick = {
                                    onBlendModeChange(mode)
                                    expandedBlendMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Opacity Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Opacidad", color = Color.LightGray, fontSize = 12.sp)
                Text(text = "${(layer.opacity * 100).toInt()}%", color = Color.LightGray, fontSize = 11.sp)
            }

            Slider(
                value = layer.opacity,
                onValueChange = onOpacityChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color(0xFF1E2128)
                )
            )

            // Layer Transform Controls (For Text, Sticker, Duplicate Image)
            if (layer.layerType == LayerType.TEXT || layer.layerType == LayerType.STICKER || layer.layerType == LayerType.IMAGE_DUPLICATE) {
                HorizontalDivider(color = Color(0xFF1E2128), modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTransformControls = !showTransformControls }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transformación (Posición / Zoom / Giro)",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = if (showTransformControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (showTransformControls) {
                    AdjustmentSlider("Posición X", layer.offsetX, -300f, 300f, 0f, unitSuffix = "px") {
                        onTransformChange(it, layer.offsetY, layer.scale, layer.rotationDegrees)
                    }
                    AdjustmentSlider("Posición Y", layer.offsetY, -300f, 300f, 0f, unitSuffix = "px") {
                        onTransformChange(layer.offsetX, it, layer.scale, layer.rotationDegrees)
                    }
                    AdjustmentSlider("Escala (Zoom)", layer.scale, 0.2f, 3.0f, 1.0f) {
                        onTransformChange(layer.offsetX, layer.offsetY, it, layer.rotationDegrees)
                    }
                    AdjustmentSlider("Rotación", layer.rotationDegrees, -180f, 180f, 0f, unitSuffix = "°") {
                        onTransformChange(layer.offsetX, layer.offsetY, layer.scale, it)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isActive) Color.Black else Color.White
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RatioChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Text(text = label, fontSize = 12.sp)
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    defaultValue: Float,
    unitSuffix: String = "",
    onValueChange: (Float) -> Unit
) {
    val isModified = kotlin.math.abs(value - defaultValue) > 0.01f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isModified) Color.White else Color.LightGray,
                fontSize = 13.sp
            )
            val formatted = if (unitSuffix == "%") {
                "${(value * 100).toInt()}%"
            } else {
                "${String.format("%.2f", value)}$unitSuffix"
            }
            Text(
                text = formatted,
                color = if (isModified) MaterialTheme.colorScheme.primary else Color.Gray,
                fontSize = 12.sp
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = if (isModified) MaterialTheme.colorScheme.primary else Color.LightGray,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
