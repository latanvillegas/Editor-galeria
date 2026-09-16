# Known Issues (HyperEditor)

## [2026-09-16] Error: Fallo de compilación Kotlin en TextInteractiveCanvas (Unresolved reference 'Key' y 'WHITE')
Síntoma: El task `:app:compileDebugKotlin` en GitHub Actions falló con:
1. `Unresolved reference 'Key'` en línea 99.
2. `@Composable invocations can only happen from the context of a @Composable function` en línea 100.
3. `Unresolved reference 'WHITE'` en línea 324.
Causa raíz:
1. En Jetpack Compose, el agrupador de clave de composición es la función composable con minúscula `key(...)` (de `androidx.compose.runtime.key`), pero se había escrito con mayúscula `Key(...)`, el cual fue interpretado como un tipo inexistente y no proporcionó el scope `@Composable` a su lambda hijo.
2. En `androidx.compose.ui.graphics.Color`, la constante de color blanco se define como `Color.White` (PascalCase), mientras que se había escrito `Color.WHITE` (formato de `android.graphics.Color`).
Solución aplicada:
1. Se reemplazó `Key(textItem.id)` por `key(textItem.id)` en `TextInteractiveCanvas.kt`.
2. Se corrigió `tint = Color.WHITE` por `tint = Color.White`.
Prevención: Respetar las convenciones de nombres de Jetpack Compose: `key` en minúscula para control de recombinación y constantes de color de `androidx.compose.ui.graphics.Color` en PascalCase (`Color.White`, `Color.Black`, etc.).

## [2026-09-09 / 2026-09-16] Error: Bloqueo y timeout del contenedor al intentar compilar APK nativo con Gradle
Síntoma: Intentos de ejecutar `./gradlew assembleDebug` dentro del contenedor web de AI Studio provocan bloqueos del daemon de Gradle, agotamiento de recursos y eventual reinicio del contenedor por `RPC::DEADLINE_EXCEEDED` o `There was an unexpected error`.
Causa raíz: El contenedor de desarrollo en la nube está dimensionado y configurado específicamente para ejecutar el dev server web (Vite/Node.js en puerto 3000) y tareas de edición de código. Los procesos pesados de Gradle Daemon (JVM con compilación Kotlin/Dexing multihilo de Android) saturan la memoria y los límites de ejecución asíncrona del contenedor.
Solución aplicada: Se confirma que el contenedor se recuperó limpiamente tras el reinicio. La suite nativa de Android está completamente preservada y lista para su compilación regular en el pipeline de CI/CD `.github/workflows/android-build.yml` (GitHub Actions con runners estándar de Ubuntu y Android SDK preconfigurado) o en Android Studio local. El simulador web de HyperEditor (`/src`) continúa compilando y operando al 100% de manera inmediata.
Prevención: No lanzar tareas de compilación pesada de Gradle (`assembleDebug`/`bundleRelease`) en el entorno de desarrollo web en la nube; confiar la compilación automatizada del artefacto APK al workflow de GitHub Actions ya verificado en el repositorio.

## [2026-09-15] Error: Fallo de compilación Kotlin por referencias no resueltas y discrepancias de parámetros
Síntoma: La compilación de Gradle en CI arrojó errores de referencias no resueltas (`computeImageBounds`, `UpdateFacialRelightZones`, `renderPreviewFast`, `Bitmap`, `patches`) e incoherencias de parámetros nominales (`centerX` vs `centerXNorm`, `globalSmoothness`).
Causa raíz:
1. `computeImageBounds` era invocado por varias vistas interactivas pero no estaba declarado como función compartida.
2. `EditorViewModel` carecía del import de `android.graphics.Bitmap` y del método auxiliar `renderPreviewFast`.
3. `EditorIntent` no exponía la variante `UpdateFacialRelightZones`.
4. Discrepancia en nombres de campos normalizados (`centerXNorm` vs `centerX`).
Solución aplicada:
1. Creación de `ImageBounds.kt` con la estructura de datos y cálculo unificado de escala `Fit` con soporte para destructuring.
2. Incorporación de `UpdateFacialRelightZones` en `EditorIntent` y su respectivo despacho en `EditorViewModel`.
3. Implementación de `renderPreviewFast` e import explícito de `Bitmap`.
4. Corrección de nombres de parámetros en `HyperEditorScreen` (`patchOperations`, `ClearPatchOperations`, `centerXNorm`).
5. Agregado de `globalSmoothness` al modelo `EditOperation.FacialRelight`.
Prevención: Mantener interfaces de intención y modelos de datos tipados estrictamente sincronizados en `EditorDocument`, `EditOperation` y `EditorIntent`.

## [2026-09-15] Error: Pincel inoperativo / no dibuja al arrastrar el dedo sobre la foto
Symptom: Al seleccionar la herramienta Pincel en Herramientas Creativas y arrastrar el dedo sobre la imagen, no aparecía ningún trazo.
Root cause:
1. En `HyperEditorScreen.kt`, el área central del viewport conmutaba entre `CropInteractiveCanvas` y `CloneStampInteractiveCanvas`, pero omitía por completo un componente interactivo para `selectedCreativeTool == 0` (Pincel). En su lugar se renderizaba el canvas pasivo predeterminado que no consumía eventos táctiles de dibujo.
2. Los estados del pincel (`brushSize`, `brushColor`, `brushOpacity`, `isEraserMode`) estaban declarados con `remember` únicamente dentro del sub-bloque de la barra lateral, inaccesibles para el área del canvas.
3. `BrushDrawRenderStage` interpretaba `strokeWidth` en píxeles absolutos sin escalar por la resolución del bitmap, lo que provocaba inconsistencias entre la previsualización reducida y la exportación de alta resolución.
Applied solution:
1. Creación de `BrushInteractiveCanvas.kt` con captura directa de gestos (`detectDragGestures`), trazado acelerado en tiempo real en Compose Canvas y cursor circular bajo el dedo.
2. Elevación del estado del pincel al ámbito superior de `HyperEditorScreen` y conexión directa con `BrushInteractiveCanvas`.
3. Normalización del grosor mediante factor de escala `minDim / 1000f` en el canvas y en `BrushDrawRenderStage`, y consolidación atómica en `onDragEnd` para un único paso en Undo/Redo.
Prevention: Toda herramienta interactiva de dibujo o retoque debe tener un componente de viewport dedicado que capture gestos sobre la imagen con coordenadas normalizadas `[0f, 1f]` y cálculo de escala proporcional.

