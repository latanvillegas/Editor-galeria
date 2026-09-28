package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hypereditor.nativegallery.ui.HyperEditorScreen
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

private val LegacyEditorHeaderHeight = 56.dp

/**
 * Incremental desktop host around the proven editor implementation.
 *
 * Desktop chrome owns close/undo/redo/save, so the legacy 56dp editor header is
 * clipped out here. The legacy editor remains mounted below that boundary while
 * its interactive canvases and option panels are extracted incrementally.
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
                modifier = Modifier.weight(1f).fillMaxSize()
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    HyperEditorScreen(
                        state = state,
                        onIntent = onIntent,
                        onClose = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredHeight(maxHeight + LegacyEditorHeaderHeight)
                            .offset(y = -LegacyEditorHeaderHeight)
                    )
                }
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
