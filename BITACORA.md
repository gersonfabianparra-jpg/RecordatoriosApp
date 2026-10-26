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
