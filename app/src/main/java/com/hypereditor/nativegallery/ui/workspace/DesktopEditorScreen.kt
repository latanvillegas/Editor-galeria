package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

@Composable
fun DesktopEditorScreen(state: EditorUiState, onIntent: (EditorIntent) -> Unit, onClose: () -> Unit) {
    var selectedTool by remember(state.selectedTab) { mutableStateOf(desktopToolFor(state.selectedTab, creativeToolIndex = 0)) }
    val creativeSelection = remember { DesktopCreativeSelectionState() }
    val focusRequester = remember { FocusRequester() }
    val documentSize = state.originalBitmap?.let { "${it.width} × ${it.height} px" }

    fun selectTool(tool: DesktopTool) {
        selectedTool = tool
        tool.target().creativeToolIndex?.let { index ->
            creativeSelection.selected = DesktopCreativeTool.entries[index.coerceIn(0, 3)]
        }
        onIntent(EditorIntent.SelectTab(tool.target().tab))
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    CompositionLocalProvider(LocalDesktopCreativeSelection provides creativeSelection) {
        DesktopWorkspaceShell(
            selectedTool = selectedTool,
            onToolSelected = ::selectTool,
            documentSize = documentSize,
            zoomPercent = 100,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            isSaving = state.isExporting,
            onUndo = { onIntent(EditorIntent.Undo) },
            onRedo = { onIntent(EditorIntent.Redo) },
            onSave = { onIntent(EditorIntent.SaveAndExport()) },
            onClose = onClose,
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val ctrl = event.isCtrlPressed
                    when {
                        ctrl && event.key == Key.Z && event.isShiftPressed -> { if (state.canRedo) onIntent(EditorIntent.Redo); true }
                        ctrl && event.key == Key.Z -> { if (state.canUndo) onIntent(EditorIntent.Undo); true }
                        ctrl && event.key == Key.Y -> { if (state.canRedo) onIntent(EditorIntent.Redo); true }
                        ctrl && event.key == Key.S -> { if (!state.isExporting) onIntent(EditorIntent.SaveAndExport()); true }
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
                DesktopCanvasStage(documentSize = documentSize, modifier = Modifier.fillMaxSize()) {
                    DesktopInteractiveCanvasHost(state = state, onIntent = onIntent, onClose = onClose, modifier = Modifier.fillMaxSize())
                }
            },
            inspector = { DesktopInspectorPanel(state = state, onIntent = onIntent) }
        )
    }
}
