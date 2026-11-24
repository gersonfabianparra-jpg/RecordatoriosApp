package com.example.recordatorios.datos

import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.local.SyncEstado
import com.example.recordatorios.data.red.MonitorConectividad
import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.data.remoto.RecordatoriosApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException

/** DAO en memoria con el mismo comportamiento que las consultas SQL de Room. */
class DaoEnMemoria : RecordatorioDao() {
    val filas = MutableStateFlow<List<RecordatorioEntity>>(emptyList())
    private var siguienteId = 1L

    private fun visibles() = filas.value.filter { !it.eliminado }

    override fun observarTodos(): Flow<List<RecordatorioEntity>> =
        filas.map { l -> l.filter { !it.eliminado }.sortedWith(compareByDescending<RecordatorioEntity> { it.prioridad }.thenByDescending { it.creadoEn }) }

    override fun observarFiltrados(prioridad: Int?, incluirCompletados: Boolean, orden: String): Flow<List<RecordatorioEntity>> =
        filas.map { l ->
            l.filter { !it.eliminado && (prioridad == null || it.prioridad == prioridad) && (incluirCompletados || it.estado != "COMPLETADO") }
                .sortedWith(
                    when (orden) {
                        "PRIORIDAD" -> compareByDescending<RecordatorioEntity> { it.prioridad }.thenByDescending { it.creadoEn }
                        "TITULO" -> compareBy<RecordatorioEntity> { it.titulo.lowercase() }.thenByDescending { it.creadoEn }
                        "FECHA_LIMITE" -> compareBy<RecordatorioEntity> { it.fechaLimite == null }.thenBy { it.fechaLimite }.thenByDescending { it.creadoEn }
                        else -> compareByDescending { it.creadoEn }
                    }
                )
        }

    override fun observarPorCategoria(categoriaId: Int) = filas.map { l -> l.filter { !it.eliminado && it.categoriaId == categoriaId } }
    override fun observarPorId(id: Long) = filas.map { l -> l.firstOrNull { it.id == id && !it.eliminado } }
    override fun observarCantidadSinSincronizar() = filas.map { l -> l.count { it.syncEstado != SyncEstado.SINCRONIZADO } }
    override suspend fun obtenerPorId(id: Long) = visibles().firstOrNull { it.id == id }
    override suspend fun obtenerPorIdIncluyendoEliminados(id: Long) = filas.value.firstOrNull { it.id == id }
    override suspend fun obtenerPorUuid(uuid: String) = filas.value.firstOrNull { it.uuid == uuid }
    override suspend fun todosParaSincronizar() = filas.value

    override suspend fun pendientesDeEnvio() = filas.value
        .filter { it.syncEstado in setOf(SyncEstado.PENDIENTE_CREAR, SyncEstado.PENDIENTE_ACTUALIZAR, SyncEstado.PENDIENTE_ELIMINAR) }
        .sortedBy { it.actualizadoEn }

    override suspend fun insertar(entidad: RecordatorioEntity): Long {
        // Mismo efecto que el índice único de Room.
        check(filas.value.none { it.uuid == entidad.uuid }) { "UNIQUE constraint failed: recordatorios.uuid" }
        val id = siguienteId++
        filas.update { it + entidad.copy(id = id) }
        return id
    }

    override suspend fun actualizar(entidad: RecordatorioEntity): Int = modificar({ it.id == entidad.id }) { entidad }
    override suspend fun borrarPorId(id: Long): Int = borrar { it.id == id }

    override suspend fun marcarSincronizado(id: Long, version: Long) =
        modificar({ it.id == id && it.version == version }) { it.copy(syncEstado = SyncEstado.SINCRONIZADO, intentosSync = 0, ultimoErrorSync = null) }

    override suspend fun borrarConfirmado(id: Long, version: Long) = borrar { it.id == id && it.version == version && it.eliminado }

    override suspend fun registrarFalloTransitorio(id: Long, mensaje: String) =
        modificar({ it.id == id }) { it.copy(intentosSync = it.intentosSync + 1, ultimoErrorSync = mensaje) }

