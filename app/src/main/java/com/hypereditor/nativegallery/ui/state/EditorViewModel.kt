package com.hypereditor.nativegallery.ui.state

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hypereditor.nativegallery.data.ExportManager
import com.hypereditor.nativegallery.data.IntentImageLoader
import com.hypereditor.nativegallery.domain.history.HistoryManager
import com.hypereditor.nativegallery.domain.model.*
import com.hypereditor.nativegallery.render.RenderPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val renderPipeline = RenderPipeline(Dispatchers.Default)
    private val exportManager = ExportManager(application, renderPipeline)
    private val historyManager = HistoryManager(maxStackSize = 50)

    private var renderJob: Job? = null

    private val _uiState = MutableStateFlow(
        EditorUiState(
            userPresets = listOf(
                UserPreset(
                    name = "Golden Hour",
                    adjustments = EditOperation.Adjustments(
                        brightness = 0.05f,
                        contrast = 1.1f,
                        saturation = 1.25f,
                        temperature = 0.35f,
                        tint = 0.05f
                    )
                ),
                UserPreset(
                    name = "Cinematic Moody",
                    adjustments = EditOperation.Adjustments(
                        brightness = -0.05f,
                        contrast = 1.25f,
                        saturation = 0.85f,
                        exposure = -0.1f
                    ),
                    appliedFilter = EditOperation.ColorFilter(filterName = "CINE", intensity = 0.8f)
                ),
                UserPreset(
                    name = "Editorial B&W",
                    adjustments = EditOperation.Adjustments(
                        contrast = 1.35f,
                        exposure = 0.1f
                    ),
                    appliedFilter = EditOperation.ColorFilter(filterName = "NOIR", intensity = 1.0f)
                )
            )
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.InitializeWithUri -> loadInitialImage(intent.uri)
            is EditorIntent.SelectTab -> _uiState.update { it.copy(selectedTab = intent.tab) }

            // Color Adjustments
            is EditorIntent.UpdateAdjustments -> mutateDocument { it.copy(adjustments = intent.adjustments) }

            // Preset Filters
            is EditorIntent.ApplyFilter -> mutateDocument { doc ->
                doc.copy(appliedFilter = EditOperation.ColorFilter(filterName = intent.name, intensity = intent.intensity))
            }
            is EditorIntent.UpdateFilterIntensity -> mutateDocument { doc ->
                val currentFilter = doc.appliedFilter ?: EditOperation.ColorFilter(filterName = "BW", intensity = intent.intensity)
                doc.copy(appliedFilter = currentFilter.copy(intensity = intent.intensity))
            }
            is EditorIntent.ClearFilter -> mutateDocument { doc ->
                doc.copy(appliedFilter = null)
            }

            // Custom User Presets
            is EditorIntent.SaveUserPreset -> saveCurrentAsPreset(intent.name)
            is EditorIntent.ApplyUserPreset -> applyPreset(intent.preset)
            is EditorIntent.DeleteUserPreset -> {
                _uiState.update { state ->
                    state.copy(userPresets = state.userPresets.filterNot { it.id == intent.presetId })
                }
            }

            // Creative Tools (Brush, Eraser, Text, Clone Stamp)
            is EditorIntent.AddBrushStroke -> mutateDocument { doc ->
                doc.copy(brushStrokes = doc.brushStrokes + intent.stroke)
            }
            is EditorIntent.ClearBrushStrokes -> mutateDocument { doc ->
                doc.copy(brushStrokes = emptyList())
            }
            is EditorIntent.AddTextOverlay -> mutateDocument { doc ->
                val newText = EditOperation.TextOverlay(
                    text = intent.text,
                    posX = intent.posX,
                    posY = intent.posY,
                    textSize = intent.textSize,
                    colorInt = intent.colorInt,
                    alignment = intent.alignment,
                    opacity = intent.opacity,
                    fontFamilyName = intent.fontFamilyName
                )
                doc.copy(textOverlays = doc.textOverlays + newText)
            }
            is EditorIntent.UpdateTextOverlay -> mutateDocument { doc ->
                doc.copy(textOverlays = doc.textOverlays.map {
                    if (it.id == intent.textItem.id) intent.textItem else it
                })
            }
            is EditorIntent.DeleteTextOverlay -> mutateDocument { doc ->
                doc.copy(textOverlays = doc.textOverlays.filterNot { it.id == intent.textId })
            }
            is EditorIntent.AddCloneStamp -> mutateDocument { doc ->
                val newStamp = EditOperation.CloneStampPoint(
                    sourceX = intent.sourceX,
                    sourceY = intent.sourceY,
                    targetX = intent.targetX,
                    targetY = intent.targetY,
                    radius = intent.radius
                )
                doc.copy(cloneStamps = doc.cloneStamps + newStamp)
            }
            is EditorIntent.ClearCloneStamps -> mutateDocument { doc ->
                doc.copy(cloneStamps = emptyList())
            }

            // Masks & Selections
            is EditorIntent.AddMask -> addMask(intent.selectionType)
            is EditorIntent.ToggleMaskEnabled -> toggleMaskEnabled(intent.maskId)
            is EditorIntent.ToggleMaskInvert -> toggleMaskInvert(intent.maskId)
            is EditorIntent.UpdateMaskFeather -> updateMaskFeather(intent.maskId, intent.feather)
            is EditorIntent.UpdateMaskSelectionType -> updateMaskSelectionType(intent.maskId, intent.type)
            is EditorIntent.UpdateMaskRectBounds -> updateMaskRectBounds(intent.maskId, intent.bounds)
            is EditorIntent.UpdateMaskEllipseBounds -> updateMaskEllipseBounds(intent.maskId, intent.bounds)
            is EditorIntent.AddMaskBrushStroke -> addMaskBrushStroke(intent.maskId, intent.stroke)
            is EditorIntent.ClearMaskBrushStrokes -> clearMaskBrushStrokes(intent.maskId)
            is EditorIntent.UpdateMaskLocalAdjustments -> updateMaskLocalAdjustments(intent.maskId, intent.adjustments)
            is EditorIntent.DeleteMask -> deleteMask(intent.maskId)
            is EditorIntent.SelectActiveMask -> _uiState.update { it.copy(activeMaskId = intent.maskId) }

            // Layers
            is EditorIntent.AddColorLayer -> addColorLayer(intent.name, intent.colorHex, intent.blendMode, intent.opacity)
            is EditorIntent.AddDuplicateImageLayer -> addDuplicateImageLayer(intent.name, intent.blendMode, intent.opacity)
            is EditorIntent.AddTextLayer -> addTextLayer(intent.text, intent.textSize, intent.textColor, intent.blendMode, intent.opacity)
            is EditorIntent.AddStickerLayer -> addStickerLayer(intent.emoji, intent.blendMode, intent.opacity)
            is EditorIntent.UpdateLayerTransform -> updateLayerTransform(intent.layerId, intent.offsetX, intent.offsetY, intent.scale, intent.rotation)
            is EditorIntent.ToggleLayerVisibility -> toggleLayerVisibility(intent.layerId)
            is EditorIntent.UpdateLayerOpacity -> updateLayerOpacity(intent.layerId, intent.opacity)
            is EditorIntent.UpdateLayerBlendMode -> updateLayerBlendMode(intent.layerId, intent.blendMode)
            is EditorIntent.DuplicateLayer -> duplicateLayer(intent.layerId)
            is EditorIntent.MoveLayerUp -> moveLayer(intent.layerId, moveUp = true)
            is EditorIntent.MoveLayerDown -> moveLayer(intent.layerId, moveUp = false)
            is EditorIntent.DeleteLayer -> deleteLayer(intent.layerId)
            is EditorIntent.SelectActiveLayer -> _uiState.update { it.copy(activeLayerId = intent.layerId) }

            // Geometry & Crop
            is EditorIntent.UpdateCropTransform -> mutateDocument { it.copy(cropTransform = intent.cropTransform) }
            is EditorIntent.SetCropAspectRatio -> mutateDocument { doc ->
                doc.copy(cropTransform = doc.cropTransform.copy(aspectRatio = intent.aspectRatio, scale = 1.0f, panXNorm = 0f, panYNorm = 0f))
            }
            is EditorIntent.SetCropScaleMode -> mutateDocument { doc ->
                val newScale = when (intent.scaleMode) {
                    CropScaleMode.FIT -> 1.0f
                    CropScaleMode.FILL -> 1.25f
                    CropScaleMode.CENTER -> 1.0f
                }
                doc.copy(cropTransform = doc.cropTransform.copy(scaleMode = intent.scaleMode, scale = newScale, panXNorm = 0f, panYNorm = 0f))
            }
            is EditorIntent.ToggleCropRuleOfThirds -> mutateDocument { doc ->
                doc.copy(cropTransform = doc.cropTransform.copy(showRuleOfThirds = intent.show))
            }
            is EditorIntent.Rotate90Clockwise -> mutateDocument { doc ->
                val nextRot = (doc.cropTransform.rotation90Degrees + 90) % 360
                doc.copy(cropTransform = doc.cropTransform.copy(rotation90Degrees = nextRot))
            }
            is EditorIntent.Rotate90CounterClockwise -> mutateDocument { doc ->
                val nextRot = (doc.cropTransform.rotation90Degrees - 90 + 360) % 360
                doc.copy(cropTransform = doc.cropTransform.copy(rotation90Degrees = nextRot))
            }
            is EditorIntent.ToggleFlipHorizontal -> mutateDocument { doc ->
                doc.copy(cropTransform = doc.cropTransform.copy(flipHorizontal = !doc.cropTransform.flipHorizontal))
            }
            is EditorIntent.ToggleFlipVertical -> mutateDocument { doc ->
                doc.copy(cropTransform = doc.cropTransform.copy(flipVertical = !doc.cropTransform.flipVertical))
            }
            is EditorIntent.UpdateStraightenAngle -> mutateDocument { doc ->
                doc.copy(cropTransform = doc.cropTransform.copy(fineStraightenAngle = intent.angle))
            }
            is EditorIntent.ApplyCropNorm -> mutateDocument { doc ->
                doc.copy(
                    cropTransform = doc.cropTransform.copy(
                        cropLeftNorm = intent.left.coerceIn(0f, 0.9f),
                        cropTopNorm = intent.top.coerceIn(0f, 0.9f),
                        cropRightNorm = intent.right.coerceIn(0.1f, 1f),
                        cropBottomNorm = intent.bottom.coerceIn(0.1f, 1f)
                    )
                )
            }
            is EditorIntent.ApplyAspectRatioCrop -> applyAspectRatio(intent.ratioW, intent.ratioH)
            is EditorIntent.ResetCrop -> mutateDocument { doc ->
                doc.copy(
                    cropTransform = doc.cropTransform.copy(
                        scale = 1.0f,
                        panXNorm = 0f,
                        panYNorm = 0f,
                        cropLeftNorm = 0f,
                        cropTopNorm = 0f,
                        cropRightNorm = 1f,
                        cropBottomNorm = 1f,
                        aspectRatio = CropAspectRatio.ORIGINAL
                    )
                )
            }
            is EditorIntent.ResetGeometry -> mutateDocument { doc ->
                doc.copy(cropTransform = EditOperation.CropTransform())
            }

            // Global Undo / Redo & Export
            is EditorIntent.SetCompareOriginalMode -> _uiState.update { it.copy(isComparingOriginal = intent.isComparing) }
            is EditorIntent.Undo -> performUndo()
            is EditorIntent.Redo -> performRedo()
            is EditorIntent.SaveAndExport -> performExport(intent.format, intent.quality)
        }
    }

    private fun loadInitialImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = IntentImageLoader.loadBitmapFromUri(getApplication(), uri)
            result.onSuccess { bitmap ->
                val initialDoc = EditorDocument(
                    sourceUri = uri,
                    originalWidth = bitmap.width,
                    originalHeight = bitmap.height
                )
                historyManager.clear()
                val preview = renderPipeline.renderPreview(bitmap, initialDoc)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        originalBitmap = bitmap,
                        previewBitmap = preview,
                        document = initialDoc,
                        canUndo = false,
                        canRedo = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Fallo al cargar imagen"
                    )
                }
            }
        }
    }

    // Mask Handlers
    private fun addMask(type: SelectionToolType) {
        val count = (_uiState.value.document?.masks?.size ?: 0) + 1
        val newMask = MaskModel(
            name = "Máscara $count (${type.name.lowercase().replaceFirstChar { it.uppercase() }})",
            selectionType = type,
            brushStrokes = if (type == SelectionToolType.BRUSH) {
                listOf(
                    MaskBrushStroke(
                        points = listOf(Pair(0.3f, 0.5f), Pair(0.7f, 0.5f)),
                        strokeWidthNorm = 0.15f
                    )
                )
            } else emptyList()
        )
        mutateDocument { doc ->
            doc.copy(masks = doc.masks + newMask)
        }
        _uiState.update { it.copy(activeMaskId = newMask.id) }
    }

    private fun toggleMaskEnabled(maskId: String) {
        mutateDocument { doc ->
            doc.copy(masks = doc.masks.map {
                if (it.id == maskId) it.copy(isEnabled = !it.isEnabled) else it
            })
        }
    }

    private fun toggleMaskInvert(maskId: String) {
        mutateDocument { doc ->
            doc.copy(masks = doc.masks.map {
                if (it.id == maskId) it.copy(isInverted = !it.isInverted) else it
            })
        }
    }

    private fun updateMaskFeather(maskId: String, feather: Float) {
        mutateDocument { doc ->
            doc.copy(masks = doc.masks.map {
                if (it.id == maskId) it.copy(featherRadius = feather) else it
            })
        }
    }

    private fun updateMaskSelectionType(maskId: String, type: SelectionToolType) {
        mutateDocument { doc ->
            doc.copy(masks = doc.masks.map {
                if (it.id == maskId) it.copy(selectionType = type) else it
            })
        }
    }

    private fun updateMaskRectBounds(maskId: String, bounds: RectNorm) {
        mutateDocument { doc ->
            doc.copy(masks = doc.masks.map {
                if (it.id == maskId) it.copy(rectBounds = bounds) else it
            })
        }
    }

    private fun updateMaskEllipseBounds(maskId: String, bounds: RectNorm) {
        mutateDocument { doc ->
            doc.copy(masks = doc.masks.map {
                if (it.id == maskId) it.copy(ellipseBounds = bounds) else it
            })
        }
    }

    private fun addMaskBrushStroke(maskId: String, stroke: MaskBrushStroke) {
        mutateDocument { doc ->
            doc.copy(masks = doc.masks.map {
                if (it.id == maskId) it.copy(brushStrokes = it.brushStrokes + stroke) else it
            })
        }
    }

    private fun clearMaskBrushStrokes(maskId: String) {
        mutateDocument { doc ->
            doc.copy(masks = doc.masks.map {
                if (it.id == maskId) it.copy(brushStrokes = emptyList()) else it
            })
        }
    }

    private fun updateMaskLocalAdjustments(maskId: String, adjustments: EditOperation.Adjustments) {
        mutateDocument { doc ->
            doc.copy(masks = doc.masks.map {
                if (it.id == maskId) it.copy(localAdjustments = adjustments) else it
            })
        }
    }

    private fun deleteMask(maskId: String) {
        mutateDocument { doc ->
            doc.copy(masks = doc.masks.filterNot { it.id == maskId })
        }
        if (_uiState.value.activeMaskId == maskId) {
            _uiState.update { it.copy(activeMaskId = null) }
        }
    }

    // Layer Handlers
    private fun addColorLayer(name: String, colorHex: Long, blendMode: LayerBlendMode, opacity: Float) {
        val newLayer = LayerModel(
            name = name,
            layerType = LayerType.COLOR_FILL,
            colorHex = colorHex,
            blendMode = blendMode,
            opacity = opacity
        )
        mutateDocument { doc ->
            doc.copy(layers = doc.layers + newLayer)
        }
        _uiState.update { it.copy(activeLayerId = newLayer.id) }
    }

    private fun addDuplicateImageLayer(name: String, blendMode: LayerBlendMode, opacity: Float) {
        val baseBmp = _uiState.value.originalBitmap
        val newLayer = LayerModel(
            name = name,
            layerType = LayerType.IMAGE_DUPLICATE,
            bitmap = baseBmp,
            blendMode = blendMode,
            opacity = opacity
        )
        mutateDocument { doc ->
            doc.copy(layers = doc.layers + newLayer)
        }
        _uiState.update { it.copy(activeLayerId = newLayer.id) }
    }

    private fun addTextLayer(text: String, textSize: Float, textColor: Long, blendMode: LayerBlendMode, opacity: Float) {
        val count = (_uiState.value.document?.layers?.size ?: 0) + 1
        val newLayer = LayerModel(
            name = "Texto $count",
            layerType = LayerType.TEXT,
            text = text,
            textSize = textSize,
            textColor = textColor,
            blendMode = blendMode,
            opacity = opacity
        )
        mutateDocument { doc ->
            doc.copy(layers = doc.layers + newLayer)
        }
        _uiState.update { it.copy(activeLayerId = newLayer.id) }
    }

    private fun addStickerLayer(emoji: String, blendMode: LayerBlendMode, opacity: Float) {
        val count = (_uiState.value.document?.layers?.size ?: 0) + 1
        val newLayer = LayerModel(
            name = "Sticker $count $emoji",
            layerType = LayerType.STICKER,
            stickerEmoji = emoji,
            blendMode = blendMode,
            opacity = opacity
        )
        mutateDocument { doc ->
            doc.copy(layers = doc.layers + newLayer)
        }
        _uiState.update { it.copy(activeLayerId = newLayer.id) }
    }

    private fun updateLayerTransform(layerId: String, offsetX: Float, offsetY: Float, scale: Float, rotation: Float) {
        mutateDocument { doc ->
            doc.copy(layers = doc.layers.map {
                if (it.id == layerId) {
                    it.copy(offsetX = offsetX, offsetY = offsetY, scale = scale, rotationDegrees = rotation)
                } else it
            })
        }
    }

    private fun toggleLayerVisibility(layerId: String) {
        mutateDocument { doc ->
            doc.copy(layers = doc.layers.map {
                if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
            })
        }
    }

    private fun updateLayerOpacity(layerId: String, opacity: Float) {
        mutateDocument { doc ->
            doc.copy(layers = doc.layers.map {
                if (it.id == layerId) it.copy(opacity = opacity) else it
            })
        }
    }

    private fun updateLayerBlendMode(layerId: String, blendMode: LayerBlendMode) {
        mutateDocument { doc ->
            doc.copy(layers = doc.layers.map {
                if (it.id == layerId) it.copy(blendMode = blendMode) else it
            })
        }
    }

    private fun moveLayer(layerId: String, moveUp: Boolean) {
        val doc = _uiState.value.document ?: return
        val list = doc.layers.toMutableList()
        val index = list.indexOfFirst { it.id == layerId }
        if (index == -1) return

        val targetIndex = if (moveUp) index + 1 else index - 1
        if (targetIndex in list.indices) {
            Collections.swap(list, index, targetIndex)
            mutateDocument { it.copy(layers = list) }
        }
    }

    private fun duplicateLayer(layerId: String) {
        val doc = _uiState.value.document ?: return
        val targetLayer = doc.layers.find { it.id == layerId } ?: return
        val duplicated = targetLayer.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${targetLayer.name} (Copia)",
            offsetX = targetLayer.offsetX + 24f,
            offsetY = targetLayer.offsetY + 24f
        )
        mutateDocument { it.copy(layers = it.layers + duplicated) }
        _uiState.update { it.copy(activeLayerId = duplicated.id) }
    }

    private fun deleteLayer(layerId: String) {
        mutateDocument { doc ->
            doc.copy(layers = doc.layers.filterNot { it.id == layerId })
        }
        if (_uiState.value.activeLayerId == layerId) {
            _uiState.update { it.copy(activeLayerId = null) }
        }
    }

    private fun saveCurrentAsPreset(name: String) {
        val doc = _uiState.value.document ?: return
        val trimmed = name.trim().ifEmpty { "Preset ${_uiState.value.userPresets.size + 1}" }
        val newPreset = UserPreset(
            name = trimmed,
            adjustments = doc.adjustments,
            appliedFilter = doc.appliedFilter
        )
        _uiState.update { state ->
            state.copy(userPresets = listOf(newPreset) + state.userPresets)
        }
    }

    private fun applyPreset(preset: UserPreset) {
        mutateDocument { doc ->
            doc.copy(
                adjustments = preset.adjustments,
                appliedFilter = preset.appliedFilter
            )
        }
    }

    private fun applyAspectRatio(ratioW: Float, ratioH: Float) {
        val baseBmp = _uiState.value.originalBitmap ?: return
        val imgAspect = baseBmp.width.toFloat() / baseBmp.height.toFloat()
        val targetAspect = ratioW / ratioH

        var cropL = 0f
        var cropT = 0f
        var cropR = 1f
        var cropB = 1f

        if (targetAspect > imgAspect) {
            val newHeightNorm = imgAspect / targetAspect
            val diff = (1f - newHeightNorm) / 2f
            cropT = diff
            cropB = 1f - diff
        } else {
            val newWidthNorm = targetAspect / imgAspect
            val diff = (1f - newWidthNorm) / 2f
            cropL = diff
            cropR = 1f - diff
        }

        mutateDocument { doc ->
            doc.copy(
                cropTransform = doc.cropTransform.copy(
                    cropLeftNorm = cropL,
                    cropTopNorm = cropT,
                    cropRightNorm = cropR,
                    cropBottomNorm = cropB
                )
            )
        }
    }

    private fun mutateDocument(transform: (EditorDocument) -> EditorDocument) {
        val currentDoc = _uiState.value.document ?: return
        val baseBmp = _uiState.value.originalBitmap ?: return

        val updatedDoc = transform(currentDoc)
        if (updatedDoc == currentDoc) return

        historyManager.pushState(currentDoc)

        _uiState.update {
            it.copy(
                document = updatedDoc,
                canUndo = historyManager.canUndo,
                canRedo = historyManager.canRedo
            )
        }

        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val updatedPreview = renderPipeline.renderPreview(baseBmp, updatedDoc)
            _uiState.update {
                it.copy(previewBitmap = updatedPreview)
            }
        }
    }

    private fun performUndo() {
        val currentDoc = _uiState.value.document ?: return
        val prevDoc = historyManager.undo(currentDoc) ?: return
        val baseBmp = _uiState.value.originalBitmap ?: return

        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val preview = renderPipeline.renderPreview(baseBmp, prevDoc)
            _uiState.update {
                it.copy(
                    document = prevDoc,
                    previewBitmap = preview,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo
                )
            }
        }
    }

    private fun performRedo() {
        val currentDoc = _uiState.value.document ?: return
        val nextDoc = historyManager.redo(currentDoc) ?: return
        val baseBmp = _uiState.value.originalBitmap ?: return

        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val preview = renderPipeline.renderPreview(baseBmp, nextDoc)
            _uiState.update {
                it.copy(
                    document = nextDoc,
                    previewBitmap = preview,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo
                )
            }
        }
    }

    private fun performExport(
        format: android.graphics.Bitmap.CompressFormat = android.graphics.Bitmap.CompressFormat.JPEG,
        quality: Int = 95
    ) {
        val baseBmp = _uiState.value.originalBitmap ?: return
        val doc = _uiState.value.document ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val saveResult = exportManager.renderAndExport(baseBmp, doc, format, quality)
            saveResult.onSuccess { finalUri ->
                _uiState.update { it.copy(isExporting = false, exportedUri = finalUri) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isExporting = false, errorMessage = "Error al exportar: ${err.localizedMessage}")
                }
            }
        }
    }
}
