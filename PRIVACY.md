# Declaración de Privacidad y Arquitectura de HyperEditor

HyperEditor no utiliza internet, no contiene publicidad, no utiliza inteligencia artificial, no recopila, transmite ni almacena telemetría, analítica de uso, identificadores publicitarios ni datos personales fuera del dispositivo, y no mantiene caché en disco. Todo el procesamiento de imagen ocurre de forma local, manual y en memoria.

## Principios Fundamentales del Sistema

### 1. Sin Publicidad (0 Ads)
- Cero SDKs de anuncios (AdMob, Meta Audience Network, Unity Ads, IronSource, AppLovin, etc.).
- Cero banners, intersticiales, videos recompensados o mediadores publicitarios.
- Remoción explícita del permiso de identificador de publicidad `com.google.android.gms.permission.AD_ID`.

### 2. Sin Internet (100% Offline)
- La aplicación funciona exclusivamente fuera de línea de manera autónoma.
- Ausencia y remoción expresa de permisos de red: `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`, `android.permission.ACCESS_WIFI_STATE`.
- Ninguna dependencia o herramienta se comunica con endpoints o servidores externos.

### 3. Sin Inteligencia Artificial (0 AI / 0 ML)
- Cero modelos de Machine Learning (TensorFlow Lite, ML Kit, MediaPipe, ONNX, PyTorch Mobile, servicios de IA generativa).
- Cero detección automática de rostros, sujetos o fondos por redes neuronales.
- Procesamiento 100% manual, determinista, basado en matrices afines, álgebra de píxeles, renders en canvas y shaders matemáticos estándar del framework Android.

### 4. Sin Telemetría ni Recolección de Datos
- Cero integración de Firebase Analytics, Crashlytics, Performance Monitoring o Google Analytics.
- Cero recolección de diagnósticos, métricas de sesión o identificadores de hardware.
- Desactivación forzada en manifiesto de cualquier señal analítica o de personalización.

### 5. Sin Caché en Disco (Procesamiento Estrictamente en Memoria RAM)
- Cero uso de `cacheDir` o `externalCacheDir` como almacén temporal de trabajo o búfer de edición.
- Todo el pipeline no destructivo (documento de edición, pila de deshacer/rehacer, capas, trazos y transformaciones) reside en memoria RAM durante la sesión activa.
- El único punto de escritura en almacenamiento persistente ocurre al exportar el resultado final de la imagen a `MediaStore` en el directorio público de imágenes del usuario (`Pictures/HyperEditor`). Al cerrar la sesión, guardando o descartando, no queda ningún rastro temporal en el disco.
