# Pruebas y compatibilidad

## Dispositivo de referencia

Las comprobaciones de interfaz y navegación se hicieron en un TV Box MXQ Pro, Android API 25, resolución 1280 × 720 y control remoto D-pad.

Se validó:

- Selección de CanoTV como launcher y recuperación del launcher del fabricante.
- Navegación con D-pad por Inicio, cuadrícula y Ajustes.
- Apertura de apps y retorno a CanoTV.
- Actualización de la lista al instalar y desinstalar apps.
- Cuadrículas con pocos elementos y con más de dos filas.
- Comportamiento ante el cierre inesperado de una app externa.
- Cambio de tema/idioma, preferencias persistentes y estado de Wi‑Fi.
- Desconexión temporal de Wi‑Fi y reconexión por el usuario.

El proyecto no tiene pruebas unitarias automatizadas definidas todavía; `testDebugUnitTest` informa `NO-SOURCE`. Las validaciones en hardware son manuales y no garantizan el comportamiento en otros firmware o dispositivos.

## Límites conocidos

- CanoTV solo inicia aplicaciones externas; no controla la interfaz, certificación o DRM de esas apps.
- Netflix y otros servicios pueden requerir hardware, firmware o certificación que un TV Box no tenga.
- Los fabricantes pueden cambiar nombres de menús, accesibilidad D-pad e intents del sistema.
- El ajuste de idioma cubre los textos propios de CanoTV; nombres de apps provienen de cada aplicación instalada.

Al informar un problema, incluye modelo general, API de Android, resolución, pasos para reproducir y captura sin datos de red, cuenta o número de serie.
