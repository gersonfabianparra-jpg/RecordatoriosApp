# RecordatoriosApp — Módulo 1

Aplicación Android de recordatorios contextuales (primera etapa): modelo de datos en Kotlin,
validaciones y una pantalla con Jetpack Compose + Material 3 que muestra datos simulados.

## Requisitos
- Android Studio reciente (probado con Rabbit 1), que trae su propio JDK.
- Android SDK Platform 37 (compileSdk) y 36 (targetSdk), desde **SDK Manager**.
- Un emulador (API 26 o superior) creado en **Device Manager**, o un teléfono con depuración USB.

## Cómo ejecutar
1. Android Studio → **Open** → seleccionar la carpeta `RecordatoriosApp`.
2. Esperar la **sincronización de Gradle** (descarga Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00).
   Si pide instalar el SDK 37 o aceptar licencias, aceptar.
3. Device Manager → **Create Virtual Device** → Pixel 8 → imagen API 36 → Finish.
4. Seleccionar el emulador y pulsar **Run ▶** (`Shift+F10` / `Ctrl+R` en Mac).
5. En **Logcat**, filtrar por `tag:Recordatorios` para ver las colecciones y la validación rechazada.
6. Pruebas unitarias: clic derecho en `app/src/test` → **Run 'Tests in recordatorios'**.

## Estructura
```
app/src/main/java/com/example/recordatorios/
├── MainActivity.kt                  # Punto de entrada + diagnóstico en Logcat
├── model/                           # Consigna 2
│   ├── Recordatorio.kt              # data class inmutable + Ubicacion, Aviso, validador
│   ├── Categoria.kt                 # data class Categoria + EstiloCategoria
│   ├── Prioridad.kt                 # sealed class Alta / Media / Baja
│   ├── EstadoRecordatorio.kt        # sealed class Pendiente / Completado / Pospuesto / Vencido
│   └── UiState.kt                   # sealed interface de estados de pantalla
├── data/                            # Consigna 3
│   ├── DatosSimulados.kt            # List de recordatorios, Set de categorías, Map categoría→estilo
│   └── RepositorioRecordatorios.kt  # filtros por estado/prioridad, groupBy, conteos
└── ui/                              # Consigna 4
    ├── RecordatoriosScreen.kt       # Scaffold, TopAppBar, FilterChips, LazyColumn
    ├── components/RecordatorioCard.kt
    └── theme/Theme.kt               # Material 3, claro/oscuro
```

## Correspondencia con las consignas
| Consigna | Dónde |
|---|---|
| 1. Proyecto Kotlin + Compose, SDK, Gradle | `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts` (compileSdk 37, targetSdk 36, minSdk 26) |
| 2. data classes inmutables y sealed classes | `model/` — todo `val`; `init { require(...) }` impide estados inválidos (también en `copy`) |
| 3. List, Set, Map | `DatosSimulados` (`toSet()` elimina categorías repetidas; `Map<Categoria, EstiloCategoria>`), `RepositorioRecordatorios` |
| 4. Pantalla Compose + Material 3 | `ui/RecordatoriosScreen.kt` — título, categoría, prioridad y estado por tarjeta |
| 5. Ejecución | Emulador Pixel 8 API 36 (ver capturas en el PDF) |

Los problemas encontrados y su corrección están en [BITACORA.md](BITACORA.md).
