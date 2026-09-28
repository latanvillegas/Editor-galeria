package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

@Composable
fun DesktopEditorScreen(state: EditorUiState, onIntent: (EditorIntent) -> Unit, onClose: () -> Unit) {
    var selectedTool by remember(state.selectedTab) { mutableStateOf(desktopToolFor(state.selectedTab, creativeToolIndex = 0)) }
    val creativeSelection = remember { DesktopCreativeSelectionState() }
    val documentSize = state.originalBitmap?.let { "${it.width} × ${it.height} px" }

    CompositionLocalProvider(LocalDesktopCreativeSelection provides creativeSelection) {
        DesktopWorkspaceShell(
            selectedTool = selectedTool,
            onToolSelected = { tool ->
                selectedTool = tool
                tool.target().creativeToolIndex?.let { index -> creativeSelection.selected = DesktopCreativeTool.entries[index.coerceIn(0, 3)] }
                onIntent(EditorIntent.SelectTab(tool.target().tab))
            },
            documentSize = documentSize,
            zoomPercent = 100,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            isSaving = state.isExporting,
            onUndo = { onIntent(EditorIntent.Undo) },
            onRedo = { onIntent(EditorIntent.Redo) },
            onSave = { onIntent(EditorIntent.SaveAndExport()) },
            onClose = onClose,
            canvas = { DesktopCanvasStage(documentSize = documentSize, modifier = Modifier.fillMaxSize()) { DesktopInteractiveCanvasHost(state = state, onIntent = onIntent, onClose = onClose, modifier = Modifier.fillMaxSize()) } },
            inspector = { DesktopInspectorPanel(state = state, onIntent = onIntent) }
        )
    }
}
