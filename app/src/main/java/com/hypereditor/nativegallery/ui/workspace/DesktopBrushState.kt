package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class DesktopBrushPreset(val label: String, val size: Float, val opacity: Float)

class DesktopBrushState {
    var size by mutableStateOf(24f)
    var opacity by mutableStateOf(1f)
    var isEraser by mutableStateOf(false)

    val presets = listOf(
        DesktopBrushPreset("Fino", 6f, 1f),
        DesktopBrushPreset("Medio", 24f, 1f),
        DesktopBrushPreset("Suave", 48f, 0.35f),
        DesktopBrushPreset("Grande", 96f, 0.75f)
    )

    fun adjustSize(delta: Float) {
        size = (size + delta).coerceIn(1f, 300f)
    }

    fun adjustOpacity(delta: Float) {
        opacity = (opacity + delta).coerceIn(0.05f, 1f)
    }

    fun applyPreset(preset: DesktopBrushPreset) {
        size = preset.size
        opacity = preset.opacity
        isEraser = false
    }

    fun toggleEraser() {
        isEraser = !isEraser
    }
}

val LocalDesktopBrushState = compositionLocalOf<DesktopBrushState> {
    error("DesktopBrushState not provided")
}
