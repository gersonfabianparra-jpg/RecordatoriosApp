package com.example.recordatorios.data.mapeo

import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.local.SyncEstado
import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.data.remoto.UbicacionDto
import com.example.recordatorios.model.Aviso
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstadoSincronizacion
import com.example.recordatorios.model.OperacionPendiente
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.model.Ubicacion
import com.example.recordatorios.model.ValidadorRecordatorio
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/*
 * Mapeos explícitos entre las tres representaciones:
 *
 *   RecordatorioDto (contrato del servicio)  <-->  RecordatorioEntity (esquema de Room)  -->  Recordatorio (dominio / UI)
 *
 * El DTO nunca se convierte directamente en modelo de dominio: primero se valida y se guarda
 * en Room, y la interfaz lo recibe desde Room. Así Room es siempre la fuente de verdad.
 */

// ---------- Fechas ----------

private val zona: ZoneId get() = ZoneId.systemDefault()

fun LocalDateTime.aEpochMillis(): Long = atZone(zona).toInstant().toEpochMilli()

fun Long.aLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zona)

private fun String.aEpochMillisOrNull(): Long? = try {
    LocalDateTime.parse(this).aEpochMillis()
} catch (_: DateTimeParseException) {
    null
}

private fun Long.aIso(): String = aLocalDateTime().toString()

// ---------- Prioridad y estado ----------

fun Prioridad.aNivel(): Int = nivel

fun prioridadDesdeNivel(nivel: Int): Prioridad? = Prioridad.todas.firstOrNull { it.nivel == nivel }

private fun Prioridad.aCodigoRemoto(): String = when (this) {
    Prioridad.Alta -> "ALTA"
    Prioridad.Media -> "MEDIA"
    Prioridad.Baja -> "BAJA"
}

private fun prioridadDesdeCodigoRemoto(codigo: String): Prioridad? = when (codigo.uppercase()) {
    "ALTA" -> Prioridad.Alta
    "MEDIA" -> Prioridad.Media
    "BAJA" -> Prioridad.Baja
    else -> null
}

private val ESTADOS_VALIDOS = setOf("PENDIENTE", "COMPLETADO", "POSPUESTO", "VENCIDO")

/** Estado de dominio → (código, fecha asociada) para Room. */
fun EstadoRecordatorio.aColumnas(): Pair<String, Long?> = when (this) {
    EstadoRecordatorio.Pendiente -> "PENDIENTE" to null
    EstadoRecordatorio.Vencido -> "VENCIDO" to null
    is EstadoRecordatorio.Completado -> "COMPLETADO" to completadoEn.aEpochMillis()
    is EstadoRecordatorio.Pospuesto -> "POSPUESTO" to hasta.aEpochMillis()
}

private fun estadoDesdeColumnas(codigo: String, fecha: Long?, respaldo: Long): EstadoRecordatorio = when (codigo) {
    "COMPLETADO" -> EstadoRecordatorio.Completado((fecha ?: respaldo).aLocalDateTime())
    "POSPUESTO" -> fecha?.let { EstadoRecordatorio.Pospuesto(it.aLocalDateTime()) } ?: EstadoRecordatorio.Pendiente
    "VENCIDO" -> EstadoRecordatorio.Vencido
    else -> EstadoRecordatorio.Pendiente
}

// ---------- Entity -> Dominio ----------

fun SyncEstado.aDominio(intentos: Int, ultimoError: String?): EstadoSincronizacion = when (this) {
    SyncEstado.SINCRONIZADO -> EstadoSincronizacion.Sincronizado
    SyncEstado.PENDIENTE_CREAR -> EstadoSincronizacion.Pendiente(OperacionPendiente.CREAR, intentos, ultimoError)
    SyncEstado.PENDIENTE_ACTUALIZAR -> EstadoSincronizacion.Pendiente(OperacionPendiente.ACTUALIZAR, intentos, ultimoError)
    SyncEstado.PENDIENTE_ELIMINAR -> EstadoSincronizacion.Pendiente(OperacionPendiente.ELIMINAR, intentos, ultimoError)
    SyncEstado.ERROR -> EstadoSincronizacion.ConError(ultimoError ?: "El servicio rechazó la operación")
}

