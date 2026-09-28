package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DesktopWorkspaceShell(
    selectedTool: DesktopTool,
    onToolSelected: (DesktopTool) -> Unit,
    documentSize: String?,
    zoomPercent: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onResetView: () -> Unit,
    canvas: @Composable RowScope.() -> Unit,
    inspector: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        DesktopMenuBar(
            onClose = onClose,
            onUndo = onUndo,
            onRedo = onRedo,
            onSave = onSave,
            canUndo = canUndo,
            canRedo = canRedo,
            isSaving = isSaving
        )
        DesktopToolOptionsBar(activeTool = selectedTool, documentSize = documentSize)
        Row(Modifier.fillMaxWidth().weight(1f)) {
            DesktopToolBar(selected = selectedTool, onSelect = onToolSelected)
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF181818)),
                content = canvas
            )
            Surface(
                modifier = Modifier.widthIn(min = 260.dp, max = 360.dp).fillMaxHeight(),
                color = Color(0xFF252525)
            ) { inspector() }
        }
        DesktopStatusBar(
            zoomPercent = zoomPercent,
            documentSize = documentSize,
            onZoomOut = onZoomOut,
            onZoomIn = onZoomIn,
            onResetView = onResetView
        )
    }
}
