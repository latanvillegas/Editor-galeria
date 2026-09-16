package com.hypereditor.nativegallery.ui.state

import android.graphics.Bitmap
import android.net.Uri
import com.hypereditor.nativegallery.domain.model.*

enum class EditorSectionTab {
    GEOMETRY_CROP,
    ADJUSTMENTS,
    FILTERS_PRESETS,
    CREATIVE_TOOLS,
    LAYERS,
    MASKS_SELECTIONS
}

sealed interface EditorIntent {
    data class InitializeWithUri(val uri: Uri) : EditorIntent
    data class SelectTab(val tab: EditorSectionTab) : EditorIntent

    // Adjustments
    data class UpdateAdjustments(
        val adjustments: EditOperation.Adjustments,
        val isFinished: Boolean = true,
        val actionLabel: String = "Ajustes de color"
    ) : EditorIntent

    // Filters & Presets
    data class ApplyFilter(val name: String, val intensity: Float = 1.0f) : EditorIntent
    data class UpdateFilterIntensity(val intensity: Float) : EditorIntent
    data object ClearFilter : EditorIntent
    data class SaveUserPreset(val name: String) : EditorIntent
    data class ApplyUserPreset(val preset: UserPreset) : EditorIntent
    data class DeleteUserPreset(val presetId: String) : EditorIntent

    // Creative & Retouch Tools (Brush, Eraser, Text, Clone Stamp / Healing)
    data class AddBrushStroke(val stroke: EditOperation.BrushDraw) : EditorIntent
    data object ClearBrushStrokes : EditorIntent
    data class AddTextOverlay(
        val text: String,
        val posX: Float = 0.5f,
        val posY: Float = 0.5f,
        val textSize: Float = 48f,
        val colorInt: Int = android.graphics.Color.WHITE,
        val alignment: Int = 0,
        val opacity: Float = 1.0f,
        val fontFamilyName: String = "SANS_SERIF",
        val rotationDegrees: Float = 0f,
        val scale: Float = 1.0f,
        val customFontPath: String? = null
    ) : EditorIntent
    data class UpdateTextOverlay(val textItem: EditOperation.TextOverlay) : EditorIntent
    data class DeleteTextOverlay(val textId: String) : EditorIntent
    data class AddCloneStamp(val sourceX: Float, val sourceY: Float, val targetX: Float, val targetY: Float, val radius: Float = 40f) : EditorIntent
    data class AddCloneStampBatch(val stamps: List<EditOperation.CloneStampPoint>) : EditorIntent
    data object ClearCloneStamps : EditorIntent
    data class AddHealingStroke(val stroke: EditOperation.HealingStroke) : EditorIntent
    data object ClearHealingStrokes : EditorIntent
    data class AddPatchOperation(val patch: EditOperation.PatchOperation) : EditorIntent
    data object ClearPatchOperations : EditorIntent

    // Portrait Light & Facial Relight
    data class UpdatePortraitLight(val light: EditOperation.PortraitLight, val isFinished: Boolean = true) : EditorIntent
    data object ClearPortraitLights : EditorIntent
    data class UpdateFacialRelight(val relight: EditOperation.FacialRelight, val isFinished: Boolean = true) : EditorIntent
    data class UpdateFacialRelightZones(val zones: List<EditOperation.FacialRelightZone>, val isFinished: Boolean = true) : EditorIntent
    data class UpdateFacialZone(val zone: EditOperation.FacialRelightZone, val isFinished: Boolean = true) : EditorIntent
    data object ClearFacialRelights : EditorIntent

    // Masks & Selections
    data class AddMask(val selectionType: SelectionToolType) : EditorIntent
    data class ToggleMaskEnabled(val maskId: String) : EditorIntent
    data class ToggleMaskInvert(val maskId: String) : EditorIntent
    data class UpdateMaskFeather(val maskId: String, val feather: Float) : EditorIntent
    data class UpdateMaskSelectionType(val maskId: String, val type: SelectionToolType) : EditorIntent
    data class UpdateMaskSelectionMode(val maskId: String, val mode: com.hypereditor.nativegallery.domain.model.SelectionMode) : EditorIntent
    data class UpdateMaskRectBounds(val maskId: String, val bounds: RectNorm) : EditorIntent
    data class UpdateMaskEllipseBounds(val maskId: String, val bounds: RectNorm) : EditorIntent
    data class UpdateMaskLassoPoints(val maskId: String, val points: List<Pair<Float, Float>>) : EditorIntent
    data class AddMaskBrushStroke(val maskId: String, val stroke: MaskBrushStroke) : EditorIntent
    data class ClearMaskBrushStrokes(val maskId: String) : EditorIntent
    data class ClearMask(val maskId: String) : EditorIntent
    data class UpdateMaskLocalAdjustments(val maskId: String, val adjustments: EditOperation.Adjustments) : EditorIntent
    data class DeleteMask(val maskId: String) : EditorIntent
    data class SelectActiveMask(val maskId: String?) : EditorIntent

