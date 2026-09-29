package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class DesktopBrushPreset(val label: String, val size: Float, val opacity: Float, val hardness: Float, val flow: Float, val smoothing: Float)

class DesktopBrushState {
    var size by mutableStateOf(24f)
    var opacity by mutableStateOf(1f)
    var hardness by mutableStateOf(1f)
    var flow by mutableStateOf(1f)
    var smoothing by mutableStateOf(0.35f)
    var isEraser by mutableStateOf(false)
    var activePresetLabel by mutableStateOf("Medio")
        private set

    val presets = listOf(
        DesktopBrushPreset("Fino", 6f, 1f, 1f, 1f, 0.2f),
        DesktopBrushPreset("Medio", 24f, 1f, 1f, 1f, 0.35f),
        DesktopBrushPreset("Suave", 48f, 0.35f, 0.25f, 0.45f, 0.55f),
        DesktopBrushPreset("Grande", 96f, 0.75f, 0.7f, 0.8f, 0.45f)
    )

    fun adjustSize(delta: Float) { size = (size + delta).coerceIn(1f, 300f); markCustom() }
    fun setOpacity(value: Float) { opacity = value.coerceIn(0.05f, 1f); markCustom() }
    fun adjustOpacity(delta: Float) { setOpacity(opacity + delta) }
    fun setHardness(value: Float) { hardness = value.coerceIn(0f, 1f); markCustom() }
    fun adjustHardness(delta: Float) { setHardness(hardness + delta) }
    fun setFlow(value: Float) { flow = value.coerceIn(0.05f, 1f); markCustom() }
    fun adjustFlow(delta: Float) { setFlow(flow + delta) }
    fun setSmoothing(value: Float) { smoothing = value.coerceIn(0f, 0.9f); markCustom() }
    fun adjustSmoothing(delta: Float) { setSmoothing(smoothing + delta) }

    fun applyPreset(preset: DesktopBrushPreset) {
        size = preset.size; opacity = preset.opacity; hardness = preset.hardness; flow = preset.flow; smoothing = preset.smoothing
        isEraser = false; activePresetLabel = preset.label
    }

    fun toggleEraser() { isEraser = !isEraser }
    private fun markCustom() { activePresetLabel = "Personalizado" }
}

val LocalDesktopBrushState = compositionLocalOf<DesktopBrushState> { error("DesktopBrushState not provided") }
