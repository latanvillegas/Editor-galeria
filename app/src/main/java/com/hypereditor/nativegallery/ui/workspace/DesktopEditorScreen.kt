package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

/**
 * Desktop editor composition root.
 *
 * This screen now depends only on the desktop workspace contracts. The compatibility
 * details required by the original HyperEditorScreen live behind
 * DesktopInteractiveCanvasHost and can be removed independently when the interactive
 * canvases finish their extraction.
 */
@Composable
fun DesktopEditorScreen(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    onClose: () -> Unit
) {
    var selectedTool by remember(state.selectedTab) {
        mutableStateOf(desktopToolFor(state.selectedTab, creativeToolIndex = 0))
    }

    val documentSize = state.originalBitmap?.let { "${it.width} × ${it.height} px" }

    DesktopWorkspaceShell(
        selectedTool = selectedTool,
        onToolSelected = { tool ->
            selectedTool = tool
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
        canvas = {
            DesktopCanvasStage(
                documentSize = documentSize,
                modifier = Modifier.fillMaxSize()
            ) {
                DesktopInteractiveCanvasHost(
                    state = state,
                    onIntent = onIntent,
                    onClose = onClose,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        inspector = {
            DesktopInspectorPanel(
                state = state,
                onIntent = onIntent
            )
        }
    )
}
