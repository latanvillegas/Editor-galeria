package com.hypereditor.nativegallery.ui.canvas

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

class CanvasViewportState(
    initialZoom: Float = 1.0f,
    val minZoom: Float = 0.25f,
    val maxZoom: Float = 12.0f
) {
    var zoom by mutableFloatStateOf(initialZoom)
    var panX by mutableFloatStateOf(0f)
    var panY by mutableFloatStateOf(0f)
    var rotationDegrees by mutableFloatStateOf(0f)

    val isModified: Boolean
        get() = zoom != 1.0f || panX != 0f || panY != 0f || rotationDegrees != 0f

    val zoomPercentage: Int
        get() = (zoom * 100).toInt()

    fun reset() {
        zoom = 1.0f
        panX = 0f
        panY = 0f
        rotationDegrees = 0f
    }

    fun onDoubleTap() {
        if (zoom > 1.2f || panX != 0f || panY != 0f) {
            reset()
        } else {
            zoom = 2.5f
        }
    }

    fun updateTransform(pan: androidx.compose.ui.geometry.Offset, zoomFactor: Float, rotationFactor: Float) {
        zoom = (zoom * zoomFactor).coerceIn(minZoom, maxZoom)
        rotationDegrees += rotationFactor
        panX += pan.x
        panY += pan.y
    }
}

@Composable
fun rememberCanvasViewportState(): CanvasViewportState {
    return remember { CanvasViewportState() }
}

fun Modifier.editorCanvasGestures(
    viewportState: CanvasViewportState
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = {
                viewportState.onDoubleTap()
            }
        )
    }
    .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoomFactor, rotationFactor ->
            viewportState.updateTransform(pan, zoomFactor, rotationFactor)
        }
    }
