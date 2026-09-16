# Reglas de Arquitectura Permanente de HyperEditor

Este documento define la arquitectura canónica y los invariantes del sistema HyperEditor. Son restricciones absolutas, no negociables y aplicables a cualquier función presente o futura.

## Invariantes del Sistema

1. **Sin Publicidad**: No se permite ningún SDK de publicidad ni mediador.
2. **Sin Conectividad (100% Offline)**: Ausencia absoluta de `android.permission.INTERNET` y de cualquier API de comunicación por red.
3. **Sin Inteligencia Artificial**: Cero inferencias, modelos de ML o redes neuronales. Toda manipulación es matemática y determinista (matrices afines, algoritmos de renderizado de Canvas, matrices de color y composición de bitmaps en memoria).
4. **Sin Telemetría**: Cero trackers, analítica ni recolección de métricas.
5. **Sin Caché en Disco (RAM Only)**:
   - Todo el estado (`EditorDocument`, historial `Undo/Redo`, trazos de pincel, selecciones, transformaciones) reside en memoria RAM.
   - Prohibido el uso de `cacheDir` y `externalCacheDir` para buffers de edición intermedia.
   - La única escritura en disco es al finalizar la exportación directamente a `MediaStore`.
6. **Protección de Funciones Existentes (No Regresiones)**:
   - Toda función ya operativa debe mantenerse íntegra tras cualquier cambio.

## Pipeline de Renderizado y Memoria

```
[Bitmap Original] ──(In-Memory Stream)──► [EditorDocument (RAM)]
                                                │
                                    ┌───────────┴───────────┐
                                    ▼                       ▼
                         [Interactive UI View]     [Export Full-Res]
                             (Preview Canvas)       (Pure Memory Render)
                                                            │
                                                            ▼
                                                    [MediaStore Stream]
                                                    (Direct Output, No Temp Cache)
```
