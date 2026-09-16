package com.hypereditor.nativegallery.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.graphics.ImageDecoder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.hypereditor.nativegallery.ui.canvas.CustomCropInteractiveCanvas
import com.hypereditor.nativegallery.ui.canvas.EditorCanvas
import com.hypereditor.nativegallery.ui.canvas.rememberCanvasViewportState
import com.hypereditor.nativegallery.ui.canvas.rememberCropUiState
import com.hypereditor.nativegallery.ui.canvas.rememberCustomCropState
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
    var isCustomCropActive by remember { mutableStateOf(false) }
    val customCropState = rememberCustomCropState(state.document?.cropTransform)
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var showAddTextLayerDialog by remember { mutableStateOf(false) }
    var newTextLayerContent by remember { mutableStateOf("Texto de Capa") }
    var showAddStickerDialog by remember { mutableStateOf(false) }

    // Retoque / Tampón de clonar táctil state
    var selectedCreativeTool by remember { mutableIntStateOf(0) } // 0: Pincel, 1: Texto, 2: Clone Stamp, 3: Healing
    var cloneMode by remember { mutableStateOf(com.hypereditor.nativegallery.ui.canvas.CloneMode.SELECT_ORIGIN) }
    var cloneOriginNorm by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var cloneRadius by remember { mutableFloatStateOf(45f) }
    var cloneHardness by remember { mutableFloatStateOf(0.5f) }
    var cloneOpacity by remember { mutableFloatStateOf(1.0f) }
    var cloneFlow by remember { mutableFloatStateOf(1.0f) }

    // Healing / Pincel Corrector state
    var healingToolMode by remember { mutableStateOf(com.hypereditor.nativegallery.ui.canvas.HealingToolMode.TAP) }
    var healingSamplingMode by remember { mutableStateOf(com.hypereditor.nativegallery.ui.canvas.HealingSamplingMode.AUTO) }
    var healingManualSource by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var healingRadius by remember { mutableFloatStateOf(32f) }
    var healingFeather by remember { mutableFloatStateOf(0.5f) }
    var healingStrength by remember { mutableFloatStateOf(1.0f) }

    // Parche / Patch Tool state
    var patchRadius by remember { mutableFloatStateOf(45f) }
    var patchFeather by remember { mutableFloatStateOf(0.5f) }
    var patchStrength by remember { mutableFloatStateOf(1.0f) }

    // Portrait Light / Luz de Retrato state
    var portraitLightExposure by remember { mutableFloatStateOf(0.35f) }
    var portraitLightShadows by remember { mutableFloatStateOf(0.2f) }
    var portraitLightHighlights by remember { mutableFloatStateOf(0.15f) }
    var portraitLightTemperature by remember { mutableFloatStateOf(0.05f) }
    var portraitLightFeather by remember { mutableFloatStateOf(0.6f) }
    var portraitLightOpacity by remember { mutableFloatStateOf(1.0f) }
    var portraitLightInvert by remember { mutableStateOf(false) }
    var activePortraitLight by remember {
        mutableStateOf(
            state.document?.portraitLights?.firstOrNull() ?: EditOperation.PortraitLight(
                exposure = 0.35f,
                shadows = 0.2f,
                highlights = 0.15f,
                temperature = 0.05f,
                feather = 0.6f,
                opacity = 1.0f
            )
        )
    }

    // Facial Relight / Reiluminación Facial state
    var selectedFacialZoneType by remember { mutableStateOf(EditOperation.FacialZoneType.FOREHEAD) }
    val defaultFacialZones = remember {
        listOf(
            EditOperation.FacialRelightZone(EditOperation.FacialZoneType.FOREHEAD, 0.5f, 0.28f, 0.22f, 0.12f, exposure = 0.25f),
            EditOperation.FacialRelightZone(EditOperation.FacialZoneType.LEFT_CHEEK, 0.35f, 0.45f, 0.14f, 0.14f, exposure = 0.15f),
            EditOperation.FacialRelightZone(EditOperation.FacialZoneType.RIGHT_CHEEK, 0.65f, 0.45f, 0.14f, 0.14f, exposure = 0.15f),
            EditOperation.FacialRelightZone(EditOperation.FacialZoneType.NOSE, 0.5f, 0.46f, 0.08f, 0.16f, exposure = 0.3f),
            EditOperation.FacialRelightZone(EditOperation.FacialZoneType.CHIN, 0.5f, 0.68f, 0.12f, 0.10f, exposure = 0.2f),
            EditOperation.FacialRelightZone(EditOperation.FacialZoneType.JAWLINE, 0.5f, 0.76f, 0.28f, 0.12f, exposure = -0.1f)
        )
    }
    var facialZones by remember {
        mutableStateOf(
            state.document?.facialRelights?.firstOrNull()?.zones?.takeIf { it.isNotEmpty() } ?: defaultFacialZones
        )
    }
    var facialGlobalSmoothness by remember { mutableFloatStateOf(0.2f) }
    var facialGlobalIntensity by remember { mutableFloatStateOf(1.0f) }

    // Mask & Selection Interactive Canvas state
    var maskBrushSizeNorm by remember { mutableFloatStateOf(0.05f) }
    var maskBrushIsEraser by remember { mutableStateOf(false) }

    // Double Exposure Picker
    val context = LocalContext.current
    val doubleExposurePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                val layersCount = (state.document?.layers?.size ?: 0) + 1
                onIntent(
                    EditorIntent.AddDoubleExposureLayer(
                        bitmap = bmp,
                        name = "Doble Exposición $layersCount",
                        blendMode = LayerBlendMode.SCREEN,
                        opacity = 0.75f
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(state.document?.cropTransform) {
        val currentTransform = state.document?.cropTransform ?: return@LaunchedEffect
        if (!cropUiState.isInteracting) {
            cropUiState.syncFrom(currentTransform, 0f, 0f)
        }
    }

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
                Text("Cargando imagen desde galería...", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
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
                    Text(text = "Error al abrir imagen", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = state.errorMessage, color = Color(0xFFFF6B6B), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Regresar a la Galería", color = MaterialTheme.colorScheme.onSurface)
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
            title = { Text("Guardar como Preset", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Guarda los ajustes y filtros actuales para reutilizarlos:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        placeholder = { Text("Nombre del Preset (ej. Mi Estilo)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
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
                    Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog para agregar Capa de Texto
    if (showAddTextLayerDialog) {
        AlertDialog(
            onDismissRequest = { showAddTextLayerDialog = false },
            title = { Text("Nueva Capa de Texto", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Ingresa el texto que deseas superponer como capa independiente:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newTextLayerContent,
                        onValueChange = { newTextLayerContent = it },
                        placeholder = { Text("Escribe tu texto aquí") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
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
                    Text("Agregar Capa", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextLayerDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            title = { Text("Seleccionar Sticker", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Elige un sticker para agregar como capa:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
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
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog para Historial Completo de Operaciones (Undo / Redo Real)
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Historial de Operaciones", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pila cronológica de ediciones aplicadas. Cada paso registra exactamente la operación realizada:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(state.historyList) { index, item ->
                            val isLatest = index == state.historyList.lastIndex
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isLatest) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isLatest) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "#$index",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Text(
                                            text = item,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp
                                        )
                                    }
                                    if (isLatest) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "ACTUAL",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onIntent(EditorIntent.Undo) },
                        enabled = state.canUndo
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(state.undoActionName?.let { "Deshacer: $it" } ?: "Deshacer")
                    }
                    Button(
                        onClick = { onIntent(EditorIntent.Redo) },
                        enabled = state.canRedo
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Redo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(state.redoActionName?.let { "Rehacer: $it" } ?: "Rehacer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                            tint = MaterialTheme.colorScheme.onSurface
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
                    Text("HyperEditor Pro", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp)

                    state.originalBitmap?.let { bmp ->
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${bmp.width} × ${bmp.height} px",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Undo
                    IconButton(
                        onClick = { onIntent(EditorIntent.Undo) },
                        enabled = state.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = state.undoActionName?.let { "Deshacer: $it" } ?: "Deshacer",
                            tint = if (state.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { onIntent(EditorIntent.Redo) },
                        enabled = state.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = state.redoActionName?.let { "Rehacer: $it" } ?: "Rehacer",
                            tint = if (state.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Botón para desplegar Historial de Operaciones
                    IconButton(
                        onClick = { showHistoryDialog = true }
                    ) {
                        BadgedBox(
                            badge = {
                                if (state.historyList.size > 1) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("${state.historyList.size - 1}", fontSize = 10.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Historial de Operaciones",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Botón Comparar Original
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.isComparingOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
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
                                tint = if (state.isComparingOriginal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isComparingOriginal) "ORIGINAL" else "Comparar",
                                color = if (state.isComparingOriginal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Reset Vista
                    if (viewportState.isModified) {
                        OutlinedButton(
                            onClick = { viewportState.reset() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
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
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardando...", color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
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

            // Viewport Canvas Area (Switches to Pro Interactive Crop / Custom Crop in GEOMETRY_CROP tab or CloneStamp in CREATIVE_TOOLS tab)
            if (state.selectedTab == EditorSectionTab.GEOMETRY_CROP) {
                if (isCustomCropActive) {
                    val currentCrop = state.document?.cropTransform ?: EditOperation.CropTransform()
                    val totalRot = currentCrop.rotation90Degrees * 90f + currentCrop.fineStraightenAngle
                    CustomCropInteractiveCanvas(
                        bitmap = state.originalBitmap,
                        rotation = totalRot,
                        flipHorizontal = currentCrop.flipHorizontal,
                        flipVertical = currentCrop.flipVertical,
                        cropState = customCropState,
                        onApply = { leftNorm, topNorm, rightNorm, bottomNorm ->
                            onIntent(EditorIntent.ApplyCustomFreeCrop(leftNorm, topNorm, rightNorm, bottomNorm))
                            isCustomCropActive = false
                        },
                        onCancel = {
                            val cur = state.document?.cropTransform ?: EditOperation.CropTransform()
                            customCropState.syncFrom(cur)
                            isCustomCropActive = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else {
                    CropInteractiveCanvas(
                        bitmap = state.originalBitmap,
                        cropState = cropUiState,
                        onCropTransformChanged = { onIntent(EditorIntent.UpdateCropTransform(it)) },
                        onInteractionStart = { onIntent(EditorIntent.BeginCropInteraction) },
                        onInteractionEnd = { onIntent(EditorIntent.CommitCropTransform("Recorte interactivo")) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else if (state.selectedTab == EditorSectionTab.CREATIVE_TOOLS && selectedCreativeTool == 2) {
                com.hypereditor.nativegallery.ui.canvas.CloneStampInteractiveCanvas(
                    bitmap = bitmapToDisplay,
                    cloneMode = cloneMode,
                    onCloneModeChanged = { cloneMode = it },
                    originNorm = cloneOriginNorm,
                    onOriginSelected = { cloneOriginNorm = it },
                    stampRadius = cloneRadius,
                    stampHardness = cloneHardness,
                    stampOpacity = cloneOpacity,
                    stampFlow = cloneFlow,
                    onApplyStamps = { stamps ->
                        onIntent(EditorIntent.AddCloneStampBatch(stamps))
                    },
                    onClearStamps = {
                        onIntent(EditorIntent.ClearCloneStamps)
                    },
                    stampsCount = state.document?.cloneStamps?.size ?: 0,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            } else if (state.selectedTab == EditorSectionTab.CREATIVE_TOOLS && selectedCreativeTool == 3) {
                com.hypereditor.nativegallery.ui.canvas.HealingInteractiveCanvas(
                    bitmap = bitmapToDisplay,
                    toolMode = healingToolMode,
                    samplingMode = healingSamplingMode,
                    onToolModeChanged = { healingToolMode = it },
                    onSamplingModeChanged = { healingSamplingMode = it },
                    radius = healingRadius,
                    feather = healingFeather,
                    strength = healingStrength,
                    manualSourceNorm = healingManualSource,
                    onManualSourceSelected = { healingManualSource = it },
                    onApplyStroke = { stroke ->
                        onIntent(EditorIntent.AddHealingStroke(stroke))
                    },
                    strokesCount = state.document?.healingStrokes?.size ?: 0,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            } else if (state.selectedTab == EditorSectionTab.CREATIVE_TOOLS && selectedCreativeTool == 4) {
                com.hypereditor.nativegallery.ui.canvas.PatchInteractiveCanvas(
                    bitmap = bitmapToDisplay,
                    radius = patchRadius,
                    feather = patchFeather,
                    strength = patchStrength,
                    onApplyPatch = { patch ->
                        onIntent(EditorIntent.AddPatchOperation(patch))
                    },
                    patchesCount = state.document?.patchOperations?.size ?: 0,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            } else if (state.selectedTab == EditorSectionTab.CREATIVE_TOOLS && selectedCreativeTool == 5) {
                com.hypereditor.nativegallery.ui.canvas.PortraitLightInteractiveCanvas(
                    bitmap = bitmapToDisplay,
                    portraitLight = activePortraitLight,
                    onLightChanged = { newLight ->
                        activePortraitLight = newLight
                        portraitLightExposure = newLight.exposure
                        portraitLightShadows = newLight.shadows
                        portraitLightHighlights = newLight.highlights
                        portraitLightTemperature = newLight.temperature
                        portraitLightFeather = newLight.feather
                        portraitLightOpacity = newLight.opacity
                        portraitLightInvert = newLight.isInverted
                        onIntent(EditorIntent.UpdatePortraitLight(newLight))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            } else if (state.selectedTab == EditorSectionTab.CREATIVE_TOOLS && selectedCreativeTool == 6) {
                com.hypereditor.nativegallery.ui.canvas.FacialRelightInteractiveCanvas(
                    bitmap = bitmapToDisplay,
                    zones = facialZones,
                    selectedZoneType = selectedFacialZoneType,
                    onZoneSelected = { selectedFacialZoneType = it },
                    onZoneChanged = { updatedZone ->
                        val newZones = facialZones.map { if (it.zoneType == updatedZone.zoneType) updatedZone else it }
                        facialZones = newZones
                        onIntent(EditorIntent.UpdateFacialRelightZones(newZones))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            } else if (state.selectedTab == EditorSectionTab.MASKS_SELECTIONS) {
                val masks = state.document?.masks ?: emptyList()
                val currentActiveMask = masks.find { it.id == state.activeMaskId } ?: masks.firstOrNull()
                com.hypereditor.nativegallery.ui.canvas.MaskSelectionInteractiveCanvas(
                    bitmap = bitmapToDisplay,
                    activeMask = currentActiveMask,
                    onUpdateRectBounds = { bounds ->
                        currentActiveMask?.let { onIntent(EditorIntent.UpdateMaskRectBounds(it.id, bounds)) }
                    },
                    onUpdateEllipseBounds = { bounds ->
                        currentActiveMask?.let { onIntent(EditorIntent.UpdateMaskEllipseBounds(it.id, bounds)) }
                    },
                    onUpdateLassoPoints = { points ->
                        currentActiveMask?.let { onIntent(EditorIntent.UpdateMaskLassoPoints(it.id, points)) }
                    },
                    onAddBrushStroke = { stroke ->
                        currentActiveMask?.let { onIntent(EditorIntent.AddMaskBrushStroke(it.id, stroke)) }
                    },
                    onClearSelection = {
                        currentActiveMask?.let { onIntent(EditorIntent.ClearMask(it.id)) }
                    },
                    onToggleSelectionMode = {
                        currentActiveMask?.let {
                            val newMode = if (it.selectionMode == com.hypereditor.nativegallery.domain.model.SelectionMode.ADD) {
                                com.hypereditor.nativegallery.domain.model.SelectionMode.SUBTRACT
                            } else {
                                com.hypereditor.nativegallery.domain.model.SelectionMode.ADD
                            }
                            onIntent(EditorIntent.UpdateMaskSelectionMode(it.id, newMode))
                        }
                    },
                    brushSizeNorm = maskBrushSizeNorm,
                    isEraserMode = maskBrushIsEraser,
                    onToggleEraserMode = { maskBrushIsEraser = !maskBrushIsEraser },
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
                                    Text(text = "Ajustes de Color", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                                    TextButton(
                                        onClick = {
                                            onIntent(EditorIntent.UpdateAdjustments(EditOperation.Adjustments(), isFinished = true, actionLabel = "Restablecer ajustes"))
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Restablecer", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                }

                                AdjustmentSlider(
                                    label = "Brillo",
                                    value = adj.brightness,
                                    min = -1.0f,
                                    max = 1.0f,
                                    defaultValue = 0.0f,
                                    onValueChangeFinished = {
                                        onIntent(EditorIntent.UpdateAdjustments(adj, isFinished = true, actionLabel = "Ajuste de brillo"))
                                    }
                                ) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(brightness = it), isFinished = false))
                                }

                                AdjustmentSlider(
                                    label = "Contraste",
                                    value = adj.contrast,
                                    min = 0.0f,
                                    max = 2.0f,
                                    defaultValue = 1.0f,
                                    onValueChangeFinished = {
                                        onIntent(EditorIntent.UpdateAdjustments(adj, isFinished = true, actionLabel = "Ajuste de contraste"))
                                    }
                                ) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(contrast = it), isFinished = false))
                                }

                                AdjustmentSlider(
                                    label = "Saturación",
                                    value = adj.saturation,
                                    min = 0.0f,
                                    max = 2.0f,
                                    defaultValue = 1.0f,
                                    onValueChangeFinished = {
                                        onIntent(EditorIntent.UpdateAdjustments(adj, isFinished = true, actionLabel = "Ajuste de saturación"))
                                    }
                                ) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(saturation = it), isFinished = false))
                                }

                                AdjustmentSlider(
                                    label = "Exposición",
                                    value = adj.exposure,
                                    min = -2.0f,
                                    max = 2.0f,
                                    defaultValue = 0.0f,
                                    onValueChangeFinished = {
                                        onIntent(EditorIntent.UpdateAdjustments(adj, isFinished = true, actionLabel = "Ajuste de exposición"))
                                    }
                                ) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(exposure = it), isFinished = false))
                                }

                                AdjustmentSlider(
                                    label = "Temperatura",
                                    value = adj.temperature,
                                    min = -1.0f,
                                    max = 1.0f,
                                    defaultValue = 0.0f,
                                    onValueChangeFinished = {
                                        onIntent(EditorIntent.UpdateAdjustments(adj, isFinished = true, actionLabel = "Ajuste de temperatura"))
                                    }
                                ) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(temperature = it), isFinished = false))
                                }

                                AdjustmentSlider(
                                    label = "Tinte",
                                    value = adj.tint,
                                    min = -1.0f,
                                    max = 1.0f,
                                    defaultValue = 0.0f,
                                    onValueChangeFinished = {
                                        onIntent(EditorIntent.UpdateAdjustments(adj, isFinished = true, actionLabel = "Ajuste de tinte"))
                                    }
                                ) {
                                    onIntent(EditorIntent.UpdateAdjustments(adj.copy(tint = it), isFinished = false))
                                }
                            }

                            EditorSectionTab.FILTERS_PRESETS -> {
                                val activeFilter = state.document?.appliedFilter

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Filtros Predefinidos", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
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
                                                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) else null,
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
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Sección de Presets del Usuario
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Presets Guardados", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                                    Button(
                                        onClick = { showSavePresetDialog = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Guardar Preset", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp)
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (preset in state.userPresets) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
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
                                                    Text(text = preset.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                                    Text(
                                                        text = if (preset.appliedFilter != null) "Filtro ${preset.appliedFilter.filterName} + Ajustes" else "Ajustes de Color",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

                                Text(text = "Máscaras y Ajustes Locales", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)

                                // Quick Add Mask Selection Tools
                                Text(text = "Crear Selección:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                if (masks.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No hay máscaras activas.\nCrea una selección (Rect, Óvalo, Lazo o Pincel)\npara aplicar ajustes localizados.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        masks.forEach { mask ->
                                            MaskCard(
                                                mask = mask,
                                                isSelected = (mask.id == state.activeMaskId) || (state.activeMaskId == null && mask == masks.firstOrNull()),
                                                onSelect = { onIntent(EditorIntent.SelectActiveMask(mask.id)) },
                                                onToggleEnabled = { onIntent(EditorIntent.ToggleMaskEnabled(mask.id)) },
                                                onToggleInvert = { onIntent(EditorIntent.ToggleMaskInvert(mask.id)) },
                                                onToggleMode = {
                                                    val newMode = if (mask.selectionMode == SelectionMode.ADD) SelectionMode.SUBTRACT else SelectionMode.ADD
                                                    onIntent(EditorIntent.UpdateMaskSelectionMode(mask.id, newMode))
                                                },
                                                onSelectionTypeChange = { onIntent(EditorIntent.UpdateMaskSelectionType(mask.id, it)) },
                                                onClearSelection = { onIntent(EditorIntent.ClearMask(mask.id)) },
                                                onFeatherChange = { onIntent(EditorIntent.UpdateMaskFeather(mask.id, it)) },
                                                onAdjustmentsChange = { onIntent(EditorIntent.UpdateMaskLocalAdjustments(mask.id, it)) },
                                                onDelete = { onIntent(EditorIntent.DeleteMask(mask.id)) }
                                            )
                                        }
                                    }
                                }
                            }

                            EditorSectionTab.CREATIVE_TOOLS -> {
                                var brushSize by remember { mutableFloatStateOf(24f) }
                                var brushColor by remember { mutableIntStateOf(android.graphics.Color.YELLOW) }
                                var brushOpacity by remember { mutableFloatStateOf(1.0f) }
                                var isEraserMode by remember { mutableStateOf(false) }

                                var newTextContent by remember { mutableStateOf("HyperEditor") }
                                var textSize by remember { mutableFloatStateOf(44f) }
                                var textFont by remember { mutableStateOf("SANS_SERIF") }

                                Text(text = "Herramientas Creativas y Retoque", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)

                                // Selector de sub-herramienta
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        CreativeTabChip("Corrector", Icons.Default.Healing, selectedCreativeTool == 3, modifier = Modifier.weight(1f)) {
                                            selectedCreativeTool = 3
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CreativeTabChip("Parche", Icons.Default.CropPortrait, selectedCreativeTool == 4, modifier = Modifier.weight(1f)) {
                                            selectedCreativeTool = 4
                                        }
                                        CreativeTabChip("Luz Retrato", Icons.Default.WbIncandescent, selectedCreativeTool == 5, modifier = Modifier.weight(1f)) {
                                            selectedCreativeTool = 5
                                        }
                                        CreativeTabChip("Reiluminar", Icons.Default.Face, selectedCreativeTool == 6, modifier = Modifier.weight(1f)) {
                                            selectedCreativeTool = 6
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                when (selectedCreativeTool) {
                                    0 -> {
                                        // Pincel y Borrador
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "Modo:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = { isEraserMode = false },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (!isEraserMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        contentColor = if (!isEraserMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Pincel", fontSize = 11.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { isEraserMode = true },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (isEraserMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        contentColor = if (isEraserMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Borrador", fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        if (!isEraserMode) {
                                            Text(text = "Color de trazo:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
                                                                color = if (brushColor == cInt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
                                            Icon(if (isEraserMode) Icons.Default.Delete else Icons.Default.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isEraserMode) "Aplicar Borrador" else "Añadir Trazo Pincel", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
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
                                        Text(text = "Contenido del Texto:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        OutlinedTextField(
                                            value = newTextContent,
                                            onValueChange = { newTextContent = it },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )

                                        AdjustmentSlider("Tamaño de Fuente", textSize, 16f, 100f, 44f, "sp") {
                                            textSize = it
                                        }

                                        Text(text = "Tipografía:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("SANS_SERIF", "SERIF", "MONOSPACE", "CURSIVE").forEach { font ->
                                                OutlinedButton(
                                                    onClick = { textFont = font },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (textFont == font) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        contentColor = if (textFont == font) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
                                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Insertar Texto", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
                                        }

                                        // Lista de textos añadidos
                                        state.document?.textOverlays?.forEach { txt ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
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
                                                        Text(text = txt.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                                        Text(text = "Fuente: ${txt.fontFamilyName} (${txt.textSize.toInt()}sp)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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
                                        // Retoque manual y táctil: Tampón de Clonar
                                        Text(
                                            text = "Tampón de Clonar (Retoque Táctil)",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 15.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )

                                        // 1. Selector de los dos modos claramente visibles
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { cloneMode = com.hypereditor.nativegallery.ui.canvas.CloneMode.SELECT_ORIGIN },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (cloneMode == com.hypereditor.nativegallery.ui.canvas.CloneMode.SELECT_ORIGIN)
                                                        Color(0xFF00E5FF) else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (cloneMode == com.hypereditor.nativegallery.ui.canvas.CloneMode.SELECT_ORIGIN)
                                                        Color.Black else MaterialTheme.colorScheme.onSurface
                                                ),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 10.dp)
                                            ) {
                                                Icon(Icons.Default.FilterTiltShift, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Elegir origen", fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = { cloneMode = com.hypereditor.nativegallery.ui.canvas.CloneMode.PAINT },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (cloneMode == com.hypereditor.nativegallery.ui.canvas.CloneMode.PAINT)
                                                        Color(0xFFFF9100) else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (cloneMode == com.hypereditor.nativegallery.ui.canvas.CloneMode.PAINT)
                                                        Color.Black else MaterialTheme.colorScheme.onSurface
                                                ),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 10.dp)
                                            ) {
                                                Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Clonar/Pintar", fontSize = 12.sp)
                                            }
                                        }

                                        // 2. Indicador textual del estado actual
                                        val statusDesc = when {
                                            cloneMode == com.hypereditor.nativegallery.ui.canvas.CloneMode.SELECT_ORIGIN && cloneOriginNorm == null ->
                                                "Toca la imagen para fijar el origen"
                                            cloneMode == com.hypereditor.nativegallery.ui.canvas.CloneMode.SELECT_ORIGIN && cloneOriginNorm != null ->
                                                "Origen seleccionado. Cambia a Clonar para pintar."
                                            cloneMode == com.hypereditor.nativegallery.ui.canvas.CloneMode.PAINT && cloneOriginNorm == null ->
                                                "Primero selecciona un origen"
                                            cloneMode == com.hypereditor.nativegallery.ui.canvas.CloneMode.PAINT && cloneOriginNorm != null ->
                                                "Arrastra para clonar desde el origen"
                                            else -> "Toca la imagen para fijar el origen"
                                        }

                                        val statusColor = when {
                                            cloneMode == com.hypereditor.nativegallery.ui.canvas.CloneMode.PAINT && cloneOriginNorm == null ->
                                                Color(0xFFFF5252)
                                            cloneOriginNorm != null ->
                                                Color(0xFF00E5FF)
                                            else ->
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.6f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (cloneMode == com.hypereditor.nativegallery.ui.canvas.CloneMode.PAINT && cloneOriginNorm == null)
                                                        Icons.Default.Warning else Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = statusColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "Estado:",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = statusDesc,
                                                        fontSize = 13.sp,
                                                        color = statusColor,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                                    )
                                                    if (cloneOriginNorm != null) {
                                                        Text(
                                                            text = "Origen fijado en (${(cloneOriginNorm!!.x * 100).toInt()}%, ${(cloneOriginNorm!!.y * 100).toInt()}%)",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // 3. Botones rápidos para cambiar origen o pasar a clonar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { cloneMode = com.hypereditor.nativegallery.ui.canvas.CloneMode.SELECT_ORIGIN },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Cambiar origen", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { cloneMode = com.hypereditor.nativegallery.ui.canvas.CloneMode.PAINT },
                                                enabled = cloneOriginNorm != null,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Pasar a Clonar", fontSize = 11.sp)
                                            }
                                        }

                                        // 4. Sliders profesionales de control estilo Photoshop
                                        AdjustmentSlider("Tamaño / Radio", cloneRadius, 15f, 180f, 45f, "px") {
                                            cloneRadius = it
                                        }

                                        AdjustmentSlider("Dureza", cloneHardness * 100f, 5f, 100f, 50f, "%") {
                                            cloneHardness = it / 100f
                                        }

                                        AdjustmentSlider("Opacidad", cloneOpacity * 100f, 10f, 100f, 100f, "%") {
                                            cloneOpacity = it / 100f
                                        }

                                        AdjustmentSlider("Flujo", cloneFlow * 100f, 10f, 100f, 100f, "%") {
                                            cloneFlow = it / 100f
                                        }

                                        // 5. Integración con Capas existentes
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "Integrado con el Pipeline: Los parches se procesan en ARGB_8888 y se integran automáticamente debajo de las capas activas (${state.document?.layers?.size ?: 0} capas).",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // 6. Botón para limpiar retoques
                                        val totalStamps = state.document?.cloneStamps?.size ?: 0
                                        if (totalStamps > 0) {
                                            OutlinedButton(
                                                onClick = { onIntent(EditorIntent.ClearCloneStamps) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Limpiar Retoques ($totalStamps parches)", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    3 -> {
                                        // Pincel Corrector / Healing (Spot Healing & Healing Brush)
                                        Text(
                                            text = "Pincel Corrector (Healing)",
                                            fontSize = 13.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "Elimina imperfecciones, polvo, manchas o cables mezclando la textura circundante con el color e iluminación del destino.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // 1. Selector de Modo: Toque vs Pincel
                                        Text(text = "Modo de Aplicación:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilterChip(
                                                selected = healingToolMode == com.hypereditor.nativegallery.ui.canvas.HealingToolMode.TAP,
                                                onClick = { healingToolMode = com.hypereditor.nativegallery.ui.canvas.HealingToolMode.TAP },
                                                label = { Text("Toque (Puntual)", fontSize = 11.sp) },
                                                leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = healingToolMode == com.hypereditor.nativegallery.ui.canvas.HealingToolMode.BRUSH,
                                                onClick = { healingToolMode = com.hypereditor.nativegallery.ui.canvas.HealingToolMode.BRUSH },
                                                label = { Text("Pincel (Trazo)", fontSize = 11.sp) },
                                                leadingIcon = { Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // 2. Selector de Muestreo: Automático vs Manual
                                        Text(text = "Modo de Muestreo:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilterChip(
                                                selected = healingSamplingMode == com.hypereditor.nativegallery.ui.canvas.HealingSamplingMode.AUTO,
                                                onClick = { healingSamplingMode = com.hypereditor.nativegallery.ui.canvas.HealingSamplingMode.AUTO },
                                                label = { Text("Automático", fontSize = 11.sp) },
                                                leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = healingSamplingMode == com.hypereditor.nativegallery.ui.canvas.HealingSamplingMode.MANUAL,
                                                onClick = { healingSamplingMode = com.hypereditor.nativegallery.ui.canvas.HealingSamplingMode.MANUAL },
                                                label = { Text("Manual", fontSize = 11.sp) },
                                                leadingIcon = { Icon(Icons.Default.Adjust, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // 3. Sliders de control: Tamaño/Radio, Dureza/Feather, Fuerza
                                        AdjustmentSlider("Tamaño del Pincel", healingRadius, 10f, 150f, 32f, "px") {
                                            healingRadius = it
                                        }

                                        AdjustmentSlider("Difuminado / Feather", healingFeather * 100f, 10f, 100f, 50f, "%") {
                                            healingFeather = it / 100f
                                        }

                                        AdjustmentSlider("Fuerza / Opacidad", healingStrength * 100f, 10f, 100f, 100f, "%") {
                                            healingStrength = it / 100f
                                        }

                                        // 4. Badge informativo de integración con el pipeline
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Healing, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "Fusión de textura armónica: Transfiere el micro-detalle de la piel/superficie sin bordes duros. Cada trazo o toque añade 1 paso a Deshacer/Rehacer.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // 5. Botón para limpiar retoques de Healing
                                        val totalHealing = state.document?.healingStrokes?.size ?: 0
                                        if (totalHealing > 0) {
                                            OutlinedButton(
                                                onClick = { onIntent(EditorIntent.ClearHealingStrokes) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Limpiar Correcciones ($totalHealing trazos)", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    4 -> {
                                        // Parche / Patch Tool
                                        Text(
                                            text = "Herramienta Parche (Patch Tool)",
                                            fontSize = 13.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "Toca o selecciona una imperfección en el lienzo y arrástrala hacia una zona limpia para muestrear textura con bordes difuminados y tono parejo.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        AdjustmentSlider("Radio de Selección", patchRadius, 15f, 180f, 45f, "px") {
                                            patchRadius = it
                                        }

                                        AdjustmentSlider("Difuminado de Borde (Feather)", patchFeather * 100f, 10f, 100f, 50f, "%") {
                                            patchFeather = it / 100f
                                        }

                                        AdjustmentSlider("Fuerza de Fusión", patchStrength * 100f, 10f, 100f, 100f, "%") {
                                            patchStrength = it / 100f
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.CropPortrait, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "Flujo de 2 pasos estilo Photoshop: 1) Selecciona el defecto, 2) Arrastra al origen donante. El algoritmo armoniza iluminación y grano.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        val totalPatches = state.document?.patchOperations?.size ?: 0
                                        if (totalPatches > 0) {
                                            OutlinedButton(
                                                onClick = { onIntent(EditorIntent.ClearPatchOperations) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Limpiar Parches ($totalPatches aplicados)", color = Color(0xFFFF6B6B), fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    5 -> {
                                        // Luz de Retrato / Portrait Light
                                        Text(
                                            text = "Luz de Retrato Manual (Studio Relight)",
                                            fontSize = 13.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "Coloca y arrastra la fuente de luz sobre la imagen. Modela volumen, sombras y calidez emulando reflectores de estudio fotográfico.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        AdjustmentSlider("Exposición de Luz", portraitLightExposure, -1f, 1f, 0.35f) {
                                            portraitLightExposure = it
                                            val updated = activePortraitLight.copy(exposure = it)
                                            activePortraitLight = updated
                                            onIntent(EditorIntent.UpdatePortraitLight(updated))
                                        }

                                        AdjustmentSlider("Relleno de Sombras", portraitLightShadows, -1f, 1f, 0.2f) {
                                            portraitLightShadows = it
                                            val updated = activePortraitLight.copy(shadows = it)
                                            activePortraitLight = updated
                                            onIntent(EditorIntent.UpdatePortraitLight(updated))
                                        }

                                        AdjustmentSlider("Brillo / Altas Luces", portraitLightHighlights, -1f, 1f, 0.15f) {
                                            portraitLightHighlights = it
                                            val updated = activePortraitLight.copy(highlights = it)
                                            activePortraitLight = updated
                                            onIntent(EditorIntent.UpdatePortraitLight(updated))
                                        }

                                        AdjustmentSlider("Temperatura de Luz", portraitLightTemperature, -1f, 1f, 0.05f) {
                                            portraitLightTemperature = it
                                            val updated = activePortraitLight.copy(temperature = it)
                                            activePortraitLight = updated
                                            onIntent(EditorIntent.UpdatePortraitLight(updated))
                                        }

                                        AdjustmentSlider("Suavizado / Difusión", portraitLightFeather * 100f, 10f, 100f, 60f, "%") {
                                            portraitLightFeather = it / 100f
                                            val updated = activePortraitLight.copy(feather = it / 100f)
                                            activePortraitLight = updated
                                            onIntent(EditorIntent.UpdatePortraitLight(updated))
                                        }

                                        AdjustmentSlider("Opacidad de la Luz", portraitLightOpacity * 100f, 10f, 100f, 100f, "%") {
                                            portraitLightOpacity = it / 100f
                                            val updated = activePortraitLight.copy(opacity = it / 100f)
                                            activePortraitLight = updated
                                            onIntent(EditorIntent.UpdatePortraitLight(updated))
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Invertir Foco (Iluminar Fondo)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Switch(
                                                checked = portraitLightInvert,
                                                onCheckedChange = {
                                                    portraitLightInvert = it
                                                    val updated = activePortraitLight.copy(isInverted = it)
                                                    activePortraitLight = updated
                                                    onIntent(EditorIntent.UpdatePortraitLight(updated))
                                                }
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val reset = EditOperation.PortraitLight(
                                                    centerXNorm = 0.5f,
                                                    centerYNorm = 0.45f,
                                                    radiusXNorm = 0.35f,
                                                    radiusYNorm = 0.45f,
                                                    exposure = 0.35f,
                                                    shadows = 0.2f,
                                                    highlights = 0.15f,
                                                    temperature = 0.05f,
                                                    feather = 0.6f,
                                                    opacity = 1.0f,
                                                    isInverted = false
                                                )
                                                activePortraitLight = reset
                                                portraitLightExposure = 0.35f
                                                portraitLightShadows = 0.2f
                                                portraitLightHighlights = 0.15f
                                                portraitLightTemperature = 0.05f
                                                portraitLightFeather = 0.6f
                                                portraitLightOpacity = 1.0f
                                                portraitLightInvert = false
                                                onIntent(EditorIntent.UpdatePortraitLight(reset))
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Centrar Fuente de Luz", fontSize = 12.sp)
                                        }
                                    }

                                    6 -> {
                                        // Reiluminación Facial / Facial Relight
                                        Text(
                                            text = "Reiluminación Facial por Zonas",
                                            fontSize = 13.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "Ajusta la luz individualmente sobre Frente, Pómulos, Nariz, Mentón o Mandíbula arrastrando los puntos anatómicos o mediante los controles:",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Selector de Zonas Anatómicas
                                        val zoneLabels = listOf(
                                            Pair(EditOperation.FacialZoneType.FOREHEAD, "Frente"),
                                            Pair(EditOperation.FacialZoneType.LEFT_CHEEK, "Pómulo Izq"),
                                            Pair(EditOperation.FacialZoneType.RIGHT_CHEEK, "Pómulo Der"),
                                            Pair(EditOperation.FacialZoneType.NOSE, "Nariz"),
                                            Pair(EditOperation.FacialZoneType.CHIN, "Mentón"),
                                            Pair(EditOperation.FacialZoneType.JAWLINE, "Mandíbula")
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            zoneLabels.take(3).forEach { (zt, name) ->
                                                FilterChip(
                                                    selected = selectedFacialZoneType == zt,
                                                    onClick = { selectedFacialZoneType = zt },
                                                    label = { Text(name, fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            zoneLabels.drop(3).forEach { (zt, name) ->
                                                FilterChip(
                                                    selected = selectedFacialZoneType == zt,
                                                    onClick = { selectedFacialZoneType = zt },
                                                    label = { Text(name, fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        val currentZone = facialZones.find { it.zoneType == selectedFacialZoneType }
                                            ?: EditOperation.FacialRelightZone(selectedFacialZoneType, 0.5f, 0.5f, 0.15f, 0.15f)

                                        AdjustmentSlider("Luz de Zona (Exposición)", currentZone.exposure, -1f, 1f, 0f) {
                                            val updated = currentZone.copy(exposure = it)
                                            val newZones = facialZones.map { z -> if (z.zoneType == currentZone.zoneType) updated else z }
                                            facialZones = newZones
                                            onIntent(EditorIntent.UpdateFacialRelightZones(newZones))
                                        }

                                        AdjustmentSlider("Tono / Temperatura", currentZone.temperature, -1f, 1f, 0f) {
                                            val updated = currentZone.copy(temperature = it)
                                            val newZones = facialZones.map { z -> if (z.zoneType == currentZone.zoneType) updated else z }
                                            facialZones = newZones
                                            onIntent(EditorIntent.UpdateFacialRelightZones(newZones))
                                        }

                                        AdjustmentSlider("Contorno / Sombras", currentZone.shadows, -1f, 1f, 0f) {
                                            val updated = currentZone.copy(shadows = it)
                                            val newZones = facialZones.map { z -> if (z.zoneType == currentZone.zoneType) updated else z }
                                            facialZones = newZones
                                            onIntent(EditorIntent.UpdateFacialRelightZones(newZones))
                                        }

                                        AdjustmentSlider("Suavizado de Zona", currentZone.smoothness * 100f, 0f, 100f, 20f, "%") {
                                            val updated = currentZone.copy(smoothness = it / 100f)
                                            val newZones = facialZones.map { z -> if (z.zoneType == currentZone.zoneType) updated else z }
                                            facialZones = newZones
                                            onIntent(EditorIntent.UpdateFacialRelightZones(newZones))
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                        AdjustmentSlider("Suavizado Global Piel", facialGlobalSmoothness * 100f, 0f, 100f, 20f, "%") {
                                            facialGlobalSmoothness = it / 100f
                                            val currentRelight = state.document?.facialRelights?.firstOrNull() ?: EditOperation.FacialRelight(zones = facialZones)
                                            val updated = currentRelight.copy(globalSmoothness = it / 100f, globalIntensity = facialGlobalIntensity)
                                            onIntent(EditorIntent.UpdateFacialRelight(updated))
                                        }

                                        AdjustmentSlider("Intensidad Global", facialGlobalIntensity * 100f, 10f, 100f, 100f, "%") {
                                            facialGlobalIntensity = it / 100f
                                            val currentRelight = state.document?.facialRelights?.firstOrNull() ?: EditOperation.FacialRelight(zones = facialZones)
                                            val updated = currentRelight.copy(globalSmoothness = facialGlobalSmoothness, globalIntensity = it / 100f)
                                            onIntent(EditorIntent.UpdateFacialRelight(updated))
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                facialZones = defaultFacialZones
                                                onIntent(EditorIntent.UpdateFacialRelightZones(defaultFacialZones))
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Restablecer Posiciones Anatómicas", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            EditorSectionTab.LAYERS -> {
                                val layers = state.document?.layers ?: emptyList()

                                Text(text = "Gestor de Capas", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)

                                // Quick Add Layer Actions (Tinte, Duplicar, Doble Exp, Texto, Sticker)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                            Text("+ Tinte", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp)
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
                                            Text("+ Duplicar", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                doubleExposurePicker.launch("image/*")
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                                        ) {
                                            Text("+ Doble Exp.", color = Color.White, fontSize = 11.sp)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { showAddTextLayerDialog = true },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Text("+ Texto", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { showAddStickerDialog = true },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Text("+ Sticker", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                if (layers.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No hay capas adicionales.\nAgrega un tinte, duplica la imagen, añade doble exposición o textos/stickers.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                                onToggleFlipH = { onIntent(EditorIntent.ToggleLayerFlipHorizontal(layer.id)) },
                                                onToggleFlipV = { onIntent(EditorIntent.ToggleLayerFlipVertical(layer.id)) },
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

                                // Selector de herramienta: Encuadre Proporcional vs Recorte personalizado
                                TabRow(
                                    selectedTabIndex = if (isCustomCropActive) 1 else 0,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    Tab(
                                        selected = !isCustomCropActive,
                                        onClick = { isCustomCropActive = false },
                                        text = { Text("Encuadre", fontSize = 11.sp, maxLines = 1) },
                                        icon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                    Tab(
                                        selected = isCustomCropActive,
                                        onClick = {
                                            customCropState.syncFrom(crop)
                                            isCustomCropActive = true
                                        },
                                        text = { Text("Personalizado", fontSize = 11.sp, maxLines = 1) },
                                        icon = { Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }

                                if (isCustomCropActive) {
                                    val baseBmp = state.originalBitmap
                                    val imgW = baseBmp?.width?.toFloat() ?: 1000f
                                    val imgH = baseBmp?.height?.toFloat() ?: 1000f
                                    val pixelW = ((customCropState.cropRightNorm - customCropState.cropLeftNorm) * imgW).toInt().coerceAtLeast(1)
                                    val pixelH = ((customCropState.cropBottomNorm - customCropState.cropTopNorm) * imgH).toInt().coerceAtLeast(1)
                                    val ratioVal = pixelW.toFloat() / pixelH.toFloat()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Recorte personalizado", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                                        TextButton(
                                            onClick = { customCropState.resetToFull() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Restablecer", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Arrastra las 4 esquinas o los 4 lados para redimensionar libremente. Arrastra el centro para mover la selección.",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Switch Bloquear proporción
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Bloquear proporción", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                                Text(
                                                    text = if (customCropState.isAspectRatioLocked) "Aspecto fijo actual (${String.format(java.util.Locale.US, "%.2f:1", ratioVal)})" else "Ancho y alto independientes (libre)",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Switch(
                                                checked = customCropState.isAspectRatioLocked,
                                                onCheckedChange = { customCropState.toggleLockAspectRatio(imgW, imgH) }
                                            )
                                        }
                                    }

                                    // Switch Cuadrícula de tercios (3×3)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Guías de composición (3×3)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                            Switch(
                                                checked = customCropState.showGrid,
                                                onCheckedChange = { customCropState.showGrid = it }
                                            )
                                        }
                                    }

                                    // Dimensiones en tiempo real
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Ancho Real", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                                Text("$pixelW px", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Alto Real", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                                Text("$pixelH px", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Proporción", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                                Text(String.format(java.util.Locale.US, "%.2f:1", ratioVal), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                            }
                                        }
                                    }

                                    // Botones Cancelar y Aplicar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                customCropState.syncFrom(crop)
                                                isCustomCropActive = false
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cancelar")
                                        }

                                        Button(
                                            onClick = {
                                                onIntent(
                                                    EditorIntent.ApplyCustomFreeCrop(
                                                        customCropState.cropLeftNorm,
                                                        customCropState.cropTopNorm,
                                                        customCropState.cropRightNorm,
                                                        customCropState.cropBottomNorm
                                                    )
                                                )
                                                isCustomCropActive = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Aplicar")
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Recorte y Encuadre Snapseed", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
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
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

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

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Slider de Enderezado Fino (Straighten con Snapping a 0°)
                                AdjustmentSlider(
                                    label = "Enderezar (Ángulo fino)",
                                    value = crop.fineStraightenAngle,
                                    min = -45.0f,
                                    max = 45.0f,
                                    defaultValue = 0.0f,
                                    unitSuffix = "°",
                                    onValueChangeFinished = {
                                        onIntent(EditorIntent.UpdateStraightenAngle(crop.fineStraightenAngle, isFinished = true))
                                    },
                                    onValueChange = {
                                        val angleWithSnap = if (Math.abs(it) < 0.8f) 0f else it
                                        cropUiState.applyStraighten(angleWithSnap)
                                        onIntent(EditorIntent.UpdateStraightenAngle(angleWithSnap, isFinished = false))
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Regla de Tercios Toggle & Reset
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Guías de composición (3×3)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    Switch(
                                        checked = cropUiState.showRuleOfThirds,
                                        onCheckedChange = {
                                            cropUiState.showRuleOfThirds = it
                                            onIntent(EditorIntent.ToggleCropRuleOfThirds(it))
                                        }
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Presets de Relación de Aspecto
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Proporción de Recorte",
                                        color = MaterialTheme.colorScheme.onSurface,
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
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) else null,
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
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
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
            Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MaskCard(
    mask: MaskModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleEnabled: () -> Unit,
    onToggleInvert: () -> Unit,
    onToggleMode: () -> Unit,
    onSelectionTypeChange: (SelectionToolType) -> Unit,
    onClearSelection: () -> Unit,
    onFeatherChange: (Float) -> Unit,
    onAdjustmentsChange: (EditOperation.Adjustments) -> Unit,
    onDelete: () -> Unit
) {
    val adj = mask.localAdjustments
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
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
                            tint = if (mask.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = mask.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Modo Añadir / Quitar
                    OutlinedButton(
                        onClick = onToggleMode,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (mask.selectionMode == SelectionMode.ADD) {
                                Color(0xFFE91E63).copy(alpha = 0.2f)
                            } else {
                                Color(0xFF2196F3).copy(alpha = 0.2f)
                            }
                        ),
                        modifier = Modifier.height(26.dp)
                    ) {
                        val modeLabel = if (mask.selectionMode == SelectionMode.ADD) "+ Añadir" else "- Quitar"
                        Text(text = modeLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Invertir
                    OutlinedButton(
                        onClick = onToggleInvert,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (mask.isInverted) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (mask.isInverted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Invertir", fontSize = 10.sp)
                    }

                    // Eliminar
                    IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Selector de Forma de Selección
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MaskToolChip(
                    label = "Rect",
                    icon = Icons.Default.CropSquare,
                    modifier = Modifier.weight(1f)
                ) {
                    onSelectionTypeChange(SelectionToolType.RECTANGLE)
                }
                MaskToolChip(
                    label = "Óvalo",
                    icon = Icons.Default.RadioButtonUnchecked,
                    modifier = Modifier.weight(1f)
                ) {
                    onSelectionTypeChange(SelectionToolType.ELLIPSE)
                }
                MaskToolChip(
                    label = "Lazo",
                    icon = Icons.Default.Polyline,
                    modifier = Modifier.weight(1f)
                ) {
                    onSelectionTypeChange(SelectionToolType.LASSO)
                }
                MaskToolChip(
                    label = "Pincel",
                    icon = Icons.Default.Brush,
                    modifier = Modifier.weight(1f)
                ) {
                    onSelectionTypeChange(SelectionToolType.BRUSH)
                }
            }

            // Botón Limpiar Selección
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onClearSelection,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpiar selección", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

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
    onToggleFlipH: () -> Unit = {},
    onToggleFlipV: () -> Unit = {},
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
        LayerType.DOUBLE_EXPOSURE -> "Doble Exp."
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
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
                            tint = if (layer.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = layer.name,
                            color = if (layer.isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            tint = if (!isTop) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
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
                            tint = if (!isBottom) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
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
                Text(text = "Fusión:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { expandedBlendMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = layer.blendMode.name, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = expandedBlendMenu,
                        onDismissRequest = { expandedBlendMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        LayerBlendMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name, color = if (layer.blendMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
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
                Text(text = "Opacidad", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(text = "${(layer.opacity * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }

            Slider(
                value = layer.opacity,
                onValueChange = onOpacityChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surface
                )
            )

            // Layer Transform Controls (For Text, Sticker, Duplicate Image, Double Exposure)
            if (layer.layerType == LayerType.TEXT || layer.layerType == LayerType.STICKER || layer.layerType == LayerType.IMAGE_DUPLICATE || layer.layerType == LayerType.DOUBLE_EXPOSURE) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTransformControls = !showTransformControls }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transformación (Posición / Zoom / Giro / Flip)",
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onToggleFlipH,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (layer.flipHorizontal) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Voltear H", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onToggleFlipV,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (layer.flipVertical) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Voltear V", fontSize = 11.sp)
                        }
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
            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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
    onValueChangeFinished: (() -> Unit)? = null,
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
                color = if (isModified) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            val formatted = if (unitSuffix == "%") {
                "${(value * 100).toInt()}%"
            } else {
                "${String.format("%.2f", value)}$unitSuffix"
            }
            Text(
                text = formatted,
                color = if (isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = if (isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