/**
 * Convierte una fila de Room en el modelo de dominio. [categorias] es el catálogo de la app;
 * si la categoría no existe se crea una genérica en lugar de fallar.
 */
fun RecordatorioEntity.aDominio(categorias: Collection<Categoria>): Recordatorio {
    val categoria = categorias.firstOrNull { it.id == categoriaId } ?: Categoria(categoriaId.coerceAtLeast(1), "Otra")
    val nombreLugar = ubicacionNombre
    val lat = latitud
    val lon = longitud
    val ubicacion = if (nombreLugar != null && lat != null && lon != null) {
        runCatching { Ubicacion(nombreLugar, lat, lon) }.getOrNull()
    } else {
        null
    }
    val limite = fechaLimite?.aLocalDateTime()
    return Recordatorio(
        id = id,
        titulo = titulo,
        categoria = categoria,
        prioridad = prioridadDesdeNivel(prioridad) ?: Prioridad.Media,
        estado = estadoDesdeColumnas(estado, estadoFecha, actualizadoEn),
        descripcion = descripcion,
        fechaLimite = limite,
        ubicacion = ubicacion,
        aviso = if (limite != null) avisoMinutos?.let { runCatching { Aviso(it) }.getOrNull() } else null,
        creadoEn = creadoEn.aLocalDateTime(),
        sincronizacion = syncEstado.aDominio(intentosSync, ultimoErrorSync)
    )
}

// ---------- Entity -> DTO ----------

fun RecordatorioEntity.aDto(): RecordatorioDto = RecordatorioDto(
    id = uuid,
    titulo = titulo,
    descripcion = descripcion,
    categoriaId = categoriaId,
    prioridad = (prioridadDesdeNivel(prioridad) ?: Prioridad.Media).aCodigoRemoto(),
    estado = estado,
    estadoFecha = estadoFecha?.aIso(),
    fechaLimite = fechaLimite?.aIso(),
    avisoMinutos = avisoMinutos,
    ubicacion = if (ubicacionNombre != null && latitud != null && longitud != null) {
        UbicacionDto(ubicacionNombre, latitud, longitud)
    } else {
        null
    },
    creadoEn = creadoEn.aIso(),
    actualizadoEn = actualizadoEn.aIso()
)

// ---------- DTO -> Entity ----------

/**
 * Valida y transforma un DTO recibido. Devuelve null si el servicio envió datos que
 * violan las reglas de la app (se descartan en lugar de corromper la base local).
 */
fun RecordatorioDto.aEntidadOrNull(ahora: Long): RecordatorioEntity? {
    if (id.isBlank()) return null
    if (ValidadorRecordatorio.validarTitulo(titulo) != null) return null
    if (ValidadorRecordatorio.validarDescripcion(descripcion) != null) return null
    val prioridad = prioridadDesdeCodigoRemoto(prioridad) ?: return null
    val estado = estado.uppercase().takeIf { it in ESTADOS_VALIDOS } ?: return null
    if (categoriaId <= 0) return null
    val limite = fechaLimite?.let { it.aEpochMillisOrNull() ?: return null }
    val ubicacionValida = ubicacion?.takeIf { it.latitud in -90.0..90.0 && it.longitud in -180.0..180.0 }
    val actualizado = actualizadoEn?.aEpochMillisOrNull() ?: ahora
    return RecordatorioEntity(
        id = 0,
        uuid = id,
        titulo = titulo.trim(),
        descripcion = descripcion?.trim()?.ifBlank { null },
        categoriaId = categoriaId,
        prioridad = prioridad.aNivel(),
        estado = estado,
        estadoFecha = estadoFecha?.aEpochMillisOrNull(),
        fechaLimite = limite,
        avisoMinutos = avisoMinutos?.takeIf { limite != null && it in 1..Aviso.MAX_MINUTOS },
        ubicacionNombre = ubicacionValida?.nombre,
        latitud = ubicacionValida?.latitud,
        longitud = ubicacionValida?.longitud,
        creadoEn = creadoEn?.aEpochMillisOrNull() ?: actualizado,
        actualizadoEn = actualizado,
        syncEstado = SyncEstado.SINCRONIZADO
    )
}
