# Agudeza Visual para Android TV

Aplicación de pantalla completa para Android TV, Google TV, Fire TV y cajas
Android compatibles. Incluye dentro del instalador todas las cartillas,
recursos, menús y funciones de Agudeza Visual; no requiere internet.

## Compatibilidad

- Android 5.0 o superior.
- Controles con D-pad: arriba, abajo, izquierda, derecha, OK y Atrás.
- Android TV Stick, Google TV Streamer, Chromecast con Google TV, Fire TV y
  equipos compatibles que permitan instalar APK.

## Comportamiento del control

- Flechas: navegación por menús, distancias, pruebas e imágenes.
- OK, botón central, Play/Pausa o botón A: seleccionar o abrir.
- Canal/Página arriba y abajo: navegación vertical alternativa.
- Anterior/Siguiente multimedia: navegación horizontal alternativa.
- Atrás: regresar a la sección anterior.
- Atrás dos veces en menos de 1,2 segundos: cerrar la aplicación.

## Generar el APK en Android Studio

1. Instale Android Studio.
2. Abra la carpeta `AgudezaVisualAndroidTV`.
3. Espere a que termine la sincronización.
4. Seleccione **Build > Generate App Bundles or APKs > Generate APKs**.
5. El instalador se generará en `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar mediante USB

1. Copie `app-debug.apk` en una memoria USB.
2. Conecte la memoria al Android TV o dispositivo Google TV.
3. Active **Instalar aplicaciones desconocidas** para el explorador de archivos.
4. Abra el APK y seleccione **Instalar**.
5. Inicie **Agudeza Visual** desde el menú de aplicaciones.

El funcionamiento ordinario no requiere Wi-Fi, red local ni internet.

## Versión 2.2 y calibración clínica

El proyecto genera y firma dos instaladores de producción:

- `AgudezaVisual-Offline.apk`: funcionamiento normal.
- `AgudezaVisual-Kiosco.apk`: funcionamiento como dispositivo dedicado.

La versión más reciente se descarga desde la [página pública de
instalación](https://juansaldarriaga2003.github.io/AgudezaVisualAndroidTV/) o
desde [GitHub Releases](https://github.com/juansaldarriaga2003/AgudezaVisualAndroidTV/releases/latest).

La versión kiosco abre Agudeza Visual después de reiniciar, mantiene la pantalla
inmersiva y utiliza el modo Lock Task de Android cuando ha sido autorizada como
propietaria del dispositivo.

La pantalla se calibra con una regla física: ajuste la barra verde hasta que
mida exactamente 100 mm y seleccione la distancia real entre los ojos del
paciente y la pantalla. El sistema guarda la relación píxeles/milímetro y
calcula la altura del optotipo para que abarque cinco minutos de arco. Los
optotipos dinámicos usan anillos C de Landolt, cinco por línea y progresión de
0,1 logMAR.

Antes de calibrar, configure el televisor en `Ajustar a pantalla`, `Just Scan`,
`1:1` o `Sin overscan`, según el nombre usado por el fabricante. Desactive zoom,
contraste dinámico, ahorro de energía y cualquier cambio automático del formato
de imagen. Repita la calibración si cambia el TV, la resolución HDMI o el modo
de imagen.

Para abrir la salida administrativa mantenga presionado **OK** durante cuatro
segundos e ingrese el PIN inicial `2580`. El PIN debe cambiarse antes de una
implementación definitiva.

### Activación inicial del H96Max u otro Android TV

La activación como dispositivo dedicado debe hacerse en un equipo restablecido
de fábrica, antes de agregar cuentas:

```text
adb install AgudezaVisual-Kiosco.apk
adb shell dpm set-device-owner com.agudezavisual.tv.kiosk/com.agudezavisual.tv.KioskDeviceAdminReceiver
adb shell am start -n com.agudezavisual.tv.kiosk/com.agudezavisual.tv.MainActivity
```

Si `set-device-owner` informa que ya existen cuentas o usuarios configurados,
restablezca el dispositivo y repita la activación antes de iniciar sesión. Esta
autorización permite ocultar Inicio/Recientes, impedir la desinstalación normal y
usar Agudeza Visual como pantalla de inicio. Mantener OK durante cuatro segundos
y usar el PIN administrativo libera el modo kiosco.

La instalación mediante USB sí es suficiente para ejecutar la aplicación, pero
no concede por sí sola el control total del sistema. El comando ADB anterior se
ejecuta una sola vez desde un computador conectado a la misma red o por USB.

## Versión web

La misma interfaz de `webapp/` se incorpora al APK y se publica en la ruta
`/app/` del sitio. Por tanto, calibración, distancias, presentación clínica y
correcciones de imágenes se mantienen en una única fuente.

## Protección de la aplicación

Los instaladores de producción están firmados con una clave privada estable y
la compilación Release activa R8 y reducción de recursos. Android rechaza una
actualización alterada que no tenga la misma firma. Además, la edición kiosco
desactiva la depuración del WebView, bloquea acceso general a archivos y puede
impedir la desinstalación cuando es propietaria del dispositivo.

Ningún APK ni sitio web puede hacerse incopiable: alguien con control físico y
conocimientos técnicos puede extraer o reconstruir contenido. Estas medidas
protegen la instalación legítima y dificultan modificaciones casuales, pero no
sustituyen licencias, control contractual o gestión empresarial del dispositivo.

## Actualizaciones por USB

Cuando cambien cartillas, menús o funciones se genera un APK con un
`versionCode` superior y la misma firma digital. El nuevo archivo se instala
desde una memoria USB sobre la versión existente, sin desinstalarla. Android
conserva la calibración y las preferencias almacenadas.

No desinstale la versión anterior antes de actualizar. Si se desinstala, se
eliminan la calibración y las preferencias locales.

Para que Android acepte todas las actualizaciones, conserve de forma segura la
misma clave de firma utilizada para el primer APK de producción.
