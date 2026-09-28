package com.hypereditor.nativegallery.ui.workspace

import com.hypereditor.nativegallery.ui.state.EditorSectionTab

/**
 * Bridge between the new desktop workspace and the existing editor coordinator.
 * Keeping this mapping explicit lets us migrate the UI without duplicating render tools.
 */
data class DesktopToolTarget(
    val tab: EditorSectionTab,
    val creativeToolIndex: Int? = null
)

fun DesktopTool.target(): DesktopToolTarget = when (this) {
    DesktopTool.MOVE -> DesktopToolTarget(EditorSectionTab.BASIC_ADJUSTMENTS)
    DesktopTool.SELECT -> DesktopToolTarget(EditorSectionTab.MASKS_SELECTIONS)
    DesktopTool.CROP -> DesktopToolTarget(EditorSectionTab.GEOMETRY_CROP)
    DesktopTool.BRUSH -> DesktopToolTarget(EditorSectionTab.CREATIVE_TOOLS, creativeToolIndex = 0)
    DesktopTool.TEXT -> DesktopToolTarget(EditorSectionTab.CREATIVE_TOOLS, creativeToolIndex = 1)
    DesktopTool.CLONE -> DesktopToolTarget(EditorSectionTab.CREATIVE_TOOLS, creativeToolIndex = 2)
    DesktopTool.HEAL -> DesktopToolTarget(EditorSectionTab.CREATIVE_TOOLS, creativeToolIndex = 3)
    DesktopTool.HAND -> DesktopToolTarget(EditorSectionTab.BASIC_ADJUSTMENTS)
    DesktopTool.ZOOM -> DesktopToolTarget(EditorSectionTab.BASIC_ADJUSTMENTS)
}

fun desktopToolFor(tab: EditorSectionTab, creativeToolIndex: Int): DesktopTool = when {
    tab == EditorSectionTab.GEOMETRY_CROP -> DesktopTool.CROP
    tab == EditorSectionTab.MASKS_SELECTIONS -> DesktopTool.SELECT
    tab == EditorSectionTab.CREATIVE_TOOLS && creativeToolIndex == 0 -> DesktopTool.BRUSH
    tab == EditorSectionTab.CREATIVE_TOOLS && creativeToolIndex == 1 -> DesktopTool.TEXT
    tab == EditorSectionTab.CREATIVE_TOOLS && creativeToolIndex == 2 -> DesktopTool.CLONE
    tab == EditorSectionTab.CREATIVE_TOOLS && creativeToolIndex == 3 -> DesktopTool.HEAL
    else -> DesktopTool.MOVE
}
