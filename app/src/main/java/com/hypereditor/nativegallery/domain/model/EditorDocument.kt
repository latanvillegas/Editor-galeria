package com.hypereditor.nativegallery.domain.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class EditorDocument(
    val sourceUri: Uri,
    val originalWidth: Int,
    val originalHeight: Int,
    val adjustments: EditOperation.Adjustments = EditOperation.Adjustments(),
    val cropTransform: EditOperation.CropTransform = EditOperation.CropTransform(),
    val appliedFilter: EditOperation.ColorFilter? = null,
    val layers: List<LayerModel> = emptyList(),
    val masks: List<MaskModel> = emptyList(),
    val brushStrokes: List<EditOperation.BrushDraw> = emptyList(),
    val textOverlays: List<EditOperation.TextOverlay> = emptyList(),
    val cloneStamps: List<EditOperation.CloneStampPoint> = emptyList()
) : Parcelable
