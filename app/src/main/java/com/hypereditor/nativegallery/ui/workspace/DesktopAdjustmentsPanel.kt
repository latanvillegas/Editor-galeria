package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState
import kotlin.math.roundToInt

@Composable
fun DesktopAdjustmentsPanel(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val adjustments = state.document?.adjustments ?: return
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text("LUZ", color = Color(0xFF929292), fontSize = 9.sp)
        DesktopAdjustmentSlider("Exposición", adjustments.exposure, -2f..2f, { adjustments.copy(exposure = it) }, onIntent)
        DesktopAdjustmentSlider("Brillo", adjustments.brightness, -1f..1f, { adjustments.copy(brightness = it) }, onIntent)
        DesktopAdjustmentSlider("Contraste", adjustments.contrast, 0f..2f, { adjustments.copy(contrast = it) }, onIntent)
        DesktopAdjustmentSlider("Sombras", adjustments.shadows, -1f..1f, { adjustments.copy(shadows = it) }, onIntent)
        DesktopAdjustmentSlider("Iluminaciones", adjustments.highlights, -1f..1f, { adjustments.copy(highlights = it) }, onIntent)

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF353535))
        Spacer(Modifier.height(12.dp))
        Text("COLOR", color = Color(0xFF929292), fontSize = 9.sp)
        DesktopAdjustmentSlider("Saturación", adjustments.saturation, 0f..2f, { adjustments.copy(saturation = it) }, onIntent)
        DesktopAdjustmentSlider("Temperatura", adjustments.temperature, -1f..1f, { adjustments.copy(temperature = it) }, onIntent)
        DesktopAdjustmentSlider("Matiz", adjustments.tint, -1f..1f, { adjustments.copy(tint = it) }, onIntent)

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF353535))
        Spacer(Modifier.height(12.dp))
        Text("DETALLE Y EFECTOS", color = Color(0xFF929292), fontSize = 9.sp)
        DesktopAdjustmentSlider("Estructura", adjustments.structure, -1f..1f, { adjustments.copy(structure = it) }, onIntent)
        DesktopAdjustmentSlider("Nitidez", adjustments.sharpness, 0f..1f, { adjustments.copy(sharpness = it) }, onIntent)
        DesktopAdjustmentSlider("Viñeta", adjustments.vignette, 0f..1f, { adjustments.copy(vignette = it) }, onIntent)
        DesktopAdjustmentSlider("Grano", adjustments.grain, 0f..1f, { adjustments.copy(grain = it) }, onIntent)
    }
}

@Composable
private fun DesktopAdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    update: (Float) -> EditOperation.Adjustments,
    onIntent: (EditorIntent) -> Unit
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(top = 7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0xFFD5D5D5), fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text(formatAdjustmentValue(localValue), color = Color(0xFFAFAFAF), fontSize = 9.sp)
        }
        Slider(
            value = localValue,
            onValueChange = {
                localValue = it
                onIntent(EditorIntent.UpdateAdjustments(update(it), isFinished = false, actionLabel = label))
            },
            onValueChangeFinished = {
                onIntent(EditorIntent.UpdateAdjustments(update(localValue), isFinished = true, actionLabel = label))
            },
            valueRange = range,
            modifier = Modifier.fillMaxWidth().height(30.dp)
        )
    }
}

private fun formatAdjustmentValue(value: Float): String =
    if (value == value.roundToInt().toFloat()) value.roundToInt().toString() else String.format("%.2f", value)
