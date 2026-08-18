# HyperEditor Pro (Native Android Photo Editor)

Editor fotográfico profesional nativo para Android desarrollado en **Kotlin** y **Jetpack Compose**. Diseñado con una arquitectura desacoplada, no destructiva, orientada a tablets y totalmente integrable con galerías externas como **Aves Gallery** a través de Intents de Android (`ACTION_EDIT` / `ACTION_SEND`).

---

## 🛠️ 1. Cómo abrir el proyecto

1. Descarga o clona este repositorio en tu equipo.
2. Abre **Android Studio** (versión Hedgehog 2023.1.1, Ladybug o superior).
3. Selecciona **File > Open...** y selecciona la carpeta raíz del proyecto.
4. Espera a que Gradle descargue las dependencias y sincronice el proyecto automáticamente con **JDK 17**.

---

## 💻 2. Cómo compilar localmente

Para compilar el APK de depuración desde la terminal en la raíz del proyecto:

```bash
# Dar permisos de ejecución si estás en Linux/macOS
chmod +x ./gradlew

# Compilar APK de depuración
./gradlew assembleDebug
```

Una vez finalizada la compilación, el archivo APK generado se encontrará en:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔄 3. Cómo sincronizar a GitHub

1. Inicializa y vincula tu repositorio remoto en GitHub si aún no lo has hecho:
   ```bash
   git init
   git add .
   git commit -m "HyperEditor Pro v1.0"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git
   git push -u origin main
   ```
2. Desde la interfaz de AI Studio, también puedes usar la opción **Settings / Export -> Export to GitHub**.

---

## ⚡ 4. Cómo descargar el APK desde GitHub Actions

1. Dirígete a la pestaña **Actions** en tu repositorio de GitHub.
2. El workflow **`Android CI Build APK`** se ejecutará automáticamente en cada `push` a la rama `main` o puedes lanzarlo manualmente con **Run workflow**.
3. Haz clic en la ejecución completada con éxito (icono verde ✅).
4. En la sección inferior **Artifacts**, haz clic en el artefacto **`app-debug-apk`** para descargar el archivo comprimido que contiene el APK listo para instalar en tu tablet o móvil Android.

---

## 🌟 5. Características del Pipeline de Edición No Destructiva

- **Canvas Viewport:** Gestos multitáctiles fluidos (zoom, pan, rotación libre, doble toque para zoom inteligente y reset de vista).
- **Ajustes Cromáticos Globales:** Brillo, contraste, saturación, exposición, temperatura y tinte calculados en `RenderPipeline`.
- **Geometría y Recorte:** Rotación 90° paso a paso, volteo horizontal/vertical, enderezado fino continuo (-45° a +45°) y recorte libre o con relaciones de aspecto fijas (1:1, 4:3, 16:9, 3:2, 9:16).
- **Filtros y Presets:** 8 filtros estilizados (B&N, Sepia, Vívido, Cine, Cálido, Frío, Dramático, Noir) con intensidad regulable (0% - 100%) y guardado de presets personalizados del usuario.
- **Historial Completo:** Pila atómica de 50 niveles para `Undo` / `Redo` que cubre todas las operaciones.
- **Gestión de Capas:** Capas de tinte cromático y duplicados de imagen con 6 modos de fusión (`Normal`, `Multiply`, `Screen`, `Overlay`, `Darken`, `Lighten`), control de opacidad individual y reordenamiento de capas.
- **Máscaras y Selecciones:** 4 tipos de selección (Rectangular, Elíptica, Lazo poligonal, Brocha/Borrador), inversión de máscara, suavizado perimetral (*feather* de 0 a 40px) y ajustes cromáticos localizados.
- **Retoque Creativo:** Pincel libre con paleta de colores, selector de grosor y opacidad, borrador de trazos, inserción de texto tipográfico editable multilínea (Sans, Serif, Monospace, Cursive) y tampón de clonación básico (*clone stamp*) con bordes difuminados.
- **Exportación de Alta Calidad:** Guardado de imagen final no destructiva directamente en la galería del sistema mediante `MediaStore` e inserción en el flujo de retorno de la galería llamante.
