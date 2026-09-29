package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DesktopBrushState {
    var size by mutableStateOf(24f)
    var opacity by mutableStateOf(1f)
    var isEraser by mutableStateOf(false)

    fun adjustSize(delta: Float) {
        size = (size + delta).coerceIn(1f, 300f)
    }

    fun toggleEraser() {
        isEraser = !isEraser
    }
}

val LocalDesktopBrushState = compositionLocalOf<DesktopBrushState> {
    error("DesktopBrushState not provided")
}
