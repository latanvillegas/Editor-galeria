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
import com.hypereditor.nativegallery.ui.HyperEditorScreen
import com.hypereditor.nativegallery.ui.canvas.CropInteractiveCanvas
import com.hypereditor.nativegallery.ui.canvas.EditorCanvas
import com.hypereditor.nativegallery.ui.canvas.rememberCanvasViewportState
import com.hypereditor.nativegallery.ui.canvas.rememberCropUiState
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorSectionTab
import com.hypereditor.nativegallery.ui.state.EditorUiState

/**
 * Compatibility boundary between the desktop workspace and the original editor.
 *
 * Passive sections and geometry now render directly through standalone canvas
 * composables. Only creative tools and masks still require the legacy bridge.
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

        EditorSectionTab.CREATIVE_TOOLS,
        EditorSectionTab.MASKS_SELECTIONS -> LegacyInteractiveCanvasBridge(
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
