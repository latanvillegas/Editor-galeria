package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.SelectionMode
import com.hypereditor.nativegallery.ui.HyperEditorScreen
import com.hypereditor.nativegallery.ui.canvas.CropInteractiveCanvas
import com.hypereditor.nativegallery.ui.canvas.EditorCanvas
import com.hypereditor.nativegallery.ui.canvas.MaskSelectionInteractiveCanvas
import com.hypereditor.nativegallery.ui.canvas.rememberCanvasViewportState
import com.hypereditor.nativegallery.ui.canvas.rememberCropUiState
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorSectionTab
import com.hypereditor.nativegallery.ui.state.EditorUiState

/**
 * Compatibility boundary between the desktop workspace and the original editor.
 *
 * Preview, geometry and mask-selection sections now use standalone native canvas
 * composables. Only creative tools still require the legacy HyperEditorScreen bridge.
 */
@Composable
fun DesktopInteractiveCanvasHost(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state.selectedTab) {
        EditorSectionTab.ADJUSTMENTS,
        EditorSectionTab.FILTERS_PRESETS,
        EditorSectionTab.LAYERS -> DesktopNativePreviewCanvas(state, modifier)

        EditorSectionTab.GEOMETRY_CROP -> DesktopNativeCropCanvas(state, onIntent, modifier)
        EditorSectionTab.MASKS_SELECTIONS -> DesktopNativeMaskCanvas(state, onIntent, modifier)

        EditorSectionTab.CREATIVE_TOOLS -> LegacyInteractiveCanvasBridge(
            state = state,
            onIntent = onIntent,
            onClose = onClose,
            modifier = modifier
        )
    }
}

@Composable
private fun DesktopNativePreviewCanvas(state: EditorUiState, modifier: Modifier = Modifier) {
    val viewportState = rememberCanvasViewportState()
    val bitmap = if (state.isComparingOriginal) state.originalBitmap else state.previewBitmap ?: state.originalBitmap
    EditorCanvas(bitmap = bitmap, viewportState = viewportState, modifier = modifier.fillMaxSize())
}

@Composable
private fun DesktopNativeCropCanvas(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val transform = state.document?.cropTransform ?: EditOperation.CropTransform()
    val cropState = rememberCropUiState(transform)

    LaunchedEffect(transform) {
        if (!cropState.isInteracting) cropState.syncFrom(transform, 0f, 0f)
    }

    CropInteractiveCanvas(
        bitmap = state.originalBitmap,
        cropState = cropState,
        onCropTransformChanged = { onIntent(EditorIntent.UpdateCropTransform(it)) },
        onInteractionStart = { onIntent(EditorIntent.BeginCropInteraction) },
        onInteractionEnd = { onIntent(EditorIntent.CommitCropTransform("Recorte interactivo desktop")) },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun DesktopNativeMaskCanvas(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val masks = state.document?.masks.orEmpty()
    val activeMask = masks.firstOrNull { it.id == state.activeMaskId } ?: masks.firstOrNull()
    var brushSizeNorm by remember { mutableFloatStateOf(0.05f) }
    var isEraserMode by remember { mutableStateOf(false) }
    val bitmap = if (state.isComparingOriginal) state.originalBitmap else state.previewBitmap ?: state.originalBitmap

    MaskSelectionInteractiveCanvas(
        bitmap = bitmap,
        activeMask = activeMask,
        onUpdateRectBounds = { bounds -> activeMask?.let { onIntent(EditorIntent.UpdateMaskRectBounds(it.id, bounds)) } },
        onUpdateEllipseBounds = { bounds -> activeMask?.let { onIntent(EditorIntent.UpdateMaskEllipseBounds(it.id, bounds)) } },
        onUpdateLassoPoints = { points -> activeMask?.let { onIntent(EditorIntent.UpdateMaskLassoPoints(it.id, points)) } },
        onAddBrushStroke = { stroke -> activeMask?.let { onIntent(EditorIntent.AddMaskBrushStroke(it.id, stroke)) } },
        onClearSelection = { activeMask?.let { onIntent(EditorIntent.ClearMask(it.id)) } },
        onToggleSelectionMode = {
            activeMask?.let {
                val mode = if (it.selectionMode == SelectionMode.ADD) SelectionMode.SUBTRACT else SelectionMode.ADD
                onIntent(EditorIntent.UpdateMaskSelectionMode(it.id, mode))
            }
        },
        brushSizeNorm = brushSizeNorm,
        isEraserMode = isEraserMode,
        onToggleEraserMode = { isEraserMode = !isEraserMode },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun LegacyInteractiveCanvasBridge(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Box(
            Modifier
                .requiredWidth(maxWidth + LEGACY_OPTIONS_WIDTH)
                .requiredHeight(maxHeight + LEGACY_HEADER_HEIGHT)
                .offset(y = -LEGACY_HEADER_HEIGHT)
        ) {
            HyperEditorScreen(state = state, onIntent = onIntent, onClose = onClose)
        }
    }
}

private val LEGACY_HEADER_HEIGHT = 56.dp
private val LEGACY_OPTIONS_WIDTH = 380.dp
