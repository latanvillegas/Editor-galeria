package com.hypereditor.nativegallery.domain.model

import android.graphics.Color
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

sealed interface EditOperation : Parcelable {

    @Parcelize
    data class Adjustments(
        val id: String = "adjustments",
        val brightness: Float = 0f,
        val contrast: Float = 1f,
        val saturation: Float = 1f,
        val temperature: Float = 0f,
        val tint: Float = 0f,
        val exposure: Float = 0f,
        val shadows: Float = 0f,
        val highlights: Float = 0f,
        val ambiance: Float = 0f,
        val structure: Float = 0f,
        val sharpness: Float = 0f,
        val gamma: Float = 1f,
        val vignette: Float = 0f,
        val blur: Float = 0f,
        val fade: Float = 0f,
        val grain: Float = 0f
    ) : EditOperation

    @Parcelize
    data class CropTransform(
        val id: String = "crop_transform",
        val cropLeftNorm: Float = 0f,
        val cropTopNorm: Float = 0f,
        val cropRightNorm: Float = 1f,
        val cropBottomNorm: Float = 1f,
        val rotation90Degrees: Int = 0,
        val fineStraightenAngle: Float = 0f,
        val flipHorizontal: Boolean = false,
        val flipVertical: Boolean = false,
        val perspectiveHorizontal: Float = 0f,
        val perspectiveVertical: Float = 0f,
        val expandRatio: Float = 0f,
        val scale: Float = 1.0f,
        val panXNorm: Float = 0f,
        val panYNorm: Float = 0f,
        val aspectRatio: CropAspectRatio = CropAspectRatio.ORIGINAL,
        val showRuleOfThirds: Boolean = true,
        val scaleMode: CropScaleMode = CropScaleMode.FIT
    ) : EditOperation

    @Parcelize
    data class ColorFilter(
        val id: String = "color_filter",
        val filterName: String,
        val intensity: Float = 1.0f
    ) : EditOperation

    @Parcelize
    data class BrushDraw(
        val id: String = UUID.randomUUID().toString(),
        val points: List<Pair<Float, Float>>,
        val colorInt: Int = Color.RED,
        val strokeWidth: Float = 14f,
        val opacity: Float = 1.0f,
        val isEraser: Boolean = false
    ) : EditOperation

    @Parcelize
    data class TextOverlay(
        val id: String = UUID.randomUUID().toString(),
        val text: String,
        val posX: Float,
        val posY: Float,
        val textSize: Float = 40f,
        val colorInt: Int = Color.WHITE,
        val alignment: Int = 0,
        val opacity: Float = 1.0f,
        val fontFamilyName: String = "SANS_SERIF"
    ) : EditOperation

    @Parcelize
    data class CloneStampPoint(
        val id: String = UUID.randomUUID().toString(),
        val sourceX: Float,
        val sourceY: Float,
        val targetX: Float,
        val targetY: Float,
        val radius: Float = 28f,
        val hardness: Float = 0.5f,
        val opacity: Float = 1.0f,
        val flow: Float = 1.0f
    ) : EditOperation

    @Parcelize
    data class PointOffset(
        val dx: Float,
        val dy: Float
    ) : Parcelable

    @Parcelize
    data class HealingPoint(
        val targetX: Float,
        val targetY: Float
    ) : Parcelable

    @Parcelize
    data class HealingStroke(
        val id: String = UUID.randomUUID().toString(),
        val points: List<HealingPoint>,
        val radius: Float = 28f,
        val feather: Float = 0.5f,
        val strength: Float = 1.0f,
        val manualSourceOffset: PointOffset? = null // null: Muestreo automático vecindario local, no nulo: Muestreo manual
    ) : EditOperation

    @Parcelize
    data class PatchOperation(
        val id: String = UUID.randomUUID().toString(),
        val targetCenterXNorm: Float,
        val targetCenterYNorm: Float,
        val sourceCenterXNorm: Float,
        val sourceCenterYNorm: Float,
        val radiusNorm: Float = 0.08f,
        val feather: Float = 0.5f,
        val strength: Float = 1.0f,
        val boundaryPoints: List<Pair<Float, Float>> = emptyList() // Opcional si fue selección libre
    ) : EditOperation

    @Parcelize
    data class PortraitLight(
        val id: String = UUID.randomUUID().toString(),
        val centerXNorm: Float = 0.5f,
        val centerYNorm: Float = 0.4f,
        val radiusXNorm: Float = 0.25f,
        val radiusYNorm: Float = 0.32f,
        val rotationDegrees: Float = 0f,
        val isBrushMode: Boolean = false,
        val brushPoints: List<Pair<Float, Float>> = emptyList(),
        val exposure: Float = 0.35f,
        val shadows: Float = 0.2f,
        val highlights: Float = -0.1f,
        val temperature: Float = 0.1f,
        val feather: Float = 0.65f,
        val opacity: Float = 0.85f,
        val isInverted: Boolean = false,
        val isEnabled: Boolean = true
    ) : EditOperation

    enum class FacialZoneType {
        ALL,
        FOREHEAD,
        LEFT_CHEEK,
        RIGHT_CHEEK,
        NOSE,
        CHIN,
        JAWLINE
    }

    @Parcelize
    data class FacialRelightZone(
        val zoneType: FacialZoneType,
        val centerXNorm: Float,
        val centerYNorm: Float,
        val radiusXNorm: Float,
        val radiusYNorm: Float,
        val exposure: Float = 0.2f,
        val temperature: Float = 0.05f,
        val shadows: Float = 0.15f,
        val contrast: Float = 1.05f,
        val smoothness: Float = 0.1f,
        val intensity: Float = 0.8f,
        val feather: Float = 0.6f,
        val opacity: Float = 1.0f,
        val isEnabled: Boolean = true,
        val customPoints: List<Pair<Float, Float>> = emptyList()
    ) : Parcelable

    @Parcelize
    data class FacialRelight(
        val id: String = UUID.randomUUID().toString(),
        val zones: List<FacialRelightZone> = emptyList(),
        val globalSmoothness: Float = 0.2f,
        val globalIntensity: Float = 1.0f,
        val globalOpacity: Float = 1.0f
    ) : EditOperation
}
