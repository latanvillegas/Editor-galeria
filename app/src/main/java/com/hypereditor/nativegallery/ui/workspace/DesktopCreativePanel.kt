package com.hypereditor.nativegallery.ui.workspace

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

enum class DesktopCreativeTool(val label: String) { BRUSH("Pincel"), TEXT("Texto"), CLONE("Clonar"), HEALING("Healing"), PATCH("Patch"), PORTRAIT_LIGHT("Luz retrato"), FACIAL_RELIGHT("Relight") }

@Composable
fun DesktopCreativePanel(state: EditorUiState, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val selection = LocalDesktopCreativeSelection.current
    val selected = selection.selected
    val doc = state.document
    Column(modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("HERRAMIENTAS CREATIVAS", color = Color(0xFF929292), fontSize = 9.sp); Spacer(Modifier.height(8.dp))
        DesktopCreativeTool.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { tool -> Surface(modifier = Modifier.weight(1f).height(38.dp).clickable { selection.selected = tool }, shape = RoundedCornerShape(4.dp), color = if (selected == tool) Color(0xFF3B5F85) else Color(0xFF303030)) { Box(contentAlignment = Alignment.Center) { Text(tool.label, color = Color(0xFFE5E5E5), fontSize = 9.sp) } } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp)); HorizontalDivider(color = Color(0xFF353535)); Spacer(Modifier.height(10.dp))
        when (selected) {
            DesktopCreativeTool.BRUSH -> ToolStatus("Pincel", "${doc?.brushStrokes?.size ?: 0} trazos", "Limpiar trazos") { onIntent(EditorIntent.ClearBrushStrokes) }
            DesktopCreativeTool.TEXT -> TextTool(state, onIntent)
            DesktopCreativeTool.CLONE -> ToolStatus("Clonar", "${doc?.cloneStamps?.size ?: 0} puntos", "Limpiar clonado") { onIntent(EditorIntent.ClearCloneStamps) }
            DesktopCreativeTool.HEALING -> ToolStatus("Healing", "${doc?.healingStrokes?.size ?: 0} trazos", "Limpiar healing") { onIntent(EditorIntent.ClearHealingStrokes) }
            DesktopCreativeTool.PATCH -> ToolStatus("Patch", "${doc?.patchOperations?.size ?: 0} operaciones", "Limpiar patch") { onIntent(EditorIntent.ClearPatchOperations) }
            DesktopCreativeTool.PORTRAIT_LIGHT -> ToolStatus("Luz de retrato", "${doc?.portraitLights?.size ?: 0} luces", "Limpiar luces") { onIntent(EditorIntent.ClearPortraitLights) }
            DesktopCreativeTool.FACIAL_RELIGHT -> ToolStatus("Facial Relight", "${doc?.facialRelights?.size ?: 0} ajustes", "Limpiar relight") { onIntent(EditorIntent.ClearFacialRelights) }
        }
    }
}

@Composable private fun ToolStatus(title: String, count: String, clearLabel: String, onClear: () -> Unit) { Text(title.uppercase(), color = Color(0xFFBDBDBD), fontSize = 9.sp); Text(count, color = Color(0xFF888888), fontSize = 10.sp, modifier = Modifier.padding(vertical = 8.dp)); Text("La interacción se realiza directamente sobre el canvas central.", color = Color(0xFF929292), fontSize = 10.sp, lineHeight = 15.sp); Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text(clearLabel, fontSize = 9.sp) } }

@Composable private fun TextTool(state: EditorUiState, onIntent: (EditorIntent) -> Unit) {
    var text by remember { mutableStateOf("") }; Text("TEXTO", color = Color(0xFFBDBDBD), fontSize = 9.sp); Spacer(Modifier.height(7.dp)); OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Contenido") }, singleLine = true); Spacer(Modifier.height(7.dp)); Button(onClick = { onIntent(EditorIntent.AddTextOverlay(text = text)); text = "" }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Añadir texto", fontSize = 10.sp) }
    state.document?.textOverlays.orEmpty().forEach { item -> Row(Modifier.fillMaxWidth().padding(top = 5.dp).background(Color(0xFF303030), RoundedCornerShape(4.dp)).padding(7.dp), verticalAlignment = Alignment.CenterVertically) { Text(item.text, color = Color(0xFFE0E0E0), fontSize = 10.sp, maxLines = 1, modifier = Modifier.weight(1f)); TextButton(onClick = { onIntent(EditorIntent.DeleteTextOverlay(item.id)) }) { Text("Eliminar", fontSize = 8.sp) } } }
}
