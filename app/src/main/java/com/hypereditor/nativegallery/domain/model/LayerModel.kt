package com.hypereditor.nativegallery.domain.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.UUID

enum class LayerBlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN
}

@Parcelize
data class LayerModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    @RawValue val bitmap: Bitmap? = null
) : Parcelable
