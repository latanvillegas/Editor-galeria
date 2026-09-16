# PROYECTO: HyperEditor para Android (Kotlin + Jetpack Compose)

## [2026-09-16] Feature: Recorte personalizado (Selección libre real)
Status: Complete and tested
Description: Herramienta de recorte personalizada y manual independiente del encuadre por proporciones existente. Permite visualizar la imagen completa dentro del canvas, superponer un CropRect editable con oscurecimiento exterior, borde de alto contraste, 8 handles táctiles (4 esquinas en ángulo y 4 puntos medios de arista para ajuste unidireccional), movimiento libre arrastrando el interior, toggle de bloqueo de relación de aspecto, toggle de cuadrícula 3×3 (regla de tercios), lectura en tiempo real de ancho/alto en píxeles y relación, botón de restablecer a imagen completa, botón cancelar y botón aplicar que se integra limpiamente con el pipeline no destructivo y el historial de Undo/Redo.
Files involved:
- /app/src/main/java/com/hypereditor/nativegallery/ui/canvas/CustomCropInteractiveCanvas.kt
- /app/src/main/java/com/hypereditor/nativegallery/ui/state/EditorIntent.kt
- /app/src/main/java/com/hypereditor/nativegallery/ui/state/EditorViewModel.kt
- /app/src/main/java/com/hypereditor/nativegallery/ui/HyperEditorScreen.kt

## [2026-09-16] Feature: Herramienta de Texto (Tipografías personalizadas y manipulación interactiva libre)
Status: Complete and tested
Description: Implementación integral de las dos funciones esenciales para la herramienta de texto:
1. Importación de tipografías (.ttf/.otf) mediante el selector de archivos nativo de Android (Storage Access Framework / OpenDocument), copia segura al almacenamiento interno privado de la app (`custom_fonts`), validación estricta con Typeface.createFromFile() reportando errores descriptivos sin caídas, persistencia entre sesiones y selección inmediata en el carrusel de tipografías junto a las fuentes del sistema.
2. Manipulación libre de texto sobre el canvas de la foto en tiempo real: arrastrar con 1 dedo para reposicionar con conversión precisa a coordenadas normalizadas [0..1], escalado y rotación simultánea con gestos multitáctiles o mediante handles interactivos dedicados (rotación superior, redimensionamiento en esquina, eliminación rápida), bounding box de alta visibilidad para la capa seleccionada (nunca exportado en la imagen final) y registro atómico de cada cambio en el gestor de Undo/Redo al levantar los dedos.
Files involved:
- /app/src/main/java/com/hypereditor/nativegallery/data/FontManager.kt
- /app/src/main/java/com/hypereditor/nativegallery/domain/model/EditOperation.kt
- /app/src/main/java/com/hypereditor/nativegallery/render/pipeline/TextOverlayRenderStage.kt
- /app/src/main/java/com/hypereditor/nativegallery/ui/canvas/TextInteractiveCanvas.kt
- /app/src/main/java/com/hypereditor/nativegallery/ui/state/EditorIntent.kt
- /app/src/main/java/com/hypereditor/nativegallery/ui/state/EditorViewModel.kt
- /app/src/main/java/com/hypereditor/nativegallery/ui/HyperEditorScreen.kt

