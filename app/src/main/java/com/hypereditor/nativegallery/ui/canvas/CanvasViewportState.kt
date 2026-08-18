package com.hypereditor.nativegallery.ui.canvas

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

class CanvasViewportState {
    var zoom by mutableFloatStateOf(1.0f)
    var panX by mutableFloatStateOf(0f)
    var panY by mutableFloatStateOf(0f)
    var rotationDegrees by mutableFloatStateOf(0f)

    fun reset() {
        zoom = 1.0f
        panX = 0f
        panY = 0f
        rotationDegrees = 0f
    }

    fun onDoubleTap() {
        if (zoom > 1.2f) reset() else zoom = 2.5f
    }
}

fun Modifier.editorCanvasGestures(
    viewportState: CanvasViewportState
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures(onDoubleTap = { viewportState.onDoubleTap() })
    }
    .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoomFactor, rotationFactor ->
            viewportState.zoom = (viewportState.zoom * zoomFactor).coerceIn(0.5f, 8.0f)
            viewportState.rotationDegrees += rotationFactor
            viewportState.panX += pan.x
            viewportState.panY += pan.y
        }
    }
