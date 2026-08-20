package com.hypereditor.nativegallery.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class CropAspectRatio(val displayName: String, val ratioW: Float, val ratioH: Float) : Parcelable {
    ORIGINAL("Original", 0f, 0f),
    FREE("Libre", -1f, -1f),
    RATIO_1_1("1:1 Cuadrado", 1f, 1f),
    RATIO_4_5("4:5 Retrato", 4f, 5f),
    RATIO_3_4("3:4 Retrato", 3f, 4f),
    RATIO_5_4("5:4 Clásico", 5f, 4f),
    RATIO_2_3("2:3 Fotografía", 2f, 3f),
    RATIO_16_9("16:9 Panorámico", 16f, 9f),
    RATIO_9_16("9:16 Historia / Reels", 9f, 16f),
    RATIO_4_3("4:3 Estándar", 4f, 3f),
    RATIO_3_2("3:2 Clásico", 3f, 2f);

    fun getCalculatedRatio(originalWidth: Int, originalHeight: Int): Float {
        return if (ratioW > 0f && ratioH > 0f) {
            ratioW / ratioH
        } else if (originalHeight > 0) {
            originalWidth.toFloat() / originalHeight.toFloat()
        } else {
            1.0f
        }
    }
}
