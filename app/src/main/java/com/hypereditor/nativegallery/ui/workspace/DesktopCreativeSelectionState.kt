package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.runtime.*

@Stable
class DesktopCreativeSelectionState(initial: DesktopCreativeTool = DesktopCreativeTool.BRUSH) {
    var selected by mutableStateOf(initial)
}

val LocalDesktopCreativeSelection = staticCompositionLocalOf { DesktopCreativeSelectionState() }
