# Known Issues (HyperEditor)

## [2026-09-09] Error: Bloqueo y timeout del contenedor al intentar instalar JDK y Android SDK con apt-get
Síntoma: Llamadas a comandos y lectura de archivos arrojaron `RPC::DEADLINE_EXCEEDED (Server deadline expired)` y `Timed out waiting for applet file system condition`.
Causa raíz: La ejecución de `apt-get install` para JDK y utilidades en el contenedor de desarrollo web disparó prompts interactivos (`fontconfig-config`) y consumo de memoria que congeló el contenedor.
Solución aplicada: Se reinició el contenedor. La compilación del APK nativo de Android no debe ejecutarse dentro del contenedor web de AI Studio (que está optimizado para Vite/Node.js en puerto 3000), sino a través del flujo de integración continua ya configurado en `.github/workflows/android-build.yml` (GitHub Actions con runners Ubuntu y Android SDK preinstalado) o localmente en Android Studio.
Prevención: Delegar la compilación pesada del APK a GitHub Actions / Android Studio y mantener el contenedor web enfocado en la documentación, control de versiones y el simulador interactivo de HyperEditor.
