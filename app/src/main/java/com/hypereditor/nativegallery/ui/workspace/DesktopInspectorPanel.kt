package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
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

enum class InspectorTab { LAYERS, HISTORY }

@Composable
fun DesktopInspectorPanel(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(InspectorTab.LAYERS) }

    Column(modifier.fillMaxSize().background(Color(0xFF252525))) {
        Row(Modifier.fillMaxWidth().height(38.dp)) {
            InspectorTabButton(
                text = "Capas",
                selected = tab == InspectorTab.LAYERS,
                modifier = Modifier.weight(1f),
                onClick = { tab = InspectorTab.LAYERS }
            )
            InspectorTabButton(
                text = "Historial",
                selected = tab == InspectorTab.HISTORY,
                modifier = Modifier.weight(1f),
                onClick = { tab = InspectorTab.HISTORY }
            )
        }
        HorizontalDivider(color = Color(0xFF3A3A3A))
        when (tab) {
            InspectorTab.LAYERS -> LayersContent(state, onIntent)
            InspectorTab.HISTORY -> HistoryContent(state)
        }
    }
}

@Composable
private fun InspectorTabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick).background(if (selected) Color(0xFF333333) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else Color(0xFFAAAAAA), fontSize = 12.sp)
    }
}

@Composable
private fun LayersContent(state: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    val layers = state.document?.layers.orEmpty()
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Layers, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text("CAPAS", color = Color(0xFFBDBDBD), fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onIntent(EditorIntent.SelectTab(EditorSectionTab.LAYERS)) }) {
                Text("Administrar", fontSize = 10.sp)
            }
        }
        HorizontalDivider(color = Color(0xFF353535))
        LazyColumn(Modifier.weight(1f)) {
            items(layers.asReversed(), key = { it.id }) { layer ->
                val active = layer.id == state.activeLayerId
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (active) Color(0xFF3B4654) else Color.Transparent)
                        .clickable { onIntent(EditorIntent.SelectTab(EditorSectionTab.LAYERS)) }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Visibility, null, tint = if (layer.isVisible) Color(0xFFDADADA) else Color(0xFF666666), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(9.dp))
                    Box(Modifier.size(32.dp).background(Color(0xFF404040)), contentAlignment = Alignment.Center) {
                        Text(layer.name.take(1).uppercase(), color = Color.White, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(layer.name, color = Color(0xFFE0E0E0), fontSize = 12.sp, maxLines = 1)
                        Text("${layer.blendMode.name.lowercase()} • ${(layer.opacity * 100).toInt()}%", color = Color(0xFF8E8E8E), fontSize = 9.sp)
                    }
                }
                HorizontalDivider(color = Color(0xFF303030))
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, null, tint = Color(0xFFDADADA), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(9.dp))
                    Box(Modifier.size(32.dp).background(Color(0xFF3A3A3A)), contentAlignment = Alignment.Center) { Text("IMG", color = Color.White, fontSize = 8.sp) }
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text("Imagen original", color = Color(0xFFE0E0E0), fontSize = 12.sp)
                        Text("Fondo", color = Color(0xFF8E8E8E), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryContent(state: EditorUiState) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text("HISTORIAL", color = Color(0xFFBDBDBD), fontSize = 10.sp)
        }
        HorizontalDivider(color = Color(0xFF353535))
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.historyList.asReversed()) { entry ->
                Text(entry, color = Color(0xFFD0D0D0), fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
                HorizontalDivider(color = Color(0xFF303030))
            }
        }
    }
}
