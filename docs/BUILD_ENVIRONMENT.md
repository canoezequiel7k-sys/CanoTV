# Entorno de compilación de CanoTV

## Versiones detectadas

- Sistema operativo: Ubuntu 24.04.2 LTS (Linux 6.14.0-37-generic)
- Java/JDK: Temurin OpenJDK 17.0.13+11 instalado localmente en `[LOCAL_HOME_REMOVED]/.local/java/jdk-17.0.13+11`
- Android SDK: `[LOCAL_HOME_REMOVED]/Android/Sdk`
- `adb`: `[LOCAL_HOME_REMOVED]/Android/Sdk/platform-tools/adb`
- `adb version`: Android Debug Bridge version 1.0.41, Version 37.0.1-15733141
- `platforms`: Android 25 y Android 34 instalados; Android 26/37.0 también disponibles por entorno
- `build-tools`: 36.0.0 y 34.0.0 instalados

## Versión base elegida para trabajar

- JDK: 17
- Gradle: 8.7
- Android Gradle Plugin: 8.6.1
- Kotlin: 1.9.24
- `compileSdk`: 34
- `targetSdk`: 25
- `minSdk`: 25

## Justificación de la elección

Se usa JDK 17 porque es compatible con AGP 8.x y con la versión moderna del plugin de Kotlin sin depender de un JDK demasiado reciente. La app debe apuntar a Android 7.1 (API 25) como mínimo y se valida con un `compileSdk` más moderno para mantener compatibilidad con herramientas actuales, sin elevar la versión mínima de la app.

## Comprobaciones realizadas

- `adb devices` se verificó con la instalación del entorno local.
- Se aceptaron las licencias del SDK Android y se instalaron las plataformas necesarias para compilar la app mínima.
- Se preparó la ruta de SDK local para que el proyecto pueda compilar desde terminal sin Android Studio.
