package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class DesktopColorState {
    var foreground by mutableStateOf(Color.Black)
    var background by mutableStateOf(Color.White)

    fun swap() {
        val current = foreground
        foreground = background
        background = current
    }

    fun reset() {
        foreground = Color.Black
        background = Color.White
    }
}

val LocalDesktopColorState = compositionLocalOf<DesktopColorState> {
    error("DesktopColorState not provided")
}
