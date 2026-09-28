package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.ui.state.EditorIntent
import com.hypereditor.nativegallery.ui.state.EditorSectionTab
import com.hypereditor.nativegallery.ui.state.EditorUiState

enum class InspectorTab { PROPERTIES, LAYERS, HISTORY }

@Composable
fun DesktopInspectorPanel(state: EditorUiState, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(InspectorTab.PROPERTIES) }
    Column(modifier.fillMaxSize().background(Color(0xFF252525))) {
        Row(Modifier.fillMaxWidth().height(38.dp)) {
            InspectorTabButton("Propiedades", tab == InspectorTab.PROPERTIES, Modifier.weight(1f)) { tab = InspectorTab.PROPERTIES }
            InspectorTabButton("Capas", tab == InspectorTab.LAYERS, Modifier.weight(1f)) { tab = InspectorTab.LAYERS }
            InspectorTabButton("Historial", tab == InspectorTab.HISTORY, Modifier.weight(1f)) { tab = InspectorTab.HISTORY }
        }
        HorizontalDivider(color = Color(0xFF3A3A3A))
        when (tab) {
            InspectorTab.PROPERTIES -> PropertiesContent(state, onIntent)
            InspectorTab.LAYERS -> LayersContent(state, onIntent)
            InspectorTab.HISTORY -> HistoryContent(state)
        }
    }
}

@Composable
private fun InspectorTabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxHeight().clickable(onClick = onClick).background(if (selected) Color(0xFF333333) else Color.Transparent), contentAlignment = Alignment.Center) {
        Text(text, color = if (selected) Color.White else Color(0xFFAAAAAA), fontSize = 10.sp)
    }
}

@Composable
private fun PropertiesContent(state: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    val title = when (state.selectedTab) {
        EditorSectionTab.GEOMETRY_CROP -> "Recorte y geometría"
        EditorSectionTab.ADJUSTMENTS -> "Ajustes"
        EditorSectionTab.FILTERS_PRESETS -> "Filtros y presets"
        EditorSectionTab.CREATIVE_TOOLS -> "Herramienta creativa"
        EditorSectionTab.LAYERS -> "Propiedades de capa"
        EditorSectionTab.MASKS_SELECTIONS -> "Máscaras y selección"
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text(title.uppercase(), color = Color(0xFFBDBDBD), fontSize = 10.sp)
        }
        HorizontalDivider(color = Color(0xFF353535))
        if (state.selectedTab == EditorSectionTab.ADJUSTMENTS) {
            DesktopAdjustmentsPanel(state = state, onIntent = onIntent, modifier = Modifier.fillMaxSize())
        } else {
            Text("Panel contextual", color = Color(0xFFE0E0E0), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
            Text("Los controles de ${title.lowercase()} se están migrando aquí sin reemplazar todavía el canvas interactivo existente.", color = Color(0xFF929292), fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 12.dp))
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF353535))
            Text("SECCIONES", color = Color(0xFF888888), fontSize = 9.sp, modifier = Modifier.padding(12.dp))
            PropertySection("Ajustes") { onIntent(EditorIntent.SelectTab(EditorSectionTab.ADJUSTMENTS)) }
            PropertySection("Recorte") { onIntent(EditorIntent.SelectTab(EditorSectionTab.GEOMETRY_CROP)) }
            PropertySection("Filtros") { onIntent(EditorIntent.SelectTab(EditorSectionTab.FILTERS_PRESETS)) }
            PropertySection("Creativo") { onIntent(EditorIntent.SelectTab(EditorSectionTab.CREATIVE_TOOLS)) }
            PropertySection("Máscaras") { onIntent(EditorIntent.SelectTab(EditorSectionTab.MASKS_SELECTIONS)) }
        }
    }
}

@Composable private fun PropertySection(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFD0D0D0), fontSize = 11.sp)
    }
}

@Composable
private fun LayersContent(state: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    val layers = state.document?.layers.orEmpty()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Layers, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text("CAPAS", color = Color(0xFFBDBDBD), fontSize = 10.sp); Spacer(Modifier.weight(1f))
            TextButton(onClick = { onIntent(EditorIntent.SelectTab(EditorSectionTab.LAYERS)) }) { Text("Administrar", fontSize = 10.sp) }
        }
        HorizontalDivider(color = Color(0xFF353535))
        LazyColumn(Modifier.weight(1f)) {
            items(layers.asReversed(), key = { it.id }) { layer ->
                val active = layer.id == state.activeLayerId
                Row(Modifier.fillMaxWidth().background(if (active) Color(0xFF3B4654) else Color.Transparent).clickable { onIntent(EditorIntent.SelectActiveLayer(layer.id)); onIntent(EditorIntent.SelectTab(EditorSectionTab.LAYERS)) }.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, "Visibilidad", tint = if (layer.isVisible) Color(0xFFDADADA) else Color(0xFF666666), modifier = Modifier.size(16.dp).clickable { onIntent(EditorIntent.ToggleLayerVisibility(layer.id)) })
                    Spacer(Modifier.width(9.dp)); Box(Modifier.size(32.dp).background(Color(0xFF404040)), contentAlignment = Alignment.Center) { Text(layer.name.take(1).uppercase(), color = Color.White, fontSize = 11.sp) }; Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) { Text(layer.name, color = Color(0xFFE0E0E0), fontSize = 12.sp, maxLines = 1); Text("${layer.blendMode.name.lowercase()} • ${(layer.opacity * 100).toInt()}%", color = Color(0xFF8E8E8E), fontSize = 9.sp) }
                }
                HorizontalDivider(color = Color(0xFF303030))
            }
            item { Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Visibility, null, tint = Color(0xFFDADADA), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(9.dp)); Box(Modifier.size(32.dp).background(Color(0xFF3A3A3A)), contentAlignment = Alignment.Center) { Text("IMG", color = Color.White, fontSize = 8.sp) }; Spacer(Modifier.width(9.dp)); Column { Text("Imagen original", color = Color(0xFFE0E0E0), fontSize = 12.sp); Text("Fondo", color = Color(0xFF8E8E8E), fontSize = 9.sp) } } }
        }
    }
}

@Composable
private fun HistoryContent(state: EditorUiState) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.History, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text("HISTORIAL", color = Color(0xFFBDBDBD), fontSize = 10.sp) }
        HorizontalDivider(color = Color(0xFF353535))
        LazyColumn(Modifier.fillMaxSize()) { items(state.historyList.asReversed()) { entry -> Text(entry, color = Color(0xFFD0D0D0), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)); HorizontalDivider(color = Color(0xFF303030)) } }
    }
}
