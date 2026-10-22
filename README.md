# Frenapps
Fork libre y mantenido de **Appzuku** ("Process Warden"), la app que frena procesos en segundo plano con Root o Shizuku.

![Logo](https://github.com/northmendo/Appzuku/blob/f58b2f820655d7ca59ee699159965b110e23f1d5/docs/images/logo.png)
<p align="center">
  <img src="https://img.shields.io/github/v/release/HUEVOMAN77/frenapps?include_prereleases&label=Version"/>
  <img src="https://img.shields.io/badge/Idioma-Espa%C3%B1ol%20(latino)-blue.svg"/>
  <img src="https://img.shields.io/badge/License-GPLv3-blue.svg"/>
</p>


> [!IMPORTANT]
> **Este es un fork mantenido de [northmendo/Appzuku](https://github.com/northmendo/Appzuku), bajo GPL-3.0.**
>
> El autor original dejó Appzuku en marzo de 2026 para enfocar el desarrollo en
> [Process Warden](https://github.com/northmendo/ProcessWarden), que es una app paga y sin
> código fuente publicado. Acá el código sigue libre: se conserva el historial completo de
> commits y todo el crédito del autor original.
>
> Este fork no está afiliado ni avalado por northmendo, y no usa el nombre "Process Warden".

**Qué se hizo en el fork**

- **Estabilidad:** build de release reproducible y firmado con una clave propia del fork,
  reglas de ProGuard para Room/Shizuku, y crashes conocidos tapados (lista que se actualiza
  después de cerrar la pantalla, `sleep` dentro del servicio, aviso de permiso).
- **Seguridad:** los nombres de paquete se validan antes de armar cualquier comando de shell
  (`am force-stop`, `pm uninstall`, `cmd appops`), así un texto cualquiera no puede colarse
  en un comando con permisos de Root o Shizuku.
- **Arranque automático:** el `BootReceiver` estaba declarado como `exported="false"` y el
  sistema nunca podía entrar; ahora el permiso de encendido sí vuelve a activar la
  automatización después de reiniciar.
- **Idioma:** toda la interfaz está en **español latinoamericano** por defecto, con el inglés
  conservado en `values-en/`.
- **Primer uso:** una **introducción paso a paso** antes de entrar a la lista, y un aviso
  naranja en la pantalla principal cuando falta el permiso de Root o Shizuku (antes la lista
  simplemente salía vacía y no se entendía por qué).


---

> [!NOTE]
> **Aviso del repositorio original (2026):** Appzuku fue reemplazado por Process Warden, una
> aplicación paga con prueba gratuita de 3 días. El desarrollo futuro del proyecto original
> es exclusivo de Process Warden. Appzuku queda libre en GitHub, y este fork lo mantiene.


## ¿Qué es Frenapps?
Frenapps es una app de Android que detiene apps en segundo plano usando permisos de Shizuku o
de Root. Permite cierres manuales de un toque, limpieza programada y restricción de segundo
plano para apps elegidas. El objetivo es tener control directo sobre las apps que gastan RAM,
se relanzan todo el tiempo o despiertan cuando no deberían.

## Funciones
- **Permisos flexibles**: funciona con acceso Root o con Shizuku.
- **Cierre manual**: cerrá apps seleccionadas desde la pantalla principal, la app en primer
  plano desde los Ajustes rápidos, o ejecutá tu cierre en segundo plano configurado desde el
  otro tile.
- **Automatización**: auto-cierre periódico, cierre al bloquear la pantalla y umbral
  inteligente de RAM.
- **Modos de objetivo**: lista blanca para proteger apps de los cierres, o lista negra para
  atacar solo las apps elegidas.
- **Restricción en segundo plano (Android 11+)**: mantiene dormidas las apps elegidas con
  restricción basada en AppOps.
- **Reparación y registro de restricciones**: volver a aplicar las restricciones guardadas y
  ver resultados/errores recientes desde Ajustes.
- **Historial de cierres y apps más cerradas**: qué apps se cerraron, cuáles vuelven a abrirse
  y cuánta RAM se libera.
- **Copia de seguridad y restauración**: guardar y recuperar las listas de apps configuradas.
- **Búsqueda y filtro**: encontrar rápido apps en la lista por nombre o ID de paquete.
- **Temas**: claro, oscuro y el del sistema.
- **Apps protegidas**: las críticas del sistema y las blanqueadas no se pueden cerrar.
- **Monitor de RAM**: muestra el uso de RAM del sistema en tiempo real.
- **Intro y ayuda en español**: introducción paso a paso el primer día y ayuda completa
  siempre disponible desde Ajustes → Ayuda.

## Capturas
<p align="center">
  <img src="https://github.com/northmendo/Appzuku/blob/main/docs/images/screenshot0.jpg" width="30%">
  <img src="https://github.com/northmendo/Appzuku/blob/main/docs/images/screenshot1.jpg" width="30%">
  <img src="https://github.com/northmendo/Appzuku/blob/main/docs/images/screenshot2.jpg" width="30%">
</p>

## Requisitos
- **Versión de Android**: 6.0 (SDK 23) o superior.
- **Shizuku o Root**: Frenapps necesita acceso Root o la app Shizuku corriendo.
- **Android 11+ para la restricción en segundo plano**: se instala en Android 6+, pero esa
  función requiere Android 11 o más nuevo.

## Instalación
Descargá el APK más nuevo desde la
[pestaña de Releases de este fork](https://github.com/HUEVOMAN77/frenapps/releases) y
instálalo directamente (activá "instalar apps desconocidas" si el teléfono lo pide).

Como el fork usa una clave de firma propia, si tenés el Appzuku original instalado hay que
desinstalarlo antes: las firmas no coinciden.

## Cómo se usa (los mismos pasos de la intro)
1. **Dale permiso**: Root (aceptá el pedido de Superuser) o Shizuku (iniciá su servicio y
   aceptá el pedido de permiso). Sin esto la lista sale vacía y aparece el aviso naranja.
2. **Pantalla principal**: tocá las apps a cerrar y apretá el botón rojo. Deslizá para
   volver a escanear.
3. **Listas**: por defecto rige la lista blanca (cierra todo menos lo que vos agregues). En
   Ajustes → Modo podés pasar a lista negra.
4. **Automatización**: prendé *Servicio en segundo plano* y elegí auto-cierre periódico,
   cierre al bloquear o umbral de RAM.
5. **Sin abrir la app**: tiles de Ajustes rápidos, acceso directo y widget.
6. **Cuidado**: no cierres ni restrinjas apps del sistema si no sabés qué hacen.

## Compilar el proyecto
Se probó con JDK 17, Android SDK 36 y Gradle 8.11.1:

```bash
export JAVA_HOME=/ruta/al/jdk-17
export ANDROID_HOME=/ruta/AL/android-sdk
gradle assembleDebug     # APK de prueba, sin firmar
gradle assembleRelease   # APK firmado si existe keystore/frenapps.properties
```

La clave de firma del fork **no está en el repositorio** (está ignorada en `keystore/`). Para
firmar vos mismo, generá un keystore y creá `keystore/frenapps.properties` con
`storeFile`, `storePassword`, `keyAlias` y `keyPassword`. Sin ese archivo, el release se firma
con la clave de debug.

## Licencia
Frenapps está licenciado bajo la [GNU General Public License v3.0](LICENSE), igual que el
proyecto original.

## Donaciones
Si querés apoyar el desarrollo, se agradece.

[**PayPal (autor original)**](https://www.paypal.com/donate/?hosted_button_id=DDJRFUXHSHRVN)

## Créditos
- El proyecto original y todo su trabajo son de [northmendo/Appzuku](https://github.com/northmendo/Appzuku),
  que a su vez derivó de [YasserNull/shappky](https://github.com/YasserNull/shappky).
- El mantenimiento, la traducción y las correcciones de este fork las hace
  [HUEVOMAN77](https://github.com/HUEVOMAN77/frenapps).
