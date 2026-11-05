# RecordatoriosApp

Aplicación Android de recordatorios contextuales hecha con Kotlin, Jetpack Compose y Material 3.

- **Actividad 1** (etiqueta `actividad-1`): modelo de datos, validaciones y una pantalla con datos simulados.
- **Actividad 2** (versión 0.2.0): listado, creación y detalle conectados con Navigation Compose;
  ViewModel + UiState + StateFlow; repositorio en memoria detrás de una interfaz.

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

## Arquitectura (Actividad 2)

```
Pantalla (@Composable)  --eventos (lambdas)-->  ViewModel  --suspend / Flow-->  RepositorioRecordatorios (interfaz)
        ^                                          |                                   |
        +---- StateFlow<UiState> (collectAsStateWithLifecycle)                 RepositorioRecordatoriosEnMemoria
```

- Cada pantalla tiene un `...Route` (obtiene el ViewModel y recolecta el estado) y un `...Screen` sin estado
  propio, que recibe el `UiState` y lambdas. Así las pantallas se pueden previsualizar y probar sin ViewModel.
- Cada ViewModel guarda un `MutableStateFlow` privado y expone un `StateFlow` de solo lectura; los cambios
  se hacen con `update { }`, que produce una instancia nueva e inmutable.
- `ListaUiState` y `DetalleUiState` son `sealed interface` (Cargando, Vacío / No encontrado, Con datos, Error),
  y la UI los recorre con un `when` exhaustivo. `FormularioUiState` es una `data class` con los campos,
  los errores visibles y un `EstadoEnvio` sellado (Inactivo, Enviando, Éxito, Error).
- Los ViewModels dependen de las interfaces `RepositorioRecordatorios` y `ControlSimulacion`; la
  implementación en memoria se crea una sola vez en `ContenedorApp` (dentro de `RecordatoriosApplication`).
- Menú **Simular** del listado: Datos normales, Lista vacía, Error al cargar y Error al guardar.

## Rutas de navegación

| Ruta | Pantalla | Argumentos | Cómo se llega | Cómo se vuelve |
|---|---|---|---|---|
| `lista` (inicial) | Listado | — | Al abrir la app | Atrás sale de la app |
| `crear` | Formulario de creación | — | `onCrearRecordatorio` (botón Nuevo o estado vacío) | `popBackStack()` al guardar o cancelar |
| `detalle/{recordatorioId}` | Detalle | `recordatorioId: Long` (`NavType.LongType`) | `onRecordatorioClick(id)` | `onVolver` → `popBackStack()` |

## Estructura
```
app/src/main/java/com/example/recordatorios/
├── MainActivity.kt                  # Punto de entrada: tema + NavHost
├── ContenedorApp.kt                 # Application y contenedor de dependencias
├── model/                           # Consigna 2
│   ├── Recordatorio.kt              # data class inmutable + Ubicacion, Aviso, validador
│   ├── Categoria.kt                 # data class Categoria + EstiloCategoria
│   ├── Prioridad.kt                 # sealed class Alta / Media / Baja
│   ├── EstadoRecordatorio.kt        # sealed class Pendiente / Completado / Pospuesto / Vencido
│   └── UiState.kt                   # sealed interface de estados de pantalla
├── data/
│   ├── DatosSimulados.kt            # List de recordatorios, Set de categorías, Map categoría→estilo
│   ├── RepositorioRecordatorios.kt  # interfaz, BorradorRecordatorio, ModoSimulacion, ControlSimulacion
│   ├── RepositorioRecordatoriosEnMemoria.kt
│   └── ConsultasRecordatorios.kt    # filtros, groupBy y conteos (extensiones de List)
└── ui/
    ├── lista/      ListaUiState · ListaViewModel · ListaScreen
    ├── crear/      FormularioUiState · FormularioViewModel · CrearRecordatorioScreen
    ├── detalle/    DetalleUiState · DetalleViewModel · DetalleScreen
    ├── navegacion/ RecordatoriosNavHost (Rutas + NavHost)
    ├── components/ RecordatorioCard · EstadosContenido · CamposFormulario · FilaDato · EtiquetasRecordatorio
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
