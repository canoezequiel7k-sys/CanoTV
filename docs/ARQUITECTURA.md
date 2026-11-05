# Arquitectura de CanoTV

CanoTV es una aplicación Android de un solo módulo. La interfaz usa Activities, Views/XML y un `RecyclerView`; las preferencias simples se guardan en `SharedPreferences`. No hay servidor ni base de datos remota.

## Código Kotlin

| Archivo | Responsabilidad |
|---|---|
| `AppEntry.kt` | Modelo de una actividad externa que se puede iniciar. |
| `AppRepository.kt` | Consulta actividades `LEANBACK_LAUNCHER` y `LAUNCHER`, evita duplicados y ordena resultados. |
| `AppAdapter.kt` | Dibuja tarjetas, iconos/banners, favoritos, estilos y estado de foco. |
| `FavoritesStore.kt` | Guarda favoritos por paquete/actividad y limpia los que ya no están instalados. |
| `SettingsStore.kt` | Persiste tema, idioma, familia tipográfica, tamaño de texto y orden de favoritos. |
| `LocaleContext.kt` | Aplica el idioma elegido al contexto de recursos de cada pantalla. |
| `ThemePalette.kt` | Centraliza colores, fondos de tarjetas y estados enfocados por tema. |
| `HomeActivity.kt` | Coordina Inicio, Favoritos, Todas las apps, foco D-pad, red, recomendaciones y apertura. |
| `SettingsActivity.kt` | Presenta los selectores y persiste las preferencias. |
| `AboutActivity.kt` | Consulta y muestra información que Android reporta del dispositivo. |

Las clases binding `Activity*Binding` e `ItemAppBinding` se generan desde View Binding; no se editan manualmente.

## Recursos Android

| Ruta | Contenido |
|---|---|
| `res/layout/` | Pantallas Inicio, Ajustes, Acerca de y plantilla de tarjeta. |
| `res/drawable/` | Iconos vectoriales de navegación, red y acciones. |
| `res/drawable-nodpi/` | Icono CanoTV y fondos optimizados para 1280 × 720. |
| `res/values/strings.xml` | Textos en español, idioma predeterminado. |
| `res/values-en/strings.xml` | Textos en inglés. |
| `res/values/themes.xml` | Tema base de Android Views/Material. |
| `res/values/dimens.xml` | Medidas compartidas. |
| `res/anim/` | Transiciones ligeras entre pantallas. |

## Configuración del proyecto

- `settings.gradle.kts`: módulos y repositorios Gradle.
- `build.gradle.kts`: versiones de Android Gradle Plugin y Kotlin.
- `app/build.gradle.kts`: SDK, versiones, dependencias y firma release opcional.
- `gradle/wrapper/`: versión de Gradle fijada para todos los entornos.
- `app/src/main/AndroidManifest.xml`: launcher HOME, Activities y permisos mínimos.

La firma release se configura con propiedades Gradle locales; los secretos no se guardan en el repositorio. Revisa [Instalación y recuperación](INSTALACION.md) y el [README](../README.md) para compilar e instalar.
