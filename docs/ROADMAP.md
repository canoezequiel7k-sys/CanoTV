# Estado y roadmap

## Estado

- **Fases 0–6:** funciones del launcher, navegación, aplicaciones, ajustes y personalización implementados y validados en el dispositivo de referencia.
- **Fase 7 — Estabilidad:** cerrada después de pruebas de D-pad, instalación/actualización, listas pequeñas y grandes, retorno de apps, recuperación del launcher original y conexión/desconexión de red.
- **Fase 8 — Entrega:** versión 1.0.0, documentación de instalación/arquitectura, verificación de privacidad, firma local de release y compilación automatizada.

## Distribución

La APK se puede instalar manualmente en dispositivos compatibles. La prueba en un único TV Box no equivale a certificar todos los modelos. Una publicación futura en una tienda requiere una decisión separada sobre target SDK, políticas, licencia y soporte de dispositivos.

## Criterios para futuras versiones

1. Reproducir cambios de interfaz en un Android TV Box real y conservar foco por D-pad.
2. No incluir IPs, credenciales, rutas de desarrollo ni keystores en cambios públicos.
3. Ejecutar `./gradlew testDebugUnitTest assembleDebug` antes de distribuir.
4. Incrementar `versionCode` en cada APK que deba actualizar una versión ya instalada.
5. Firmar todas las actualizaciones distribuidas con la misma clave privada.
