package com.hypereditor.nativegallery.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hypereditor.nativegallery.ui.canvas.CanvasViewportState

/** Desktop document stage with Photoshop-style document/tool context and mouse navigation. */
@Composable
fun DesktopCanvasStage(
    documentSize: String?,
    activeTool: DesktopTool,
    zoomPercent: Int,
    viewportState: CanvasViewportState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val navigationModifier = when (activeTool) {
        DesktopTool.HAND -> Modifier.pointerInput(viewportState) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                viewportState.panX += dragAmount.x
                viewportState.panY += dragAmount.y
            }
        }
        DesktopTool.ZOOM -> Modifier.pointerInput(viewportState) {
            detectTapGestures(
                onTap = {
                    viewportState.zoom = (viewportState.zoom * 1.25f)
                        .coerceIn(viewportState.minZoom, viewportState.maxZoom)
                },
                onDoubleTap = { viewportState.reset() }
            )
        }
        else -> Modifier
    }

    Column(modifier.fillMaxSize().background(Color(0xFF181818))) {
        Row(
            Modifier.fillMaxWidth().height(30.dp).background(Color(0xFF2A2A2A)).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Image, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(7.dp))
            Text("Documento", color = Color(0xFFE0E0E0), fontSize = 11.sp)
            documentSize?.let {
                Spacer(Modifier.width(8.dp))
                Text("• $it", color = Color(0xFF8F8F8F), fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("${activeTool.label}  •  $zoomPercent%  •  RGB", color = Color(0xFF9E9E9E), fontSize = 9.sp)
        }
        HorizontalDivider(color = Color(0xFF3A3A3A))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .background(Color(0xFF151515))
                .then(navigationModifier),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
