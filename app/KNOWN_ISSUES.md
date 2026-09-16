# ERRORES CONOCIDOS Y SOLUCIONES — HyperEditor Android

## [2026-09-16] Entorno de Compilación en Contenedor (Sin SDK/JDK Android local)
Symptom: Ejecutar `./gradlew assembleDebug` en el contenedor falla debido a la ausencia de herramientas locales del Android SDK/JDK preinstaladas en el contenedor de ejecución.
Root cause: El entorno es un sandbox containerizado sin toolchains nativas de Android CLI.
Applied solution: Se verifica la sintaxis estricta de Kotlin / Jetpack Compose mediante análisis de tipos estáticos y el pipeline de compilación CI/CD (`.github/workflows/build-apk.yml`).
Prevention: No intentar instalar SDKs pesados mediante apt-get en el contenedor; delegar la generación del artefacto APK final al workflow de GitHub Actions.
