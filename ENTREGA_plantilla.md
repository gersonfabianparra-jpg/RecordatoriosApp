# Actividad 1 — Programación Mobile

**Estudiante:** _Nombre Apellido_
**Materia:** Programación Mobile
**Actividad:** N.º 1
**Repositorio:** _https://github.com/usuario/RecordatoriosApp_
**Fecha:** _dd/mm/aaaa_

> Guardar como `Apellido_Nombre_Actividad1_ProgramacionMobile.pdf`

---

## Consigna 1 — Configuración del proyecto
- Lenguaje: Kotlin 2.0.21 · UI: Jetpack Compose (BOM 2024.12.01) · Material 3
- Android Gradle Plugin 8.7.3 · Gradle 8.9 · JDK 17 (incluido en Android Studio)
- compileSdk / targetSdk 35 · minSdk 26 · applicationId `com.example.recordatorios`
- Dependencias declaradas en el catálogo `gradle/libs.versions.toml`

**Captura 1 — Estructura general del proyecto en Android Studio (consigna 1)**
_[Pegar captura del panel Project en vista "Android" con model/, data/, ui/ expandidos]_

## Consigna 2 — Modelado
- `Recordatorio` (data class, todo `val`): id, título, categoría, prioridad, estado y opcionales
  `descripcion`, `fechaLimite`, `ubicacion`, `aviso` (tipos anulables).
- `Prioridad` y `EstadoRecordatorio`: sealed classes (`Completado` y `Pospuesto` llevan fecha).
- `RecordatoriosUiState`: sealed interface (Cargando / Vacio / Exito / Error).
- Validaciones en `init`: título no vacío y ≤ 60 caracteres, id positivo, descripción ≤ 250,
  aviso solo con fecha límite, latitud/longitud en rango. `ValidadorRecordatorio.crear` devuelve
  `ResultadoValidacion.Valido` o `Invalido(errores)` sin lanzar excepciones.

_[Opcional: captura de Logcat con "Recordatorio rechazado: [El título no puede estar vacío]"]_

## Consigna 3 — Colecciones
- **List:** `DatosSimulados.recordatorios` (8 recordatorios).
- **Set:** `categoriasRecibidas.toSet()` reduce 8 entradas con repetidos a 5 categorías únicas.
- **Map:** `estilosPorCategoria: Map<Categoria, EstiloCategoria>` (etiqueta + color);
  `conteoPorEstado()` y `agrupadosPorCategoria()`.
- **Filtros:** `porEstado`, `porPrioridad`, `filtrar(estado, prioridad)`.

## Consigna 4 — Pantalla inicial con Jetpack Compose
**Captura 2 — Pantalla inicial (consigna 4)**
_[Pegar captura de la lista de recordatorios]_

## Consigna 5 — Ejecución
**Captura 3 — Aplicación ejecutándose en el emulador / dispositivo (consigna 5)**
_[Pegar captura de Android Studio con el emulador abierto mostrando la app, o foto del dispositivo]_

## Registro de problemas encontrados
| # | Etapa (sincronización / compilación / instalación / ejecución) | Problema / mensaje | Causa | Corrección | Verificado con nueva ejecución |
|---|---|---|---|---|---|
| 1 | | | | | ☐ |
| 2 | | | | | ☐ |
| 3 | | | | | ☐ |

_Completar solo con los problemas que realmente aparezcan. Si no hubo ninguno, indicarlo._
