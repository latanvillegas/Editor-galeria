package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hypereditor.nativegallery.ui.HyperEditorScreen
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

/**
 * Compatibility boundary between the desktop workspace and the original editor.
 *
 * The desktop shell must not know the dimensions or layout details of the legacy
 * editor. Until the interactive canvases are extracted into standalone composables,
 * this host is the only place allowed to mount HyperEditorScreen and compensate for
 * its old header/options chrome.
 *
 * Keeping this bridge isolated makes the next extraction mechanical: replace the
 * implementation of this composable with DesktopInteractiveCanvas without touching
 * DesktopEditorScreen or DesktopWorkspaceShell.
 */
@Composable
fun DesktopInteractiveCanvasHost(
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
            HyperEditorScreen(
                state = state,
                onIntent = onIntent,
                onClose = onClose
            )
        }
    }
}

private val LEGACY_HEADER_HEIGHT = 56.dp
private val LEGACY_OPTIONS_WIDTH = 380.dp
