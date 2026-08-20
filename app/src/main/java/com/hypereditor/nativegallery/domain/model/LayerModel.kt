package com.hypereditor.nativegallery.domain.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

enum class LayerBlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN
}

enum class LayerType {
    IMAGE_DUPLICATE,
    COLOR_FILL
}

@Parcelize
data class LayerModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    val layerType: LayerType = LayerType.COLOR_FILL,
    val colorHex: Long = 0xFFFFA000, // Default warm amber tint
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val bitmap: Bitmap? = null
) : Parcelable
