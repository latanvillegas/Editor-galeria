# PROYECTO: HyperEditor para Android (Kotlin + Jetpack Compose)

## [2026-09-16] Feature: Recorte personalizado (Selección libre real)
Status: Complete and tested
Description: Herramienta de recorte personalizada y manual independiente del encuadre por proporciones existente. Permite visualizar la imagen completa dentro del canvas, superponer un CropRect editable con oscurecimiento exterior, borde de alto contraste, 8 handles táctiles (4 esquinas en ángulo y 4 puntos medios de arista para ajuste unidireccional), movimiento libre arrastrando el interior, toggle de bloqueo de relación de aspecto, toggle de cuadrícula 3×3 (regla de tercios), lectura en tiempo real de ancho/alto en píxeles y relación, botón de restablecer a imagen completa, botón cancelar y botón aplicar que se integra limpiamente con el pipeline no destructivo y el historial de Undo/Redo.
Files involved:
- /app/src/main/java/com/hypereditor/nativegallery/ui/canvas/CustomCropInteractiveCanvas.kt
- /app/src/main/java/com/hypereditor/nativegallery/ui/state/EditorIntent.kt
- /app/src/main/java/com/hypereditor/nativegallery/ui/state/EditorViewModel.kt
- /app/src/main/java/com/hypereditor/nativegallery/ui/HyperEditorScreen.kt
