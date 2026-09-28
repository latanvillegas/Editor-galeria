package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorUiState

private data class DesktopFilter(val id: String, val label: String)
private val desktopFilters = listOf(
    DesktopFilter("BW", "B&N"), DesktopFilter("SEPIA", "Sepia"),
    DesktopFilter("VIVID", "Vívido"), DesktopFilter("CINE", "Cine"),
    DesktopFilter("WARM", "Cálido"), DesktopFilter("COLD", "Frío"),
    DesktopFilter("DRAMATIC", "Dramático"), DesktopFilter("NOIR", "Noir")
)

@Composable
fun DesktopFiltersPanel(state: EditorUiState, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val active = state.document?.appliedFilter
    var presetName by remember { mutableStateOf("") }
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("FILTROS", color = Color(0xFF929292), fontSize = 9.sp, modifier = Modifier.weight(1f))
            if (active != null) TextButton(onClick = { onIntent(EditorIntent.ClearFilter) }) { Text("Quitar", fontSize = 9.sp) }
        }
        desktopFilters.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { filter ->
                    val selected = active?.filterName.equals(filter.id, ignoreCase = true)
                    Surface(
                        modifier = Modifier.weight(1f).height(38.dp).clickable { onIntent(EditorIntent.ApplyFilter(filter.id, active?.intensity ?: 1f)) },
                        shape = RoundedCornerShape(5.dp),
                        color = if (selected) Color(0xFF46586C) else Color(0xFF303030),
                        border = BorderStroke(1.dp, if (selected) Color(0xFF7EA7D8) else Color(0xFF444444))
                    ) { Box(contentAlignment = Alignment.Center) { Text(filter.label, color = Color(0xFFE0E0E0), fontSize = 10.sp) } }
                }
            }
        }
        if (active != null) {
            Spacer(Modifier.height(6.dp))
            Text("Intensidad ${(active.intensity * 100).toInt()}%", color = Color(0xFFBDBDBD), fontSize = 10.sp)
            Slider(value = active.intensity, onValueChange = { onIntent(EditorIntent.UpdateFilterIntensity(it)) }, valueRange = 0f..1f)
        }
        Spacer(Modifier.height(8.dp)); HorizontalDivider(color = Color(0xFF353535)); Spacer(Modifier.height(12.dp))
        Text("PRESETS", color = Color(0xFF929292), fontSize = 9.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = presetName, onValueChange = { presetName = it }, label = { Text("Nombre", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Button(onClick = { val name = presetName.trim(); if (name.isNotEmpty()) { onIntent(EditorIntent.SaveUserPreset(name)); presetName = "" } }, enabled = presetName.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Guardar preset", fontSize = 10.sp) }
        Spacer(Modifier.height(8.dp))
        state.userPresets.forEach { preset ->
            Row(Modifier.fillMaxWidth().background(Color(0xFF303030), RoundedCornerShape(5.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { onIntent(EditorIntent.ApplyUserPreset(preset)) }) {
                    Text(preset.name, color = Color(0xFFE0E0E0), fontSize = 11.sp)
                    Text(if (preset.appliedFilter != null) "${preset.appliedFilter.filterName} + ajustes" else "Ajustes", color = Color(0xFF8F8F8F), fontSize = 9.sp)
                }
                TextButton(onClick = { onIntent(EditorIntent.DeleteUserPreset(preset.id)) }) { Text("Eliminar", fontSize = 9.sp) }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
