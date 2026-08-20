package com.hypereditor.nativegallery.ui.state

import android.net.Uri
import com.hypereditor.nativegallery.domain.model.*

enum class EditorSectionTab {
    ADJUSTMENTS,
    FILTERS_PRESETS,
    MASKS_SELECTIONS,
    CREATIVE_TOOLS,
    LAYERS,
    GEOMETRY_CROP
}

sealed interface EditorIntent {
    data class InitializeWithUri(val uri: Uri) : EditorIntent
    data class SelectTab(val tab: EditorSectionTab) : EditorIntent

    // Adjustments
    data class UpdateAdjustments(val adjustments: EditOperation.Adjustments) : EditorIntent

    // Filters & Presets
    data class ApplyFilter(val name: String, val intensity: Float = 1.0f) : EditorIntent
    data class UpdateFilterIntensity(val intensity: Float) : EditorIntent
    data object ClearFilter : EditorIntent
    data class SaveUserPreset(val name: String) : EditorIntent
    data class ApplyUserPreset(val preset: UserPreset) : EditorIntent
    data class DeleteUserPreset(val presetId: String) : EditorIntent

    // Creative & Retouch Tools (Brush, Eraser, Text, Clone Stamp)
    data class AddBrushStroke(val stroke: EditOperation.BrushDraw) : EditorIntent
    data object ClearBrushStrokes : EditorIntent
    data class AddTextOverlay(
        val text: String,
        val posX: Float = 0.1f,
        val posY: Float = 0.5f,
        val textSize: Float = 48f,
        val colorInt: Int = android.graphics.Color.WHITE,
        val alignment: Int = 0,
        val opacity: Float = 1.0f,
        val fontFamilyName: String = "SANS_SERIF"
    ) : EditorIntent
    data class UpdateTextOverlay(val textItem: EditOperation.TextOverlay) : EditorIntent
    data class DeleteTextOverlay(val textId: String) : EditorIntent
    data class AddCloneStamp(val sourceX: Float, val sourceY: Float, val targetX: Float, val targetY: Float, val radius: Float = 40f) : EditorIntent
    data object ClearCloneStamps : EditorIntent

    // Masks & Selections
    data class AddMask(val selectionType: SelectionToolType) : EditorIntent
    data class ToggleMaskEnabled(val maskId: String) : EditorIntent
    data class ToggleMaskInvert(val maskId: String) : EditorIntent
    data class UpdateMaskFeather(val maskId: String, val feather: Float) : EditorIntent
    data class UpdateMaskSelectionType(val maskId: String, val type: SelectionToolType) : EditorIntent
    data class UpdateMaskRectBounds(val maskId: String, val bounds: RectNorm) : EditorIntent
    data class UpdateMaskEllipseBounds(val maskId: String, val bounds: RectNorm) : EditorIntent
    data class AddMaskBrushStroke(val maskId: String, val stroke: MaskBrushStroke) : EditorIntent
    data class ClearMaskBrushStrokes(val maskId: String) : EditorIntent
    data class UpdateMaskLocalAdjustments(val maskId: String, val adjustments: EditOperation.Adjustments) : EditorIntent
    data class DeleteMask(val maskId: String) : EditorIntent
    data class SelectActiveMask(val maskId: String?) : EditorIntent

    // Layers
    data class AddColorLayer(val name: String, val colorHex: Long, val blendMode: LayerBlendMode = LayerBlendMode.OVERLAY, val opacity: Float = 0.5f) : EditorIntent
    data class AddDuplicateImageLayer(val name: String = "Capa Duplicada", val blendMode: LayerBlendMode = LayerBlendMode.SCREEN, val opacity: Float = 0.7f) : EditorIntent
    data class AddTextLayer(val text: String, val textSize: Float = 48f, val textColor: Long = 0xFFFFFFFF, val blendMode: LayerBlendMode = LayerBlendMode.NORMAL, val opacity: Float = 1.0f) : EditorIntent
    data class AddStickerLayer(val emoji: String, val blendMode: LayerBlendMode = LayerBlendMode.NORMAL, val opacity: Float = 1.0f) : EditorIntent
    data class UpdateLayerTransform(val layerId: String, val offsetX: Float, val offsetY: Float, val scale: Float, val rotation: Float) : EditorIntent
    data class ToggleLayerVisibility(val layerId: String) : EditorIntent
    data class UpdateLayerOpacity(val layerId: String, val opacity: Float) : EditorIntent
    data class UpdateLayerBlendMode(val layerId: String, val blendMode: LayerBlendMode) : EditorIntent
    data class MoveLayerUp(val layerId: String) : EditorIntent
    data class MoveLayerDown(val layerId: String) : EditorIntent
    data class DeleteLayer(val layerId: String) : EditorIntent
    data class SelectActiveLayer(val layerId: String?) : EditorIntent

    // Geometry & Crop
    data class UpdateCropTransform(val cropTransform: EditOperation.CropTransform) : EditorIntent
    data class SetCropAspectRatio(val aspectRatio: CropAspectRatio) : EditorIntent
    data class SetCropScaleMode(val scaleMode: CropScaleMode) : EditorIntent
    data class ToggleCropRuleOfThirds(val show: Boolean) : EditorIntent
    data object Rotate90Clockwise : EditorIntent
    data object Rotate90CounterClockwise : EditorIntent
    data object ToggleFlipHorizontal : EditorIntent
    data object ToggleFlipVertical : EditorIntent
    data class UpdateStraightenAngle(val angle: Float) : EditorIntent
    data class ApplyCropNorm(val left: Float, val top: Float, val right: Float, val bottom: Float) : EditorIntent
    data class ApplyAspectRatioCrop(val ratioW: Float, val ratioH: Float) : EditorIntent
    data object ResetCrop : EditorIntent
    data object ResetGeometry : EditorIntent

    // Global
    data class SetCompareOriginalMode(val isComparing: Boolean) : EditorIntent
    data object Undo : EditorIntent
    data object Redo : EditorIntent
    data object SaveAndExport : EditorIntent
}
