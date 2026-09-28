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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Desktop-oriented chrome inspired by professional image editors.
 * It deliberately owns no document state: HyperEditorScreen remains the coordinator
 * while this package provides reusable, testable workspace pieces.
 */
enum class DesktopTool(val label: String, val shortcut: String, val icon: ImageVector) {
    MOVE("Mover", "V", Icons.Default.OpenWith),
    SELECT("Selección", "M", Icons.Default.SelectAll),
    CROP("Recortar", "C", Icons.Default.Crop),
    BRUSH("Pincel", "B", Icons.Default.Brush),
    HEAL("Corrector", "J", Icons.Default.Healing),
    CLONE("Clonar", "S", Icons.Default.ContentCopy),
    TEXT("Texto", "T", Icons.Default.TextFields),
    HAND("Mano", "H", Icons.Default.PanTool),
    ZOOM("Zoom", "Z", Icons.Default.ZoomIn)
}

@Composable
fun DesktopMenuBar(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(34.dp),
        color = Color(0xFF242424),
        border = BorderStroke(1.dp, Color(0xFF353535))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HyperEditor", color = Color(0xFFF0F0F0), fontSize = 13.sp)
            Spacer(Modifier.width(14.dp))
            DesktopMenuItem("Archivo")
            DesktopMenuItem("Editar")
            DesktopMenuItem("Imagen")
            DesktopMenuItem("Capa")
            DesktopMenuItem("Selección")
            DesktopMenuItem("Filtro")
            DesktopMenuItem("Ver")
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Deshacer", tint = if (canUndo) Color.White else Color.Gray)
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.Redo, "Rehacer", tint = if (canRedo) Color.White else Color.Gray)
            }
            TextButton(onClick = onSave, enabled = !isSaving) {
                Text(if (isSaving) "Guardando…" else "Guardar", color = Color(0xFFB9D9FF), fontSize = 12.sp)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
            }
        }
    }
}

@Composable
private fun DesktopMenuItem(text: String) {
    Box(
        Modifier
            .height(30.dp)
            .clickable { /* Menu wiring is introduced incrementally. */ }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFFD8D8D8), fontSize = 12.sp)
    }
}

@Composable
fun DesktopToolOptionsBar(
    activeTool: DesktopTool,
    documentSize: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(38.dp),
        color = Color(0xFF2D2D2D),
        border = BorderStroke(1.dp, Color(0xFF3B3B3B))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(activeTool.icon, null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(activeTool.label, color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.width(12.dp))
            Text("[${activeTool.shortcut}]", color = Color(0xFFAAAAAA), fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            documentSize?.let { Text(it, color = Color(0xFFAAAAAA), fontSize = 11.sp) }
        }
    }
}

@Composable
fun DesktopToolBar(
    selected: DesktopTool,
    onSelect: (DesktopTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.width(50.dp).fillMaxHeight(),
        color = Color(0xFF292929),
        border = BorderStroke(1.dp, Color(0xFF3B3B3B))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DesktopTool.entries.forEach { tool ->
                val selectedColor = if (selected == tool) Color(0xFF4B4B4B) else Color.Transparent
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .size(38.dp)
                        .background(selectedColor, RoundedCornerShape(4.dp))
                        .clickable { onSelect(tool) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tool.icon, "${tool.label} (${tool.shortcut})", tint = Color(0xFFE5E5E5), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun DesktopStatusBar(
    zoomPercent: Int,
    documentSize: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(26.dp),
        color = Color(0xFF242424),
        border = BorderStroke(1.dp, Color(0xFF353535))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$zoomPercent%", color = Color(0xFFD0D0D0), fontSize = 11.sp)
            Spacer(Modifier.width(16.dp))
            documentSize?.let { Text(it, color = Color(0xFFAFAFAF), fontSize = 11.sp) }
            Spacer(Modifier.weight(1f))
            Text("RGB • Offline", color = Color(0xFF8F8F8F), fontSize = 10.sp)
        }
    }
}
