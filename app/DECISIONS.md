# DECISIONES TÉCNICAS — HyperEditor Android

## [2026-09-16] Arquitectura del Recorte Personalizado (Freeform Custom Crop)
- **Coordenadas Normalizadas [0..1]**: El estado `CustomCropState` opera enteramente en espacio unitario [0..1] relativo a la imagen mostrada. Esto garantiza total inmunidad a resoluciones de pantalla, escala de visualización, orientación y zoom del viewport.
- **Independencia Funcional**: Se implementó como componente dedicado `CustomCropInteractiveCanvas` sin modificar ni alterar el `CropInteractiveCanvas` ni la lógica de encuadre por proporciones existente. Ambos modos conviven armónicamente dentro de la pestaña `GEOMETRY_CROP` mediante un selector `TabRow` claro y conciso ("Encuadre" vs "Personalizado").
- **Tolerancia Táctil de 44dp**: Las 8 zonas de captura (handles) implementan un radio de contacto táctil de 44dp (`touchRadius = 44.dp.toPx()`) garantizando ergonomía de nivel profesional en dispositivos táctiles sin solapamientos accidentales.
- **Pipeline No Destructivo & Undo/Redo**: Al presionar "Aplicar", se despacha `EditorIntent.ApplyCustomFreeCrop` que actualiza el `EditorDocument.cropTransform` registrando una sola entrada en el historial de `historyManager`, manteniendo la compatibilidad total con el pipeline de renderizado `BitmapRenderer` y la exportación en alta resolución sin artefactos visuales ni superposiciones de UI.
- **Sin Inteligencia Artificial**: Procesamiento 100% matemático y local en dispositivo, sin llamadas a red, modelos ML ni librerías externas de inferencia.
