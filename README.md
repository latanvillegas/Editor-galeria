# HyperEditor Pro (Native Android Photo Editor)

Editor fotográfico nativo en **Kotlin + Jetpack Compose** diseñado como editor externo complementario para galerías (como **Aves Gallery**) con soporte tablet-first y pipeline no destructivo.

---

## 🚀 1. Cómo abrir y probar el proyecto en Android Studio

1. Abre **Android Studio** (Hedgehog, Iguana, Ladybug o superior).
2. Selecciona **File -> Open...** y elige la carpeta raíz de este repositorio.
3. Deja que Gradle descargue las dependencias y sincronice el proyecto.
4. Para ejecutar el build de depuración localmente desde la terminal:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📦 2. Ubicación del APK local

Una vez compilado en tu máquina, el archivo APK generado se ubica en:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔄 3. Sincronización a GitHub desde Gemini Studio

1. En la esquina superior de **Gemini Studio**, abre el menú de opciones (**Settings / Export**).
2. Elige **Export to GitHub** (o descarga el proyecto como ZIP y súbelo a tu repositorio de GitHub).
3. Asegúrate de que la rama principal sea `main`.
4. Haz `git push` de los cambios.

---

## ⚡ 4. Cómo verificar GitHub Actions y Descargar el APK

1. Ve a tu repositorio en GitHub y haz clic en la pestaña **Actions**.
2. Verás el flujo **Android CI Build APK** en ejecución tras cada `push` a `main` (también puedes iniciarlo manualmente con el botón **Run workflow**).
3. Cuando el job finalice con éxito (indicador verde ✅), haz clic sobre la ejecución.
4. En la parte inferior de la página, en la sección **Artifacts**, encontrarás **`app-debug-apk`**. Haz clic para descargar el archivo ZIP con el APK listo para instalar.

---

## 🛠️ 5. Cambio entre Build Debug y Release

- **Debug (por defecto en CI):**
  ```bash
  ./gradlew assembleDebug
  ```
  Genera un APK autofirmado con las claves de desarrollo de Android, listo para instalar directamente en cualquier tablet o teléfono.

- **Release:**
  ```bash
  ./gradlew assembleRelease
  ```
  Genera el APK en `app/build/outputs/apk/release/app-release-unsigned.apk`. Para producción, configura tu `signingConfig` en `app/build.gradle.kts` con tu keystore.
