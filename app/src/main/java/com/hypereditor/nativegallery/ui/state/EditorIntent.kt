package com.hypereditor.nativegallery.ui.state

import android.net.Uri
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.LayerBlendMode
import com.hypereditor.nativegallery.ui.tools.ToolType

sealed interface EditorIntent {
    data class InitializeWithUri(val uri: Uri) : EditorIntent
    data class SelectTool(val toolType: ToolType) : EditorIntent
    data class UpdateAdjustments(val adjustments: EditOperation.Adjustments) : EditorIntent
    data object Rotate90Clockwise : EditorIntent
    data object ToggleFlipHorizontal : EditorIntent
    data object ToggleFlipVertical : EditorIntent
    data class UpdateStraightenAngle(val angle: Float) : EditorIntent
    data class UpdatePerspective(val h: Float, val v: Float) : EditorIntent
    data class ApplyPresetFilter(val filterName: String, val intensity: Float) : EditorIntent
    data class AddBrushStroke(val stroke: EditOperation.BrushDraw) : EditorIntent
    data class AddTextOverlay(val text: EditOperation.TextOverlay) : EditorIntent
    data class AddCloneStamp(val stamp: EditOperation.CloneStampPoint) : EditorIntent
    data class SetCompareOriginalMode(val isComparing: Boolean) : EditorIntent
    data class AddLayer(val name: String) : EditorIntent
    data class ToggleLayerVisibility(val layerId: String) : EditorIntent
    data class SetLayerBlendMode(val layerId: String, val mode: LayerBlendMode) : EditorIntent
    data object Undo : EditorIntent
    data object Redo : EditorIntent
    data object SaveAndExport : EditorIntent
}
