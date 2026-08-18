package com.hypereditor.nativegallery.ui.state

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hypereditor.nativegallery.data.ExportManager
import com.hypereditor.nativegallery.data.IntentImageLoader
import com.hypereditor.nativegallery.domain.history.HistoryManager
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.EditorDocument
import com.hypereditor.nativegallery.domain.model.LayerBlendMode
import com.hypereditor.nativegallery.domain.model.LayerModel
import com.hypereditor.nativegallery.render.RenderPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val renderPipeline = RenderPipeline(Dispatchers.Default)
    private val exportManager = ExportManager(application, renderPipeline)
    private val historyManager = HistoryManager()

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.InitializeWithUri -> loadInitialImage(intent.uri)
            is EditorIntent.SelectTool -> _uiState.update { it.copy(activeToolType = intent.toolType) }
            is EditorIntent.UpdateAdjustments -> mutateDocument { it.copy(adjustments = intent.adjustments) }
            is EditorIntent.Rotate90Clockwise -> mutateDocument { doc ->
                val nextRot = (doc.cropTransform.rotation90Degrees + 90) % 360
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
            is EditorIntent.UpdatePerspective -> mutateDocument { doc ->
                doc.copy(cropTransform = doc.cropTransform.copy(perspectiveHorizontal = intent.h, perspectiveVertical = intent.v))
            }
            is EditorIntent.ApplyPresetFilter -> mutateDocument { doc ->
                if (intent.filterName == "NONE") doc.copy(appliedFilter = null)
                else doc.copy(appliedFilter = EditOperation.ColorFilter(filterName = intent.filterName, intensity = intent.intensity))
            }
            is EditorIntent.AddBrushStroke -> mutateDocument { it.copy(brushStrokes = it.brushStrokes + intent.stroke) }
            is EditorIntent.AddTextOverlay -> mutateDocument { it.copy(textOverlays = it.textOverlays + intent.text) }
            is EditorIntent.AddCloneStamp -> mutateDocument { it.copy(cloneStamps = it.cloneStamps + intent.stamp) }
            is EditorIntent.SetCompareOriginalMode -> _uiState.update { it.copy(isComparingOriginal = intent.isComparing) }
            is EditorIntent.AddLayer -> {
                val base = _uiState.value.originalBitmap ?: return
                val blank = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
                mutateDocument { it.copy(layers = it.layers + LayerModel(name = intent.name, bitmap = blank)) }
            }
            is EditorIntent.ToggleLayerVisibility -> mutateDocument { doc ->
                doc.copy(layers = doc.layers.map { if (it.id == intent.layerId) it.copy(isVisible = !it.isVisible) else it })
            }
            is EditorIntent.SetLayerBlendMode -> mutateDocument { doc ->
                doc.copy(layers = doc.layers.map { if (it.id == intent.layerId) it.copy(blendMode = intent.mode) else it })
            }
            is EditorIntent.Undo -> performUndo()
            is EditorIntent.Redo -> performRedo()
            is EditorIntent.SaveAndExport -> performExport()
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

    private fun mutateDocument(transform: (EditorDocument) -> EditorDocument) {
        val currentDoc = _uiState.value.document ?: return
        val baseBmp = _uiState.value.originalBitmap ?: return

        historyManager.pushState(currentDoc)
        val newDoc = transform(currentDoc)

        viewModelScope.launch {
            val updatedPreview = renderPipeline.renderPreview(baseBmp, newDoc)
            _uiState.update {
                it.copy(
                    document = newDoc,
                    previewBitmap = updatedPreview,
                    canUndo = historyManager.canUndo,
                    canRedo = historyManager.canRedo
                )
            }
        }
    }

    private fun performUndo() {
        val currentDoc = _uiState.value.document ?: return
        val prevDoc = historyManager.undo(currentDoc) ?: return
        val baseBmp = _uiState.value.originalBitmap ?: return

        viewModelScope.launch {
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

        viewModelScope.launch {
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

    private fun performExport() {
        val baseBmp = _uiState.value.originalBitmap ?: return
        val doc = _uiState.value.document ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val saveResult = exportManager.renderAndExport(baseBmp, doc)
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
