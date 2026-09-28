package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.domain.model.EditOperation
import com.hypereditor.nativegallery.domain.model.SelectionMode
import com.hypereditor.nativegallery.domain.model.SelectionToolType
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

@Composable
fun DesktopMasksPanel(state: EditorUiState, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val masks = state.document?.masks.orEmpty()
    val active = masks.firstOrNull { it.id == state.activeMaskId } ?: masks.firstOrNull()
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("CREAR SELECCIÓN", color = Color(0xFF929292), fontSize = 9.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MaskButton("Rect", Modifier.weight(1f)) { onIntent(EditorIntent.AddMask(SelectionToolType.RECTANGLE)) }
            MaskButton("Óvalo", Modifier.weight(1f)) { onIntent(EditorIntent.AddMask(SelectionToolType.ELLIPSE)) }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MaskButton("Lazo", Modifier.weight(1f)) { onIntent(EditorIntent.AddMask(SelectionToolType.LASSO)) }
            MaskButton("Pincel", Modifier.weight(1f)) { onIntent(EditorIntent.AddMask(SelectionToolType.BRUSH)) }
        }
        Spacer(Modifier.height(12.dp)); HorizontalDivider(color = Color(0xFF353535)); Spacer(Modifier.height(10.dp))
        Text("MÁSCARAS", color = Color(0xFF929292), fontSize = 9.sp)
        if (masks.isEmpty()) {
            Text("No hay máscaras activas.", color = Color(0xFF777777), fontSize = 10.sp, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            masks.forEach { mask ->
                val selected = mask.id == active?.id
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp).background(if (selected) Color(0xFF3B4654) else Color(0xFF303030), RoundedCornerShape(4.dp)).clickable { onIntent(EditorIntent.SelectActiveMask(mask.id)) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(mask.name, color = Color(0xFFE0E0E0), fontSize = 10.sp)
                        Text(mask.selectionType.name.lowercase(), color = Color(0xFF8E8E8E), fontSize = 8.sp)
                    }
                    TextButton(onClick = { onIntent(EditorIntent.ToggleMaskEnabled(mask.id)) }, contentPadding = PaddingValues(horizontal = 5.dp)) { Text(if (mask.isEnabled) "On" else "Off", fontSize = 8.sp) }
                    TextButton(onClick = { onIntent(EditorIntent.DeleteMask(mask.id)) }, contentPadding = PaddingValues(horizontal = 5.dp)) { Text("Eliminar", fontSize = 8.sp) }
                }
            }
        }
        active?.let { mask ->
            Spacer(Modifier.height(12.dp)); HorizontalDivider(color = Color(0xFF353535)); Spacer(Modifier.height(10.dp))
            Text("PROPIEDADES DE MÁSCARA", color = Color(0xFF929292), fontSize = 9.sp)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MaskButton(if (mask.isInverted) "Invertida" else "Invertir", Modifier.weight(1f)) { onIntent(EditorIntent.ToggleMaskInvert(mask.id)) }
                MaskButton(if (mask.selectionMode == SelectionMode.ADD) "Modo +" else "Modo −", Modifier.weight(1f)) {
                    onIntent(EditorIntent.UpdateMaskSelectionMode(mask.id, if (mask.selectionMode == SelectionMode.ADD) SelectionMode.SUBTRACT else SelectionMode.ADD))
                }
            }
            Spacer(Modifier.height(7.dp))
            Text("Suavizado ${mask.featherRadius.toInt()} px", color = Color(0xFFBDBDBD), fontSize = 10.sp)
            Slider(value = mask.featherRadius, onValueChange = { onIntent(EditorIntent.UpdateMaskFeather(mask.id, it)) }, valueRange = 0f..100f)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MaskButton("Limpiar", Modifier.weight(1f)) { onIntent(EditorIntent.ClearMask(mask.id)) }
                MaskButton("Borrar trazos", Modifier.weight(1f)) { onIntent(EditorIntent.ClearMaskBrushStrokes(mask.id)) }
            }
            Spacer(Modifier.height(12.dp)); HorizontalDivider(color = Color(0xFF353535)); Spacer(Modifier.height(10.dp))
            Text("AJUSTES LOCALES", color = Color(0xFF929292), fontSize = 9.sp)
            LocalAdjustment("Brillo", mask.localAdjustments.brightness, -1f..1f) { onIntent(EditorIntent.UpdateMaskLocalAdjustments(mask.id, mask.localAdjustments.copy(brightness = it))) }
            LocalAdjustment("Contraste", mask.localAdjustments.contrast, 0f..2f) { onIntent(EditorIntent.UpdateMaskLocalAdjustments(mask.id, mask.localAdjustments.copy(contrast = it))) }
            LocalAdjustment("Saturación", mask.localAdjustments.saturation, 0f..2f) { onIntent(EditorIntent.UpdateMaskLocalAdjustments(mask.id, mask.localAdjustments.copy(saturation = it))) }
            LocalAdjustment("Exposición", mask.localAdjustments.exposure, -2f..2f) { onIntent(EditorIntent.UpdateMaskLocalAdjustments(mask.id, mask.localAdjustments.copy(exposure = it))) }
        }
    }
}

@Composable
private fun MaskButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 3.dp)) { Text(text, fontSize = 9.sp, maxLines = 1) }
}

@Composable
private fun LocalAdjustment(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 5.dp)) {
        Row(Modifier.fillMaxWidth()) { Text(label, color = Color(0xFFD0D0D0), fontSize = 9.sp, modifier = Modifier.weight(1f)); Text(String.format("%.2f", value), color = Color(0xFF999999), fontSize = 8.sp) }
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.height(28.dp))
    }
}
