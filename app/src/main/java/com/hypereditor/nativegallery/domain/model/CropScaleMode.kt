package com.hypereditor.nativegallery.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class CropScaleMode(val displayName: String) : Parcelable {
    FIT("Ajustar (Fit)"),
    FILL("Llenar (Fill)"),
    CENTER("Centrar (Center)")
}
