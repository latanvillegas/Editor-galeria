package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hypereditor.nativegallery.ui.HyperEditorScreen
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

/**
 * Incremental desktop host around the proven editor implementation.
 *
 * The legacy editor still owns the central editing implementation while desktop
 * panels are extracted one by one. This keeps every existing rendering path alive
 * during the migration and avoids a big-bang rewrite.
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
            Box(Modifier.weight(1f).fillMaxSize()) {
                HyperEditorScreen(
                    state = state,
                    onIntent = onIntent,
                    onClose = onClose
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
