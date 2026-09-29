package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DesktopTool(val label: String, val shortcut: String, val icon: ImageVector) {
    MOVE("Mover", "V", Icons.Default.OpenWith), SELECT("Selección", "M", Icons.Default.SelectAll), CROP("Recortar", "C", Icons.Default.Crop), BRUSH("Pincel", "B", Icons.Default.Brush), HEAL("Corrector", "J", Icons.Default.Healing), CLONE("Clonar", "S", Icons.Default.ContentCopy), TEXT("Texto", "T", Icons.Default.TextFields), HAND("Mano", "H", Icons.Default.PanTool), ZOOM("Zoom", "Z", Icons.Default.ZoomIn)
}

@Composable fun DesktopMenuBar(modifier: Modifier = Modifier, onClose: () -> Unit, onUndo: () -> Unit, onRedo: () -> Unit, onSave: () -> Unit, canUndo: Boolean, canRedo: Boolean, isSaving: Boolean) {
    Surface(modifier = modifier.fillMaxWidth().height(34.dp), color = Color(0xFF242424), border = BorderStroke(1.dp, Color(0xFF353535))) { Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("HyperEditor", color = Color(0xFFF0F0F0), fontSize = 13.sp); Spacer(Modifier.width(14.dp)); listOf("Archivo", "Editar", "Imagen", "Capa", "Selección", "Filtro", "Ver").forEach { DesktopMenuItem(it) }; Spacer(Modifier.weight(1f)); IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(32.dp)) { Icon(Icons.AutoMirrored.Filled.Undo, "Deshacer", tint = if (canUndo) Color.White else Color.Gray) }; IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(32.dp)) { Icon(Icons.AutoMirrored.Filled.Redo, "Rehacer", tint = if (canRedo) Color.White else Color.Gray) }; TextButton(onClick = onSave, enabled = !isSaving) { Text(if (isSaving) "Guardando…" else "Guardar", color = Color(0xFFB9D9FF), fontSize = 12.sp) }; IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Cerrar", tint = Color.White) } } }
}
@Composable private fun DesktopMenuItem(text: String) { Box(Modifier.height(30.dp).clickable { }.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) { Text(text, color = Color(0xFFD8D8D8), fontSize = 12.sp) } }

@Composable fun DesktopToolOptionsBar(activeTool: DesktopTool, documentSize: String?, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().height(38.dp), color = Color(0xFF2D2D2D), border = BorderStroke(1.dp, Color(0xFF3B3B3B))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(activeTool.icon, null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text(activeTool.label, color = Color.White, fontSize = 12.sp); Spacer(Modifier.width(12.dp)); Text("[${activeTool.shortcut}]", color = Color(0xFFAAAAAA), fontSize = 11.sp)
            if (activeTool == DesktopTool.BRUSH) {
                val brush = LocalDesktopBrushState.current
                var presetsOpen by remember { mutableStateOf(false) }
                Spacer(Modifier.width(12.dp))
                Box {
                    TextButton(onClick = { presetsOpen = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("${brush.activePresetLabel} ▾", color = Color(0xFFD8D8D8), fontSize = 11.sp) }
                    DropdownMenu(expanded = presetsOpen, onDismissRequest = { presetsOpen = false }) {
                        brush.presets.forEach { preset -> DropdownMenuItem(text = { Text("${preset.label}  ${preset.size.toInt()} px / ${(preset.opacity * 100).toInt()}%") }, onClick = { brush.applyPreset(preset); presetsOpen = false }) }
                    }
                }
                Spacer(Modifier.width(8.dp)); Text("Tamaño", color = Color(0xFFB8B8B8), fontSize = 11.sp)
                IconButton(onClick = { brush.adjustSize(-2f) }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Remove, "Reducir pincel", tint = Color(0xFFD0D0D0), modifier = Modifier.size(14.dp)) }
                Text("${brush.size.toInt()} px", color = Color.White, fontSize = 11.sp)
                IconButton(onClick = { brush.adjustSize(2f) }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Add, "Aumentar pincel", tint = Color(0xFFD0D0D0), modifier = Modifier.size(14.dp)) }
                Spacer(Modifier.width(10.dp)); Text("Op. ${(brush.opacity * 100).toInt()}%", color = Color(0xFFB8B8B8), fontSize = 11.sp)
                Slider(value = brush.opacity, onValueChange = { brush.setOpacity(it) }, valueRange = 0.05f..1f, modifier = Modifier.width(90.dp))
                Spacer(Modifier.width(6.dp)); TextButton(onClick = { brush.toggleEraser() }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) { Icon(Icons.Default.AutoFixOff, null, tint = if (brush.isEraser) Color(0xFFB9D9FF) else Color(0xFFB8B8B8), modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text(if (brush.isEraser) "Borrador ON" else "Borrador", color = if (brush.isEraser) Color(0xFFB9D9FF) else Color(0xFFB8B8B8), fontSize = 11.sp) }
            } else { Spacer(Modifier.width(16.dp)); Text("•", color = Color(0xFF666666), fontSize = 11.sp); Spacer(Modifier.width(16.dp)); Text(toolHint(activeTool), color = Color(0xFFB8B8B8), fontSize = 11.sp) }
            Spacer(Modifier.weight(1f)); documentSize?.let { Text(it, color = Color(0xFFAAAAAA), fontSize = 11.sp) }
        }
    }
}
private fun toolHint(tool: DesktopTool): String = when (tool) { DesktopTool.MOVE -> "Arrastra para mover • Flechas para ajustar"; DesktopTool.SELECT -> "Crea o modifica una selección"; DesktopTool.CROP -> "Arrastra bordes y esquinas para recortar"; DesktopTool.BRUSH -> "Pinta sobre la imagen"; DesktopTool.HEAL -> "Corrige imperfecciones con pincel"; DesktopTool.CLONE -> "Clona desde una zona de origen"; DesktopTool.TEXT -> "Haz clic para añadir texto"; DesktopTool.HAND -> "Navega el lienzo • Flechas desplazan"; DesktopTool.ZOOM -> "Ctrl + / Ctrl - • Ctrl+0 restablece" }