    override suspend fun marcarError(id: Long, version: Long, mensaje: String) =
        modificar({ it.id == id && it.version == version }) {
            it.copy(syncEstado = SyncEstado.ERROR, intentosSync = it.intentosSync + 1, ultimoErrorSync = mensaje)
        }

    override suspend fun volverAEncolar(id: Long) = modificar({ it.id == id && it.syncEstado == SyncEstado.ERROR }) {
        it.copy(
            syncEstado = if (it.eliminado) SyncEstado.PENDIENTE_ELIMINAR else SyncEstado.PENDIENTE_ACTUALIZAR,
            intentosSync = 0, ultimoErrorSync = null
        )
    }

    private fun modificar(condicion: (RecordatorioEntity) -> Boolean, cambio: (RecordatorioEntity) -> RecordatorioEntity): Int {
        var n = 0
        filas.update { l -> l.map { if (condicion(it)) cambio(it).also { n++ } else it } }
        return n
    }

    private fun borrar(condicion: (RecordatorioEntity) -> Boolean): Int {
        val antes = filas.value.size
        filas.update { l -> l.filterNot(condicion) }
        return antes - filas.value.size
    }
}

/** Monitor de red controlable desde la prueba. */
class MonitorFalso(inicial: Boolean) : MonitorConectividad {
    val estado = MutableStateFlow(inicial)
    override val conectado: Flow<Boolean> = estado
    override fun hayConexion() = estado.value
}

/** Servicio remoto en memoria: guarda por id (PUT idempotente) y cuenta las peticiones. */
class ServidorFalso : RecordatoriosApi {
    enum class Modo { NORMAL, SIN_RED, ERROR_500, TIMEOUT, GUARDA_Y_TIMEOUT, RECHAZAR }

    val datos = linkedMapOf<String, RecordatorioDto>()
    var modo = Modo.NORMAL
    var puts = 0
    var deletes = 0
    var gets = 0
    /** Se ejecuta durante un PUT, antes de responder (simula cambios mientras viaja la petición). */
    var duranteElPut: (suspend () -> Unit)? = null

    private fun fallar() {
        when (modo) {
            Modo.SIN_RED -> throw ConnectException("Failed to connect")
            Modo.TIMEOUT -> throw SocketTimeoutException("timeout")
            else -> Unit
        }
    }

    /*
     * Respuestas armadas a mano con host 127.0.0.1: los atajos Response.success/error de Retrofit
     * usan "localhost", y en la JVM de pruebas el mapeo IDN de OkHttp 5 para Android no termina.
     */
    private fun crudo(codigo: Int) = okhttp3.Response.Builder()
        .code(codigo)
        .message("HTTP $codigo")
        .protocol(Protocol.HTTP_1_1)
        .request(Request.Builder().url("http://127.0.0.1/").build())
        .build()

    private fun <T> exito(codigo: Int, cuerpo: T): Response<T> = Response.success(cuerpo, crudo(codigo))

    private fun <T> error(codigo: Int): Response<T> =
        Response.error("""{"error":"simulado $codigo"}""".toResponseBody(), crudo(codigo))

    override suspend fun listar(): Response<List<RecordatorioDto>> {
        fallar()
        if (modo == Modo.ERROR_500) return error(500)
        gets++
        return exito(200, datos.values.toList())
    }

    override suspend fun guardar(id: String, recordatorio: RecordatorioDto): Response<RecordatorioDto> {
        fallar()
        if (modo == Modo.ERROR_500) return error(500)
        if (modo == Modo.RECHAZAR) return error(422)
        puts++
        duranteElPut?.invoke()
        val existia = datos.containsKey(id)
        datos[id] = recordatorio
        // El servidor guardó, pero la respuesta "se pierde" en el camino.
        if (modo == Modo.GUARDA_Y_TIMEOUT) throw SocketTimeoutException("timeout")
        return exito(if (existia) 200 else 201, recordatorio)
    }

    override suspend fun eliminar(id: String): Response<Unit> {
        fallar()
        if (modo == Modo.ERROR_500) return error(500)
        deletes++
        return if (datos.remove(id) != null) exito(204, Unit) else error(404)
    }
}
