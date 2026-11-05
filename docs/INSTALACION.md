# Instalación y recuperación

## Desde el APK

1. Descarga el APK de la sección **Releases** del repositorio o compílalo siguiendo el README.
2. Transfiérelo al TV Box por USB o ADB.
3. Ejecuta el instalador de Android y autoriza esa fuente si el sistema lo solicita.
4. Pulsa Home y selecciona CanoTV para probarlo o establecerlo como launcher predeterminado.

No es necesario desinstalar ni deshabilitar el launcher del fabricante.

## ADB

Con el dispositivo autorizado:

```bash
adb devices
adb install -r app-release.apk
```

Para instalar una compilación debug desde el código:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Volver al launcher del fabricante

- Abre Ajustes de Android → Aplicaciones → Aplicaciones predeterminadas → Aplicación de inicio (el nombre puede variar por firmware) y selecciona el launcher original.
- También puedes pulsar Home y elegir el launcher original cuando Android muestre el selector.
- No uses `pm disable`, root ni desinstalación del launcher original como parte de una instalación normal.

El botón de engranaje superior de CanoTV abre los ajustes del dispositivo. El acceso “Configuración” del menú lateral abre las preferencias propias de CanoTV.

## Actualizaciones

Android acepta una actualización solo si el APK conserva el mismo `applicationId` y está firmado con la misma clave que la instalación anterior. Guarda una copia segura del keystore. Si se pierde, una nueva firma no podrá actualizar esa instalación; habrá que desinstalarla y se perderán sus datos.