    // Layers
    data class AddColorLayer(val name: String, val colorHex: Long, val blendMode: LayerBlendMode = LayerBlendMode.OVERLAY, val opacity: Float = 0.5f) : EditorIntent
    data class AddDuplicateImageLayer(val name: String = "Capa Duplicada", val blendMode: LayerBlendMode = LayerBlendMode.SCREEN, val opacity: Float = 0.7f) : EditorIntent
    data class AddDoubleExposureLayer(val bitmap: Bitmap, val name: String = "Doble Exposición", val blendMode: LayerBlendMode = LayerBlendMode.SCREEN, val opacity: Float = 0.75f) : EditorIntent
    data class AddTextLayer(val text: String, val textSize: Float = 48f, val textColor: Long = 0xFFFFFFFF, val blendMode: LayerBlendMode = LayerBlendMode.NORMAL, val opacity: Float = 1.0f) : EditorIntent
    data class AddStickerLayer(val emoji: String, val blendMode: LayerBlendMode = LayerBlendMode.NORMAL, val opacity: Float = 1.0f) : EditorIntent
    data class UpdateLayerTransform(val layerId: String, val offsetX: Float, val offsetY: Float, val scale: Float, val rotation: Float) : EditorIntent
    data class ToggleLayerFlipHorizontal(val layerId: String) : EditorIntent
    data class ToggleLayerFlipVertical(val layerId: String) : EditorIntent
    data class ToggleLayerVisibility(val layerId: String) : EditorIntent
    data class UpdateLayerOpacity(val layerId: String, val opacity: Float) : EditorIntent
    data class UpdateLayerBlendMode(val layerId: String, val blendMode: LayerBlendMode) : EditorIntent
    data class DuplicateLayer(val layerId: String) : EditorIntent
    data class MoveLayerUp(val layerId: String) : EditorIntent
    data class MoveLayerDown(val layerId: String) : EditorIntent
    data class DeleteLayer(val layerId: String) : EditorIntent
    data class SelectActiveLayer(val layerId: String?) : EditorIntent

    // Geometry & Crop Pro
    data class UpdateCropTransform(val cropTransform: EditOperation.CropTransform) : EditorIntent
    data class SetCropAspectRatio(val aspectRatio: CropAspectRatio) : EditorIntent
    data class SetCropScaleMode(val scaleMode: CropScaleMode) : EditorIntent
    data class ToggleCropRuleOfThirds(val show: Boolean) : EditorIntent
    data object Rotate90Clockwise : EditorIntent
    data object Rotate90CounterClockwise : EditorIntent
    data object ToggleFlipHorizontal : EditorIntent
    data object ToggleFlipVertical : EditorIntent
    data class UpdateStraightenAngle(val angle: Float, val isFinished: Boolean = true) : EditorIntent
    data object BeginCropInteraction : EditorIntent
    data class CommitCropTransform(val actionLabel: String = "Recorte y encuadre") : EditorIntent
    data class ApplyCropNorm(val left: Float, val top: Float, val right: Float, val bottom: Float) : EditorIntent
    data class ApplyCustomFreeCrop(val leftNorm: Float, val topNorm: Float, val rightNorm: Float, val bottomNorm: Float) : EditorIntent
    data class ApplyAspectRatioCrop(val ratioW: Float, val ratioH: Float) : EditorIntent
    data object ResetCrop : EditorIntent
    data object ResetGeometry : EditorIntent

    // Global
    data class SetCompareOriginalMode(val isComparing: Boolean) : EditorIntent
    data object Undo : EditorIntent
    data object Redo : EditorIntent
    data class SaveAndExport(
        val format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        val quality: Int = 95
    ) : EditorIntent
}
