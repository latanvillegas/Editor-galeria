package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import com.hypereditor.nativegallery.ui.canvas.rememberCanvasViewportState
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

@Composable
fun DesktopEditorScreen(state: EditorUiState, onIntent: (EditorIntent) -> Unit, onClose: () -> Unit) {
    var selectedTool by remember(state.selectedTab) { mutableStateOf(desktopToolFor(state.selectedTab, creativeToolIndex = 0)) }
    var isSpaceHandActive by remember { mutableStateOf(false) }
    val creativeSelection = remember { DesktopCreativeSelectionState() }
    val colorState = remember { DesktopColorState() }
    val brushState = remember { DesktopBrushState() }
    val viewportState = rememberCanvasViewportState()
    val focusRequester = remember { FocusRequester() }
    val documentSize = state.originalBitmap?.let { "${it.width} × ${it.height} px" }
    val effectiveTool = if (isSpaceHandActive) DesktopTool.HAND else selectedTool

    fun selectTool(tool: DesktopTool) {
        selectedTool = tool
        tool.target().creativeToolIndex?.let { index -> creativeSelection.selected = DesktopCreativeTool.entries[index.coerceIn(0, 3)] }
        onIntent(EditorIntent.SelectTab(tool.target().tab))
    }
    fun zoomBy(factor: Float) { viewportState.zoom = (viewportState.zoom * factor).coerceIn(viewportState.minZoom, viewportState.maxZoom) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    CompositionLocalProvider(
        LocalDesktopCreativeSelection provides creativeSelection,
        LocalDesktopColorState provides colorState,
        LocalDesktopBrushState provides brushState
    ) {
        DesktopWorkspaceShell(
            selectedTool = effectiveTool,
            onToolSelected = ::selectTool,
            documentSize = documentSize,
            zoomPercent = viewportState.zoomPercentage,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            isSaving = state.isExporting,
            onUndo = { onIntent(EditorIntent.Undo) },
            onRedo = { onIntent(EditorIntent.Redo) },
            onSave = { onIntent(EditorIntent.SaveAndExport()) },
            onClose = onClose,
            onZoomOut = { zoomBy(1f / 1.15f) },
            onZoomIn = { zoomBy(1.15f) },
            onResetView = { viewportState.reset() },
            modifier = Modifier.focusRequester(focusRequester).focusable().onPreviewKeyEvent { event ->
                if (event.key == Key.Spacebar) { isSpaceHandActive = event.type == KeyEventType.KeyDown; return@onPreviewKeyEvent true }
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val ctrl = event.isCtrlPressed
                when {
                    ctrl && event.key == Key.Z && event.isShiftPressed -> { if (state.canRedo) onIntent(EditorIntent.Redo); true }
                    ctrl && event.key == Key.Z -> { if (state.canUndo) onIntent(EditorIntent.Undo); true }
                    ctrl && event.key == Key.Y -> { if (state.canRedo) onIntent(EditorIntent.Redo); true }
                    ctrl && event.key == Key.S -> { if (!state.isExporting) onIntent(EditorIntent.SaveAndExport()); true }
                    ctrl && (event.key == Key.Equals || event.key == Key.NumPadAdd) -> { zoomBy(1.15f); true }
                    ctrl && (event.key == Key.Minus || event.key == Key.NumPadSubtract) -> { zoomBy(1f / 1.15f); true }
                    ctrl && event.key == Key.Zero -> { viewportState.reset(); true }
                    event.key == Key.X -> { colorState.swap(); true }
                    event.key == Key.D -> { colorState.reset(); true }
                    event.key == Key.LeftBracket && selectedTool == DesktopTool.BRUSH && event.isShiftPressed -> { brushState.adjustOpacity(-0.1f); true }
                    event.key == Key.RightBracket && selectedTool == DesktopTool.BRUSH && event.isShiftPressed -> { brushState.adjustOpacity(0.1f); true }
                    event.key == Key.LeftBracket && selectedTool == DesktopTool.BRUSH -> { brushState.adjustSize(-2f); true }
                    event.key == Key.RightBracket && selectedTool == DesktopTool.BRUSH -> { brushState.adjustSize(2f); true }
                    event.key == Key.E && selectedTool == DesktopTool.BRUSH -> { brushState.toggleEraser(); true }
                    event.key == Key.DirectionLeft && effectiveTool == DesktopTool.HAND -> { viewportState.panX += 32f; true }
                    event.key == Key.DirectionRight && effectiveTool == DesktopTool.HAND -> { viewportState.panX -= 32f; true }
                    event.key == Key.DirectionUp && effectiveTool == DesktopTool.HAND -> { viewportState.panY += 32f; true }
                    event.key == Key.DirectionDown && effectiveTool == DesktopTool.HAND -> { viewportState.panY -= 32f; true }
                    event.key == Key.V -> { selectTool(DesktopTool.MOVE); true }
                    event.key == Key.M -> { selectTool(DesktopTool.SELECT); true }
                    event.key == Key.C -> { selectTool(DesktopTool.CROP); true }
                    event.key == Key.B -> { selectTool(DesktopTool.BRUSH); true }
                    event.key == Key.J -> { selectTool(DesktopTool.HEAL); true }
                    event.key == Key.S && !ctrl -> { selectTool(DesktopTool.CLONE); true }
                    event.key == Key.T -> { selectTool(DesktopTool.TEXT); true }
                    event.key == Key.H -> { selectTool(DesktopTool.HAND); true }
                    event.key == Key.Z && !ctrl -> { selectTool(DesktopTool.ZOOM); true }
                    event.key == Key.Escape -> { onClose(); true }
                    else -> false
                }
            },
            canvas = {
                DesktopCanvasStage(documentSize = documentSize, activeTool = effectiveTool, zoomPercent = viewportState.zoomPercentage, viewportState = viewportState, modifier = Modifier.fillMaxSize()) {
                    DesktopInteractiveCanvasHost(state = state, onIntent = onIntent, viewportState = viewportState, modifier = Modifier.fillMaxSize())
                }
            },
            inspector = { DesktopInspectorPanel(state = state, onIntent = onIntent) }
        )
    }
}
