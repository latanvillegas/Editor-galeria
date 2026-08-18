package com.hypereditor.nativegallery.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

enum class SelectionToolType {
    RECTANGLE,
    ELLIPSE,
    LASSO,
    BRUSH
}

@Parcelize
data class RectNorm(
    val left: Float = 0.2f,
    val top: Float = 0.2f,
    val right: Float = 0.8f,
    val bottom: Float = 0.8f
) : Parcelable

@Parcelize
data class MaskBrushStroke(
    val points: List<Pair<Float, Float>>,
    val strokeWidthNorm: Float = 0.05f,
    val isEraser: Boolean = false
) : Parcelable

@Parcelize
data class MaskModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Máscara 1",
    val selectionType: SelectionToolType = SelectionToolType.RECTANGLE,
    val rectBounds: RectNorm = RectNorm(0.2f, 0.2f, 0.8f, 0.8f),
    val ellipseBounds: RectNorm = RectNorm(0.25f, 0.25f, 0.75f, 0.75f),
    val lassoPoints: List<Pair<Float, Float>> = listOf(
        Pair(0.2f, 0.2f),
        Pair(0.8f, 0.2f),
        Pair(0.7f, 0.8f),
        Pair(0.3f, 0.8f)
    ),
    val brushStrokes: List<MaskBrushStroke> = emptyList(),
    val isInverted: Boolean = false,
    val featherRadius: Float = 15f, // Suavizado de bordes (Feather) en px
    val isEnabled: Boolean = true,
    val localAdjustments: EditOperation.Adjustments = EditOperation.Adjustments(
        brightness = 0.2f,
        contrast = 1.2f,
        saturation = 1.3f
    )
) : Parcelable
