package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.CropAspectRatio
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

@Composable
fun DesktopGeometryPanel(state: EditorUiState, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val crop = state.document?.cropTransform ?: return
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("RECORTE", color = Color(0xFF929292), fontSize = 9.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GeometryButton("Original", Modifier.weight(1f)) { onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.ORIGINAL)) }
            GeometryButton("Libre", Modifier.weight(1f)) { onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.FREE)) }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GeometryButton("1:1", Modifier.weight(1f)) { onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.SQUARE)) }
            GeometryButton("4:3", Modifier.weight(1f)) { onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_4_3)) }
            GeometryButton("16:9", Modifier.weight(1f)) { onIntent(EditorIntent.SetCropAspectRatio(CropAspectRatio.RATIO_16_9)) }
        }
        Spacer(Modifier.height(14.dp)); HorizontalDivider(color = Color(0xFF353535)); Spacer(Modifier.height(12.dp))
        Text("TRANSFORMAR", color = Color(0xFF929292), fontSize = 9.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GeometryButton("↶ 90°", Modifier.weight(1f)) { onIntent(EditorIntent.Rotate90CounterClockwise) }
            GeometryButton("90° ↷", Modifier.weight(1f)) { onIntent(EditorIntent.Rotate90Clockwise) }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GeometryButton("Voltear H", Modifier.weight(1f)) { onIntent(EditorIntent.ToggleFlipHorizontal) }
            GeometryButton("Voltear V", Modifier.weight(1f)) { onIntent(EditorIntent.ToggleFlipVertical) }
        }
        Spacer(Modifier.height(14.dp)); HorizontalDivider(color = Color(0xFF353535)); Spacer(Modifier.height(12.dp))
        Text("ENDEREZAR", color = Color(0xFF929292), fontSize = 9.sp)
        Text("${String.format("%.1f", crop.fineStraightenAngle)}°", color = Color(0xFFBDBDBD), fontSize = 10.sp)
        Slider(
            value = crop.fineStraightenAngle,
            onValueChange = { onIntent(EditorIntent.UpdateStraightenAngle(it, isFinished = false)) },
            onValueChangeFinished = { onIntent(EditorIntent.UpdateStraightenAngle(crop.fineStraightenAngle, isFinished = true)) },
            valueRange = -45f..45f
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GeometryButton("Restablecer recorte", Modifier.weight(1f)) { onIntent(EditorIntent.ResetCrop) }
            GeometryButton("Todo", Modifier.weight(1f)) { onIntent(EditorIntent.ResetGeometry) }
        }
    }
}

@Composable
private fun GeometryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
        Text(text, fontSize = 9.sp, maxLines = 1)
    }
}
