# Bitácora de problemas — Actividad 1

Entorno: macOS 26 (Apple A18 Pro, 8 GB RAM) · Android Studio (Rabbit 1) con JBR 25 · Gradle 9.8.2 · AGP 9.4.1 · Kotlin 2.4.21 · Emulador Pixel 8, API 36 (arm64).

| # | Etapa | Problema / mensaje | Causa | Corrección | Verificación |
|---|---|---|---|---|---|
| 1 | Entorno | `Unable to locate a Java Runtime`; no existía `~/Library/Android/sdk` | Equipo sin Android Studio, JDK ni SDK | Instalación de Android Studio y de las herramientas de línea de comandos; SDK Platform 36, Build-Tools 36.1.0, Platform-Tools, Emulator e imagen `android-36;google_apis;arm64-v8a`; aceptación de licencias | `java -version` responde (OpenJDK 25.0.3) y el SDK queda instalado |
| 2 | Sincronización | Configuración inicial con Gradle 8.9 / AGP 8.7.3, incompatible con el JDK 25 que trae Android Studio | Gradle 8.9 no admite ejecutarse sobre Java 25 | Actualización a Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21 y Compose BOM 2026.09.00. Con AGP 9 se quita el plugin `org.jetbrains.kotlin.android` (Kotlin integrado) y el bloque `kotlinOptions` | `./gradlew wrapper` y la configuración del proyecto terminan con `BUILD SUCCESSFUL` |
| 3 | Compilación | `checkDebugAarMetadata FAILED` — *Dependency 'androidx.compose.animation:animation-core-android:1.12.1' requires … compile against version 37 or later* (14 avisos iguales) | Compose BOM 2026.09.00 exige compileSdk 37 y el proyecto usaba 36 | Instalación de `platforms;android-37.0` y `compileSdk = 37` (targetSdk se mantiene en 36) | `./gradlew assembleDebug testDebugUnitTest` → `BUILD SUCCESSFUL`, APK generado, 7/7 pruebas OK |
| 4 | Ejecución | La app se cierra al abrir: `FATAL EXCEPTION: main … NullPointerException: Attempt to invoke virtual method 'int …Prioridad.getNivel()' on a null object reference at RecordatoriosScreen.kt:47` | Ciclo de inicialización: `MainActivity` usa primero `Prioridad.Alta`; eso inicializa la clase padre `Prioridad`, cuyo `companion` crea `listOf(Alta, Media, Baja)` cuando `Alta` todavía es `null` | `val todas by lazy { listOf(Alta, Media, Baja) }` en `Prioridad.kt` | Reinstalación y nuevo arranque: sin `FATAL` en Logcat, el proceso sigue vivo y la lista se muestra |
| 5 | Ejecución (emulador) | Diálogo del sistema «System UI isn't responding» sobre la app | Emulador con 2 GB de RAM en el primer arranque: la interfaz del sistema no da abasto (no es un error de la app) | `hw.ramSize=3072` en el AVD y arranque en frío (`-no-snapshot`) | Nuevo arranque sin el diálogo; captura limpia de la pantalla inicial |
| 6 | Herramientas | `brew install gradle` quedó bloqueado esperando un lock de descarga (además intentaba instalar otro JDK) | Proceso de Homebrew colgado | Se canceló y se usó la distribución oficial `gradle-9.8.2-bin.zip` (SHA-256 verificado) solo para generar el wrapper | `gradlew` creado; desde entonces todo se compila con `./gradlew` |
| 7 | Ejecución (emulador desde Android Studio) | Al pulsar Run, el emulador se cierra a los ~4 min: `detected a hanging thread 'QEMU2 main loop'. No response for 15005 ms` y luego `Emulator terminated with exit code 139` (SIGSEGV) | El AVD tenía `hw.gpu.enabled=no`: la pantalla se dibujaba por software (SwiftShader) con la CPU y, con Android Studio abierto en 8 GB de RAM, los hilos del emulador se bloquearon | `hw.gpu.enabled=yes` y `hw.gpu.mode=host` en el AVD (aceleración gráfica por hardware) | Nuevo Run desde Android Studio: el log muestra `OpenGL ES Translator (Apple A18 Pro)`, la app se instala y se muestra en Running Devices sin cierres ni `FATAL` en Logcat |

