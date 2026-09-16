# Architectural Decisions (DECISIONS.md)

## [2026-09-09] Arquitectura Híbrida: Código Nativo Android + Simulador Web en Vivo
- **Contexto**: HyperEditor es un editor fotográfico profesional para Android (orientado a tablets y galerías externas como Aves). Sin embargo, en el entorno de Google AI Studio, la previsualización se sirve mediante una aplicación web en el puerto 3000.
- **Decisión**:
  1. Mantener la suite completa nativa de Android en `/app/src/main/java/com/hypereditor/nativegallery/` con Kotlin, Jetpack Compose, AndroidX, RenderPipeline desacoplado e integración de Intents (`ACTION_EDIT` / `ACTION_SEND`).
  2. Implementar un workflow automatizado en `.github/workflows/android-build.yml` para compilar y empaquetar el APK de depuración en GitHub Actions con cada commit.
  3. Implementar un simulador web interactivo completo en `/src/` que reproduce fielmente el pipeline no destructivo (ajustes cromáticos, recorte interactivo con aspecto, filtros de color, capas con modos de fusión, máscaras, pincel/borrador, texto tipográfico y clonación), permitiendo a los desarrolladores y usuarios probar las capacidades visuales en tiempo real directamente desde el navegador de AI Studio.

## [2026-09-09] Pipeline de Edición No Destructiva
- **Decisión**: Toda modificación se modela como transformaciones inmutables sobre `EditorDocument`.
- **Estructura**:
  - `GeometryTransformStage`: Rotación paso a paso, espejo y enderezado fino continuo.
  - `ColorAdjustmentStage`: Brillo, contraste, saturación, exposición, temperatura y tinte.
  - `FilterStage`: 8 matrices de color ajustables (B&N, Sepia, Vívido, Cine, Cálido, Frío, Dramático, Noir).
  - `LayersRenderStage`: Modos de mezcla (Normal, Multiply, Screen, Overlay, Darken, Lighten) y opacidades.
  - `BrushDrawRenderStage` & `CloneStampRenderStage`: Retoques directos por capas.
  - `HistoryManager`: Pila atómica de 50 niveles para Undo/Redo.

## [2026-09-09] Fase 1: Arquitectura de Edición Local, Doble Exposición y Reiluminación
- **Contexto**: Retoque localizado manual avanzado (Parche, Doble Exposición, Luz de Retrato y Reiluminación Facial) sin recurrir a IA fingida ni servicios en la nube.
- **Decisiones**:
  1. **Herramienta Parche (Patch Tool)**: Implementada mediante `PatchRenderStage` en el pipeline nativo. Utiliza extracción de parches fuente y destino en espacio de imagen con normalización de luminosidad perimetral y fusión con máscara feather radial, evitando cortes duros de textura.
  2. **Doble Exposición en Capas**: Extensión de `LayerModel` con tipo `DOUBLE_EXPOSURE` y soporte nativo de flip horizontal/vertical. Permite aplicar modos de fusión fotográfica estándar sobre cualquier imagen externa cargada vía `rememberLauncherForActivityResult`.
  3. **Luz de Retrato (Studio Relight)**: Se modela como una elipse de gradiente suave con traslación 2D interactiva. Modifica simultáneamente exposición, sombras y calidez sin alterar los píxeles destructivamente.
  4. **Reiluminación Facial Anatómica**: 6 zonas de control paramétrico (`FacialRelightZone`) con anclajes táctiles editables directamente en el canvas de Compose.
  5. **Undo/Redo**: Cada acción confirmada genera un estado inmutable en `EditorDocument` asegurando compatibilidad total con el historial.

## [2026-09-15] Recorte Personalizado (Selección Libre Real) y Unificación de Límites
- **Decisión**: Implementar `CustomCropInteractiveCanvas` de forma independiente a `CropInteractiveCanvas`. El recorte de aspecto libre usa un modelo táctil interactivo con 8 asas (esquinas y puntos medios) y cálculo dinámico de `ImageBounds` con destructuring en Kotlin (`val (imgLeft, imgTop, imgW, imgH) = computeImageBounds(canvasSize, bitmap)`).
- **Razón**: Evita interferir con la lógica de proporciones fijas (1:1, 4:3, 16:9, etc.) que ya funcionaba en la app y proporciona una experiencia limpia de selección libre basada en coordenadas normalizadas seguras en el documento.
- **Unificación de Límites**: Creación de `ImageBounds.kt` en `ui.canvas` como `data class` compartida, garantizando operadores de destructuring automáticos para Compose y eliminando errores de tipo o referencias no resueltas.
