# Entorno de compilación

## Requisitos

- JDK 17.
- Gradle Wrapper incluido (Gradle 8.7).
- Android Gradle Plugin 8.6.1 y Kotlin 1.9.24, fijados en el proyecto.
- Android SDK Platform 34 y Build Tools compatibles.
- Conexión a Internet durante la primera compilación para obtener el wrapper y las dependencias.

Android Studio puede instalar y configurar el SDK; también se puede compilar desde terminal. El SDK local se configura con `local.properties` o `ANDROID_HOME`/`ANDROID_SDK_ROOT`. `local.properties` no se versiona.

## Comandos

```bash
./gradlew testDebugUnitTest assembleDebug
./gradlew assembleRelease
```

La variante debug se puede instalar para pruebas. La variante release se firma solo cuando existen las cuatro propiedades `canotvRelease*` en la configuración Gradle local; consulta el README para crear y configurar un keystore privado.

## Compatibilidad declarada

| Ajuste | Valor |
|---|---:|
| `minSdk` | 25 (Android 7.1) |
| `targetSdk` | 25 |
| `compileSdk` | 34 |
| `versionCode` / `versionName` | 2 / 1.0.0 |

El firmware de algunos TV Box informa una versión comercial distinta de la API real. Comprueba el nivel API del dispositivo antes de instalar. CanoTV mantiene `targetSdk 25` para preservar el comportamiento en el TV Box de referencia y se distribuye por APK/ADB; no está preparado para publicarse en Google Play, que requiere un target más reciente.