## Evidencia en Logcat (filtro `tag:Recordatorios`)
```
D Recordatorios: Recordatorios cargados: 8
D Recordatorios: Categorías únicas (Set): [Trabajo, Salud, Hogar, Estudio, Compras]
D Recordatorios: Conteo por estado (Map): {Pendiente=4, Vencido=1, Completado=2, Pospuesto=1}
D Recordatorios: Prioridad alta: [Entregar informe trimestral, Turno con el médico]
W Recordatorios: Recordatorio rechazado: [El título no puede estar vacío]
```

# Bitácora — Actividad 3 (persistencia, red y sincronización)

| # | Etapa | Problema / mensaje | Causa | Corrección | Verificación |
|---|---|---|---|---|---|
| 8 | Pruebas JVM | Las pruebas del repositorio quedaban en estado `Inactiva`: la sincronización nunca corría | Las corrutinas lanzadas en `backgroundScope` no cuentan para `advanceUntilIdle()` | El repositorio de prueba usa un alcance propio sobre el mismo planificador virtual, que se cancela al terminar cada prueba | 9/9 pruebas del repositorio aprobadas |
| 9 | Pruebas JVM | `OutOfMemoryError` en `okhttp3.internal.idn.IdnaMappingTable.map` | Los atajos `Response.success/error` de Retrofit arman una respuesta con host `localhost`; en la JVM de pruebas, el mapeo IDN de la variante Android de OkHttp 5 no termina | El servidor falso arma las respuestas a mano con host `127.0.0.1` | Pruebas completas en ~5 s |
| 10 | Pruebas instrumentadas | `NoSuchMethodError: runBlockingK$default` al ejecutar en el emulador | `kotlinx-coroutines-test` 1.11 no coincide con la versión de corrutinas empaquetada en el APK | Las pruebas de Room usan `runBlocking` y se quitó esa dependencia de `androidTest` | 5/5 pruebas de Room aprobadas en el emulador |
| 11 | Interfaz | El listado quedaba en blanco: la fila de filtros ocupaba toda la pantalla | `VerticalDivider` dentro de una fila con `horizontalScroll` toma toda la altura disponible | Altura fija de 28 dp para el divisor | Captura 01: filtros arriba y tarjetas visibles |
| 12 | Conectividad | Con modo avión el banner seguía en «Sincronizado» | En `onLost` se consultaba `activeNetwork`, que todavía devolvía la red perdida | `onLost` emite `false` directamente | Captura 04: «📴 Sin conexión» |
| 13 | Red | En cada sincronización la primera petición funcionaba y la segunda fallaba como «sin conexión» | Con `retryOnConnectionFailure(false)` OkHttp no se recupera de una conexión del pool que el servidor HTTP/1.0 ya cerró | `retryOnConnectionFailure(true)`; los reintentos lógicos (5xx, timeout, sin red) siguen en el repositorio | Log: PUT, PUT, DELETE y GET en una misma sincronización sin errores |
| 14 | Emulador | `connectedDebugAndroidTest`: «0 of which were compatible» y `hanging thread 'QEMU2 CPU'` | Compilación con Gradle y emulador a la vez en 8 GB de RAM | Reintentar con el emulador ya estable | Ejecución siguiente: 5/5 |

## Evidencia de sincronización sin duplicados (servidor simulado)
```
PUT /recordatorios/68a8853b-…  201   ← creado sin conexión, enviado al volver la red
PUT /recordatorios/…0002        200   ← modificación hecha sin conexión
DELETE /recordatorios/…0003     204   ← eliminación hecha sin conexión
*** Modo cambiado a: lento
PUT /recordatorios/bcd0c7c3-…   201   ← el servidor lo guardó, pero la app ya había cortado por timeout
*** Modo cambiado a: normal
PUT /recordatorios/bcd0c7c3-…   200   ← reintento: reemplaza, no duplica
estado final: total 4 · PUT_creados 2 · PUT_reemplazados 3 · PUT_rechazados 1 · DELETE 1
```