@Composable fun DesktopToolBar(selected: DesktopTool, onSelect: (DesktopTool) -> Unit, modifier: Modifier = Modifier) { Surface(modifier = modifier.width(50.dp).fillMaxHeight(), color = Color(0xFF292929), border = BorderStroke(1.dp, Color(0xFF3B3B3B))) { Column(Modifier.fillMaxSize().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) { DesktopTool.entries.forEach { tool -> val selectedColor = if (selected == tool) Color(0xFF4B4B4B) else Color.Transparent; Box(Modifier.padding(vertical = 2.dp).size(38.dp).background(selectedColor, RoundedCornerShape(4.dp)).clickable { onSelect(tool) }, contentAlignment = Alignment.Center) { Icon(tool.icon, "${tool.label} (${tool.shortcut})", tint = if (selected == tool) Color.White else Color(0xFFE5E5E5), modifier = Modifier.size(20.dp)) } }; Spacer(Modifier.weight(1f)); DesktopColorSwatches(); Spacer(Modifier.height(10.dp)) } } }

@Composable private fun DesktopColorSwatches() { val colors = LocalDesktopColorState.current; Box(Modifier.size(40.dp)) { Surface(modifier = Modifier.size(24.dp).align(Alignment.BottomEnd), color = colors.background, border = BorderStroke(1.dp, Color(0xFF777777)), shape = RoundedCornerShape(2.dp)) {}; Surface(modifier = Modifier.size(24.dp).align(Alignment.TopStart), color = colors.foreground, border = BorderStroke(1.dp, Color(0xFFB0B0B0)), shape = RoundedCornerShape(2.dp)) {}; Text("↔", color = Color(0xFFBDBDBD), fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd).clickable { colors.swap() }); Text("D", color = Color(0xFF8F8F8F), fontSize = 8.sp, modifier = Modifier.align(Alignment.BottomStart).clickable { colors.reset() }) } }

@Composable fun DesktopStatusBar(zoomPercent: Int, documentSize: String?, onZoomOut: () -> Unit, onZoomIn: () -> Unit, onResetView: () -> Unit, modifier: Modifier = Modifier) { Surface(modifier = modifier.fillMaxWidth().height(30.dp), color = Color(0xFF242424), border = BorderStroke(1.dp, Color(0xFF353535))) { Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onZoomOut, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Remove, "Alejar", tint = Color(0xFFD0D0D0), modifier = Modifier.size(15.dp)) }; Text("$zoomPercent%", color = Color(0xFFD0D0D0), fontSize = 11.sp, modifier = Modifier.clickable(onClick = onResetView).padding(horizontal = 6.dp)); IconButton(onClick = onZoomIn, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Add, "Acercar", tint = Color(0xFFD0D0D0), modifier = Modifier.size(15.dp)) }; Spacer(Modifier.width(12.dp)); documentSize?.let { Text(it, color = Color(0xFFAFAFAF), fontSize = 11.sp) }; Spacer(Modifier.weight(1f)); Text("RGB • Offline", color = Color(0xFF8F8F8F), fontSize = 10.sp) } } }
