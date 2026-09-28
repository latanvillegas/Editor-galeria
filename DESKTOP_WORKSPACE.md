# HyperEditor Desktop Workspace

Objetivo: evolucionar HyperEditor hacia un workspace de edición profesional de escritorio, optimizado para landscape, tablets, Android Desktop/DeX, teclado, ratón y stylus, sin copiar branding ni recursos propietarios.

## Invariantes

Se mantienen ARCHITECTURE.md y sus reglas: offline, sin IA, sin telemetría, RAM-only, exportación final vía MediaStore y cero regresiones.

## Layout objetivo

```text
MenuBar: Archivo | Editar | Imagen | Capa | Selección | Filtro | Ver
ToolOptionsBar
+---------+--------------------------------------+------------------+
| Toolbar |                                      | Panels           |
| V/M/C/B |              Canvas                  | Color/Ajustes    |
| J/S/T/H |                                      | Historial/Capas  |
| Z       |                                      | Propiedades      |
+---------+--------------------------------------+------------------+
StatusBar: zoom | tamaño documento | RGB | Offline
```

## Fases

### Fase 1 — Chrome desktop
- Componentes desacoplados en `ui/workspace/DesktopWorkspaceChrome.kt`.
- Menu bar, options bar, toolbar vertical y status bar.
- Branch aislada para evitar regresiones.

### Fase 2 — Integración del viewport
- Mantener un único canvas central.
- Mapear herramientas desktop a los canvas interactivos existentes.
- El cambio de herramienta no debe reconstruir el workspace completo.
- Preservar Brush, Text, Clone, Healing, Patch, Crop, Portrait Light, Facial Relight y Masks.

### Fase 3 — Paneles
- LayersPanel: visibilidad, selección, opacidad, reorder, rename, duplicate, lock.
- PropertiesPanel contextual por herramienta/capa.
- HistoryPanel persistente.
- Color/Adjustments/Histogram como paneles independientes.

### Fase 4 — Input de escritorio
- Atajos: V mover, M selección, C crop, B brush, J healing, S clone, T text, H hand, Z zoom.
- Ctrl+Z / Ctrl+Shift+Z.
- Space+drag para pan.
- Wheel/trackpad para zoom.
- Mouse hover y cursor contextual.
- Stylus pressure cuando el dispositivo lo exponga, con fallback determinista.

### Fase 5 — Motor
- Preview y export renderers separados por política de resolución.
- Invalidación por dirty stages para evitar rerender global innecesario.
- Historial basado en operaciones/deltas, sin snapshots bitmap por paso.
- Tests golden del RenderPipeline y pruebas de memoria con imágenes grandes.

## Criterio de aceptación

El modo desktop debe poder operar sin red, conservar todas las herramientas existentes y ofrecer un flujo estable con teclado/ratón/stylus. Ninguna herramienta existente puede desaparecer durante la migración.
