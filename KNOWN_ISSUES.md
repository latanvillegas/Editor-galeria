# Known Issues (HyperEditor)

## [2026-09-09] Error: Bloqueo y timeout del contenedor al intentar instalar JDK y Android SDK con apt-get
Síntoma: Llamadas a comandos y lectura de archivos arrojaron `RPC::DEADLINE_EXCEEDED (Server deadline expired)` y `Timed out waiting for applet file system condition`.
Causa raíz: La ejecución de `apt-get install` para JDK y utilidades en el contenedor de desarrollo web disparó prompts interactivos (`fontconfig-config`) y consumo de memoria que congeló el contenedor.
Solución aplicada: Se reinició el contenedor. La compilación del APK nativo de Android no debe ejecutarse dentro del contenedor web de AI Studio (que está optimizado para Vite/Node.js en puerto 3000), sino a través del flujo de integración continua ya configurado en `.github/workflows/android-build.yml` (GitHub Actions con runners Ubuntu y Android SDK preinstalado) o localmente en Android Studio.
Prevención: Delegar la compilación pesada del APK a GitHub Actions / Android Studio y mantener el contenedor web enfocado en la documentación, control de versiones y el simulador interactivo de HyperEditor.

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
