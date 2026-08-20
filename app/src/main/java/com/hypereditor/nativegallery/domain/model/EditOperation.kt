package com.hypereditor.nativegallery.domain.model

import android.graphics.Color
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

sealed interface EditOperation : Parcelable {

    @Parcelize
    data class Adjustments(
        val id: String = UUID.randomUUID().toString(),
        val brightness: Float = 0f,
        val contrast: Float = 1f,
        val saturation: Float = 1f,
        val temperature: Float = 0f,
        val tint: Float = 0f,
        val exposure: Float = 0f,
        val shadows: Float = 0f,
        val highlights: Float = 0f,
        val sharpness: Float = 0f,
        val gamma: Float = 1f,
        val vignette: Float = 0f,
        val grain: Float = 0f
    ) : EditOperation

    @Parcelize
    data class CropTransform(
        val id: String = UUID.randomUUID().toString(),
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
        val scale: Float = 1.0f,
        val panXNorm: Float = 0f,
        val panYNorm: Float = 0f,
        val aspectRatio: CropAspectRatio = CropAspectRatio.ORIGINAL,
        val showRuleOfThirds: Boolean = true
    ) : EditOperation

    @Parcelize
    data class ColorFilter(
        val id: String = UUID.randomUUID().toString(),
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
        val radius: Float = 28f
    ) : EditOperation
}
