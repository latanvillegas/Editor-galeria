package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.SelectionMode
import com.hypereditor.nativegallery.render.BitmapRenderer
import com.hypereditor.nativegallery.ui.canvas.*
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorSectionTab
import com.hypereditor.nativegallery.ui.state.EditorUiState

@Composable
fun DesktopInteractiveCanvasHost(state: EditorUiState, onIntent: (EditorIntent) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    when (state.selectedTab) {
        EditorSectionTab.ADJUSTMENTS, EditorSectionTab.FILTERS_PRESETS, EditorSectionTab.LAYERS -> DesktopNativePreviewCanvas(state, modifier)
        EditorSectionTab.GEOMETRY_CROP -> DesktopNativeCropCanvas(state, onIntent, modifier)
        EditorSectionTab.MASKS_SELECTIONS -> DesktopNativeMaskCanvas(state, onIntent, modifier)
        EditorSectionTab.CREATIVE_TOOLS -> DesktopNativeCreativeCanvas(state, onIntent, modifier)
    }
}

@Composable private fun DesktopNativePreviewCanvas(state: EditorUiState, modifier: Modifier = Modifier) {
    val viewportState = rememberCanvasViewportState(); val bitmap = if (state.isComparingOriginal) state.originalBitmap else state.previewBitmap ?: state.originalBitmap
    EditorCanvas(bitmap = bitmap, viewportState = viewportState, modifier = modifier.fillMaxSize())
}

@Composable private fun DesktopNativeCropCanvas(state: EditorUiState, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val transform = state.document?.cropTransform ?: EditOperation.CropTransform(); val cropState = rememberCropUiState(transform)
    LaunchedEffect(transform) { if (!cropState.isInteracting) cropState.syncFrom(transform, 0f, 0f) }
    CropInteractiveCanvas(bitmap = state.originalBitmap, cropState = cropState, onCropTransformChanged = { onIntent(EditorIntent.UpdateCropTransform(it)) }, onInteractionStart = { onIntent(EditorIntent.BeginCropInteraction) }, onInteractionEnd = { onIntent(EditorIntent.CommitCropTransform("Recorte interactivo desktop")) }, modifier = modifier.fillMaxSize())
}

@Composable private fun DesktopNativeMaskCanvas(state: EditorUiState, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val masks = state.document?.masks.orEmpty(); val activeMask = masks.firstOrNull { it.id == state.activeMaskId } ?: masks.firstOrNull(); var isEraserMode by remember { mutableStateOf(false) }; val bitmap = state.previewBitmap ?: state.originalBitmap
    MaskSelectionInteractiveCanvas(bitmap = bitmap, activeMask = activeMask, onUpdateRectBounds = { b -> activeMask?.let { onIntent(EditorIntent.UpdateMaskRectBounds(it.id, b)) } }, onUpdateEllipseBounds = { b -> activeMask?.let { onIntent(EditorIntent.UpdateMaskEllipseBounds(it.id, b)) } }, onUpdateLassoPoints = { p -> activeMask?.let { onIntent(EditorIntent.UpdateMaskLassoPoints(it.id, p)) } }, onAddBrushStroke = { s -> activeMask?.let { onIntent(EditorIntent.AddMaskBrushStroke(it.id, s)) } }, onClearSelection = { activeMask?.let { onIntent(EditorIntent.ClearMask(it.id)) } }, onToggleSelectionMode = { activeMask?.let { onIntent(EditorIntent.UpdateMaskSelectionMode(it.id, if (it.selectionMode == SelectionMode.ADD) SelectionMode.SUBTRACT else SelectionMode.ADD)) } }, brushSizeNorm = 0.05f, isEraserMode = isEraserMode, onToggleEraserMode = { isEraserMode = !isEraserMode }, modifier = modifier.fillMaxSize())
}

