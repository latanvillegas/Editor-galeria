package com.hypereditor.nativegallery.ui.state

import android.graphics.Bitmap
import android.net.Uri
import com.hypereditor.nativegallery.domain.model.EditorDocument
import com.hypereditor.nativegallery.domain.model.UserPreset

data class EditorUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val originalBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val document: EditorDocument? = null,
    val activeLayerId: String? = null,
    val activeMaskId: String? = null,
    val userPresets: List<UserPreset> = emptyList(),
    val selectedTab: EditorSectionTab = EditorSectionTab.ADJUSTMENTS,
    val isComparingOriginal: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isExporting: Boolean = false,
    val exportedUri: Uri? = null
)
