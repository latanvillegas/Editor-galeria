package com.hypereditor.nativegallery.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class UserPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val adjustments: EditOperation.Adjustments,
    val appliedFilter: EditOperation.ColorFilter? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
