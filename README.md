# RecordatoriosApp

Aplicación Android de recordatorios contextuales hecha con Kotlin, Jetpack Compose y Material 3.

- **Actividad 1** (etiqueta `actividad-1`): modelo de datos, validaciones y una pantalla con datos simulados.
- **Actividad 2** (etiqueta `actividad-2`, versión 0.2.0): listado, creación y detalle con Navigation Compose;
  ViewModel + UiState + StateFlow; repositorio en memoria detrás de una interfaz.
- **Actividad 3** (versión 0.3.0): persistencia local con **Room**, preferencias con **DataStore**,
  repositorio **local-first**, servicio remoto con **Retrofit + OkHttp**, sincronización controlada sin
  duplicados y funcionamiento **offline**.

## Requisitos
- Android Studio reciente (probado con JBR 25), Android SDK Platform 37 (compileSdk) y 36 (targetSdk).
- Un emulador API 26 o superior (probado en Pixel 8, API 36) o un teléfono con depuración USB.
- **Python 3** (solo biblioteca estándar) para el servidor simulado.

## 1. Configurar
La URL del servicio remoto **no está escrita en el código**: se lee al compilar y queda en `BuildConfig.API_BASE_URL`.

1. Copiar la línea de [`config.example.properties`](config.example.properties) en `local.properties`
   (ese archivo no se versiona):
   ```properties
   recordatorios.api.baseUrl=http://10.0.2.2:8080/
   ```
   - `10.0.2.2` es la computadora vista desde el emulador.
   - En un teléfono físico usar la IP de la computadora en la red Wi-Fi y agregarla en
     `app/src/main/res/xml/network_security_config.xml` (por defecto solo se permite HTTP hacia 10.0.2.2/localhost).
2. Si no se configura nada, se usa `http://10.0.2.2:8080/`.
   También se puede pasar en la línea de comandos: `./gradlew installDebug -Precordatorios.api.baseUrl=http://...`.

El servicio no usa credenciales ni tokens. Si en el futuro los necesitara, irían en `local.properties`
o en variables de entorno, nunca en el código ni en DataStore.

## 2. Iniciar el servidor simulado
```bash
cd servidor-simulado
python3 servidor.py            # http://0.0.0.0:8080/
```
Guarda los datos en `servidor-simulado/datos.json` (se crea con 3 recordatorios de ejemplo).

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/recordatorios` | 200 + lista |
| PUT | `/recordatorios/{id}` | 201 creado · 200 reemplazado · 422 datos inválidos · 400 JSON inválido |
| DELETE | `/recordatorios/{id}` | 204 eliminado · 404 no existe |
| GET | `/admin/estado` | contadores (creados, reemplazados, rechazados) — evidencia de que no hay duplicados |
| POST | `/admin/modo` `{"modo": "normal" \| "error500" \| "lento" \| "rechazar"}` | fuerza fallas para probar la app |
| POST | `/admin/reiniciar` | vuelve a los datos iniciales |

Ejemplo: `curl -X POST localhost:8080/admin/modo -d '{"modo":"error500"}'`.
`lento` demora 20 s cada respuesta: la app corta por timeout a los 8 s.

## 3. Compilar y ejecutar
Desde Android Studio: **Open** → carpeta `RecordatoriosApp` → esperar la sincronización de Gradle →
elegir el emulador → **Run ▶**.

Desde la terminal (con `JAVA_HOME` apuntando al JDK de Android Studio):
```bash
./gradlew assembleDebug                 # APK en app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                  # instala en el emulador/dispositivo conectado
./gradlew testDebugUnitTest             # 38 pruebas unitarias (JVM)
./gradlew connectedDebugAndroidTest     # 5 pruebas de Room en el emulador
```
En Logcat, filtrar por `tag:Recordatorios` para seguir la sincronización.

### Probar sin conexión
- Modo avión del emulador, o `adb shell svc wifi disable && adb shell svc data disable`.
- Crear, editar, completar o eliminar: los cambios aparecen al instante con «⏳ Pendiente de sincronizar».
- Cerrar la app por completo y volver a abrirla: los datos y las preferencias siguen ahí.
- Recuperar la red: la app sincroniza sola y cada registro pasa a «☁ Sincronizado».

## Arquitectura (Actividad 3)

```
 Compose (Screen)  --evento-->  ViewModel  --suspend-->  RepositorioRecordatoriosLocalFirst
      ^                            |                      |        |            |
      |  StateFlow<UiState>        |  Flow<Preferencias>  |        |            |
      +----------------------------+          (DataStore) |        |            |
                                                          v        v            v
                                                        Room    Retrofit    MonitorConectividad
                                                   (fuente de    + OkHttp
                                                     verdad)    (servidor)
```

- **Lecturas**: las pantallas solo observan Room (`Flow`) a través del ViewModel y el repositorio.
- **Escrituras**: primero en Room con estado `PENDIENTE_*`, luego se pide una sincronización.
- **Sincronización**: canal CONFLATED con un único consumidor + `Mutex` (nunca dos a la vez), `uuid`
  estable generado en el dispositivo + `PUT /recordatorios/{uuid}` idempotente, y confirmación condicionada
  a la `version` enviada. Reintento automático con espera exponencial ante 5xx/timeout.
- **Modelos separados** con mapeos explícitos en `data/mapeo/Mapeos.kt`:
  `RecordatorioDto` (servicio) ⇄ `RecordatorioEntity` (Room) → `Recordatorio` (dominio/UI).

## Estructura
```
app/src/main/java/com/example/recordatorios/
├── MainActivity.kt                  # tema desde DataStore + NavHost
├── ContenedorApp.kt                 # Application + contenedor: una sola DB, DataStore, Retrofit y repositorio
├── model/                           # dominio: Recordatorio, Prioridad, EstadoRecordatorio, EstadoSincronizacion…
├── data/
│   ├── RepositorioRecordatorios.kt          # contrato para los ViewModel + FiltroListado + estados de sync
│   ├── RepositorioRecordatoriosLocalFirst.kt# coordina Room, Retrofit y conectividad; sincronización
│   ├── local/        RecordatorioEntity · RecordatorioDao · RecordatoriosDatabase
│   ├── remoto/       RecordatorioDto · RecordatoriosApi · ClienteRemoto · ResultadoRemoto (códigos HTTP)
│   ├── mapeo/        Mapeos.kt (DTO ⇄ Entity → dominio)
│   ├── preferencias/ PreferenciasUsuario · RepositorioPreferencias (DataStore)
│   ├── red/          MonitorConectividad
│   └── DatosSimulados.kt · ConsultasRecordatorios.kt (catálogo de categorías y utilidades)
└── ui/
    ├── lista/ · crear/ (crear y editar) · detalle/ · ajustes/
    ├── navegacion/RecordatoriosNavHost.kt   # lista, crear, editar/{id}, detalle/{id}, ajustes
    └── components/ · theme/
app/schemas/          # esquema exportado de Room (versión 1)
servidor-simulado/    # servicio remoto de prueba (Python)
config.example.properties
```

## Rutas de navegación

| Ruta | Pantalla | Argumentos |
|---|---|---|
| `lista` (inicial) | Listado con filtros rápidos y estado de sincronización | — |
| `crear` | Formulario de creación | — |
| `editar/{recordatorioId}` | Mismo formulario, precargado | `Long` |
| `detalle/{recordatorioId}` | Detalle: editar, completar/reabrir, eliminar, reintentar sincronización | `Long` |
| `ajustes` | Preferencias (DataStore) | — |

Los problemas encontrados y su corrección están en [BITACORA.md](BITACORA.md).
