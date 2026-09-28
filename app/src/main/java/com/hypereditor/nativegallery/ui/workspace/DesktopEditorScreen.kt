package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hypereditor.nativegallery.ui.HyperEditorScreen
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

private val LegacyEditorHeaderHeight = 56.dp
private val LegacyEditorOptionsWidth = 360.dp

/**
 * Incremental desktop host around the proven editor implementation.
 *
 * Desktop chrome owns close/undo/redo/save and the desktop inspector now owns the
 * migrated option panels. We therefore crop the legacy 56dp header and the legacy
 * right-side options panel while keeping the proven interactive canvas mounted.
 * This removes duplicated UI without rewriting the gesture/rendering engine.
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
                    // HyperEditorScreen still lays out its interactive canvas beside a
                    // legacy fixed-width options panel. Give it that extra width, then
                    // clip the panel outside this desktop stage. The canvas receives the
                    // full visible width and keeps all existing gesture behavior.
                    Box(
                        Modifier
                            .requiredWidth(maxWidth + LegacyEditorOptionsWidth)
                            .requiredHeight(maxHeight + LegacyEditorHeaderHeight)
                            .offset(y = -LegacyEditorHeaderHeight)
                    ) {
                        HyperEditorScreen(
                            state = state,
                            onIntent = onIntent,
                            onClose = onClose
                        )
                    }
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