@Composable
private fun DesktopNativeCreativeCanvas(state: EditorUiState, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val tool = LocalDesktopCreativeSelection.current.selected
    val bitmap = state.previewBitmap ?: state.originalBitmap
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var cloneMode by remember { mutableStateOf(CloneMode.SELECT_ORIGIN) }; var cloneOrigin by remember { mutableStateOf<Offset?>(null) }
    var healingMode by remember { mutableStateOf(HealingToolMode.TAP) }; var healingSampling by remember { mutableStateOf(HealingSamplingMode.AUTO) }; var healingSource by remember { mutableStateOf<Offset?>(null) }
    var portraitLight by remember { mutableStateOf(state.document?.portraitLights?.firstOrNull() ?: EditOperation.PortraitLight(exposure = 0.35f, shadows = 0.2f, highlights = 0.15f, temperature = 0.05f, feather = 0.6f, opacity = 1f)) }
    var facialZoneType by remember { mutableStateOf(EditOperation.FacialZoneType.FOREHEAD) }
    var facialZones by remember { mutableStateOf(state.document?.facialRelights?.firstOrNull()?.zones.orEmpty()) }

    when (tool) {
        DesktopCreativeTool.BRUSH -> BrushInteractiveCanvas(bitmap = bitmap, brushColor = android.graphics.Color.RED, brushSize = 24f, brushOpacity = 1f, isEraserMode = false, onApplyStroke = { onIntent(EditorIntent.AddBrushStroke(it)) }, modifier = modifier.fillMaxSize())
        DesktopCreativeTool.TEXT -> {
            val textBitmap = remember(bitmap, state.document?.textOverlays?.size) { val src = state.originalBitmap; val doc = state.document; if (src != null && doc != null && doc.textOverlays.isNotEmpty()) BitmapRenderer.renderDocument(src, doc.copy(textOverlays = emptyList()), isPreview = true) else bitmap }
            TextInteractiveCanvas(bitmap = textBitmap, textOverlays = state.document?.textOverlays.orEmpty(), selectedTextId = selectedTextId, onSelectText = { selectedTextId = it }, onUpdateText = { onIntent(EditorIntent.UpdateTextOverlay(it)) }, onDeleteText = { selectedTextId = null; onIntent(EditorIntent.DeleteTextOverlay(it)) }, modifier = modifier.fillMaxSize())
        }
        DesktopCreativeTool.CLONE -> CloneStampInteractiveCanvas(bitmap = bitmap, cloneMode = cloneMode, onCloneModeChanged = { cloneMode = it }, originNorm = cloneOrigin, onOriginSelected = { cloneOrigin = it }, stampRadius = 45f, stampHardness = 0.5f, stampOpacity = 1f, stampFlow = 1f, onApplyStamps = { onIntent(EditorIntent.AddCloneStampBatch(it)) }, onClearStamps = { onIntent(EditorIntent.ClearCloneStamps) }, stampsCount = state.document?.cloneStamps?.size ?: 0, modifier = modifier.fillMaxSize())
        DesktopCreativeTool.HEALING -> HealingInteractiveCanvas(bitmap = bitmap, toolMode = healingMode, samplingMode = healingSampling, onToolModeChanged = { healingMode = it }, onSamplingModeChanged = { healingSampling = it }, radius = 32f, feather = 0.5f, strength = 1f, manualSourceNorm = healingSource, onManualSourceSelected = { healingSource = it }, onApplyStroke = { onIntent(EditorIntent.AddHealingStroke(it)) }, strokesCount = state.document?.healingStrokes?.size ?: 0, modifier = modifier.fillMaxSize())
        DesktopCreativeTool.PATCH -> PatchInteractiveCanvas(bitmap = bitmap, radius = 45f, feather = 0.5f, strength = 1f, onApplyPatch = { onIntent(EditorIntent.AddPatchOperation(it)) }, patchesCount = state.document?.patchOperations?.size ?: 0, modifier = modifier.fillMaxSize())
        DesktopCreativeTool.PORTRAIT_LIGHT -> PortraitLightInteractiveCanvas(bitmap = bitmap, portraitLight = portraitLight, onLightChanged = { portraitLight = it; onIntent(EditorIntent.UpdatePortraitLight(it)) }, modifier = modifier.fillMaxSize())
        DesktopCreativeTool.FACIAL_RELIGHT -> FacialRelightInteractiveCanvas(bitmap = bitmap, zones = facialZones, selectedZoneType = facialZoneType, onZoneSelected = { facialZoneType = it }, onZoneChanged = { updated -> facialZones = facialZones.map { if (it.zoneType == updated.zoneType) updated else it }; onIntent(EditorIntent.UpdateFacialRelightZones(facialZones)) }, modifier = modifier.fillMaxSize())
    }
}
