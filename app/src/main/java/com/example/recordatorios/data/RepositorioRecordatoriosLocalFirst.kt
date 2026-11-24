package com.example.recordatorios.data

import android.database.SQLException
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.local.SyncEstado
import com.example.recordatorios.data.mapeo.aColumnas
import com.example.recordatorios.data.mapeo.aDominio
import com.example.recordatorios.data.mapeo.aDto
import com.example.recordatorios.data.mapeo.aEntidadOrNull
import com.example.recordatorios.data.mapeo.aEpochMillis
import com.example.recordatorios.data.mapeo.aLocalDateTime
import com.example.recordatorios.data.mapeo.aNivel
import com.example.recordatorios.data.red.MonitorConectividad
import com.example.recordatorios.data.remoto.RecordatoriosApi
import com.example.recordatorios.data.remoto.ResultadoRemoto
import com.example.recordatorios.data.remoto.ejecutarLlamada
import com.example.recordatorios.data.remoto.ejecutarSinCuerpo
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.model.ValidadorRecordatorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Repositorio local-first: coordina Room (fuente de verdad), el servicio remoto (Retrofit)
 * y la conectividad.
 *
 * Escrituras: se aplican en Room al instante, con un estado de sincronización pendiente, y
 * luego se pide una sincronización. Si no hay red, el registro queda pendiente y se envía
 * cuando la conexión vuelve.
 *
 * Para evitar duplicados se combinan tres mecanismos:
 *  1. Sincronización serializada: los disparos (inicio, red recuperada, cambios locales,
 *     botón) entran a un canal CONFLATED con un único consumidor, y además cada ejecución
 *     toma un Mutex. Nunca hay dos sincronizaciones simultáneas.
 *  2. Identificador estable: cada registro tiene un uuid generado una sola vez en el
 *     dispositivo y se envía con PUT /recordatorios/{uuid}, que es idempotente.
 *  3. Confirmación condicionada a la versión: solo se marca SINCRONIZADO si la fila no
 *     cambió mientras viajaba la petición; la base además impide uuid repetidos.
 */
class RepositorioRecordatoriosLocalFirst(
    private val dao: RecordatorioDao,
    private val api: RecordatoriosApi,
    private val monitor: MonitorConectividad,
    private val alcance: CoroutineScope,
    override val categorias: Set<Categoria> = DatosSimulados.categorias,
    override val estilos: Map<Categoria, EstiloCategoria> = DatosSimulados.estilosPorCategoria,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val generarUuid: () -> String = { UUID.randomUUID().toString() },
    private val registro: (String) -> Unit = {}
) : RepositorioRecordatorios {

    private val mutex = Mutex()
    private val disparos = Channel<String>(Channel.CONFLATED)
    private var reintento: Job? = null
    private var fallosSeguidos = 0
    private val _estado = MutableStateFlow<EstadoSincronizacionGlobal>(EstadoSincronizacionGlobal.Inactiva)

    override val estadoSincronizacion: StateFlow<EstadoSincronizacionGlobal> = _estado.asStateFlow()
    override val conectado: Flow<Boolean> = monitor.conectado
    override val cambiosSinSincronizar: Flow<Int> = dao.observarCantidadSinSincronizar()

    init {
        // Único consumidor: procesa las solicitudes de a una.
        alcance.launch {
            for (motivo in disparos) programarReintento(sincronizar(motivo))
        }
        // La primera emisión cubre el arranque; las siguientes, la recuperación de la red.
        alcance.launch {
            monitor.conectado.collect { hayRed ->
                if (hayRed) {
                    solicitarSincronizacion("conexión disponible")
                } else if (_estado.value !is EstadoSincronizacionGlobal.EnCurso) {
                    _estado.value = EstadoSincronizacionGlobal.SinConexion
                }
            }
        }
    }

    // ---------------- Lecturas: siempre desde Room ----------------

    override fun observarRecordatorios(filtro: FiltroListado): Flow<List<Recordatorio>> =
        dao.observarFiltrados(
            prioridad = filtro.prioridad?.aNivel(),
            incluirCompletados = filtro.mostrarCompletados,
            orden = filtro.orden.name
        ).map { filas -> filas.map { it.aDominio(categorias) } }

    override fun observarRecordatorio(id: Long): Flow<Recordatorio?> =
        dao.observarPorId(id).map { it?.aDominio(categorias) }

    override suspend fun obtenerRecordatorio(id: Long): Recordatorio? =
        enDisco { dao.obtenerPorId(id) }?.aDominio(categorias)

    // ---------------- Escrituras: primero Room, después la red ----------------

    override suspend fun crear(borrador: BorradorRecordatorio): Recordatorio {
        validar(borrador)
        val ahora = reloj()
        val nueva = RecordatorioEntity(
            uuid = generarUuid(),
            titulo = borrador.titulo.trim(),
            descripcion = borrador.descripcion?.trim()?.ifBlank { null },
            categoriaId = borrador.categoria.id,
            prioridad = borrador.prioridad.aNivel(),
            estado = "PENDIENTE",
            estadoFecha = null,
            fechaLimite = borrador.fechaLimite?.aEpochMillis(),
            avisoMinutos = null,
            ubicacionNombre = null,
            latitud = null,
            longitud = null,
            creadoEn = ahora,
            actualizadoEn = ahora,
            syncEstado = SyncEstado.PENDIENTE_CREAR
        )
        val id = enDisco { dao.insertar(nueva) }
        registro("Creado localmente #$id (${nueva.uuid}) → PENDIENTE_CREAR")
        solicitarSincronizacion("creación local")
        return nueva.copy(id = id).aDominio(categorias)
    }

    override suspend fun actualizar(id: Long, borrador: BorradorRecordatorio): Recordatorio {
        validar(borrador)
        val actual = enDisco { dao.obtenerPorId(id) } ?: throw DatosInvalidos("El recordatorio ya no existe.")
        val limite = borrador.fechaLimite?.aEpochMillis()
        val nueva = actual.copy(
            titulo = borrador.titulo.trim(),
            descripcion = borrador.descripcion?.trim()?.ifBlank { null },
            categoriaId = borrador.categoria.id,
            prioridad = borrador.prioridad.aNivel(),
            fechaLimite = limite,
            avisoMinutos = actual.avisoMinutos.takeIf { limite != null }
        ).conCambioLocal()
        enDisco { dao.actualizar(nueva) }
        registro("Modificado localmente #$id → ${nueva.syncEstado} (versión ${nueva.version})")
        solicitarSincronizacion("modificación local")
        return nueva.aDominio(categorias)
    }

    override suspend fun cambiarCompletado(id: Long, completado: Boolean) {
        val actual = enDisco { dao.obtenerPorId(id) } ?: return
        val (estado, fecha) = if (completado) {
            EstadoRecordatorio.Completado(reloj().aLocalDateTime()).aColumnas()
        } else {
            EstadoRecordatorio.Pendiente.aColumnas()
        }
        enDisco { dao.actualizar(actual.copy(estado = estado, estadoFecha = fecha).conCambioLocal()) }
        solicitarSincronizacion("cambio de estado")
    }

    /**
     * Borrado lógico: la fila se oculta de inmediato y queda PENDIENTE_ELIMINAR hasta que el
     * servicio confirma (204, o 404 si nunca había llegado). Recién ahí se borra de Room.
     */
    override suspend fun eliminar(id: Long) {
        val actual = enDisco { dao.obtenerPorId(id) } ?: return
        enDisco {
            dao.actualizar(
                actual.copy(
                    eliminado = true,
                    syncEstado = SyncEstado.PENDIENTE_ELIMINAR,
                    version = actual.version + 1,
                    actualizadoEn = reloj(),
                    intentosSync = 0,
                    ultimoErrorSync = null
                )
            )
        }
        registro("Eliminado localmente #$id → PENDIENTE_ELIMINAR")
        solicitarSincronizacion("eliminación local")
    }

    override suspend fun reintentarSincronizacion(id: Long) {
        enDisco { dao.volverAEncolar(id) }
        solicitarSincronizacion("reintento manual")
    }

    /** Nueva versión local: sube la versión y deja el registro pendiente de envío. */
    private fun RecordatorioEntity.conCambioLocal(): RecordatorioEntity = copy(
        version = version + 1,
        actualizadoEn = reloj(),
        // Si nunca llegó al servicio sigue siendo una creación; PUT crea o reemplaza igual.
        syncEstado = if (syncEstado == SyncEstado.PENDIENTE_CREAR) SyncEstado.PENDIENTE_CREAR else SyncEstado.PENDIENTE_ACTUALIZAR,
        intentosSync = 0,
        ultimoErrorSync = null
    )

    private fun validar(borrador: BorradorRecordatorio) {
        val errores = ValidadorRecordatorio.errores(1, borrador.titulo.trim(), borrador.descripcion, borrador.fechaLimite, null)
        if (errores.isNotEmpty()) throw DatosInvalidos(errores.joinToString("; "))
        if (borrador.categoria !in categorias) throw DatosInvalidos("La categoría no es válida")
    }

    private suspend fun <T> enDisco(operacion: suspend () -> T): T = try {
        operacion()
    } catch (e: SQLException) {
        throw ErrorAlmacenamientoLocal("No se pudo acceder al almacenamiento del dispositivo.", e)
    }

    // ---------------- Sincronización ----------------

    override fun solicitarSincronizacion(motivo: String) {
        disparos.trySend(motivo)
    }

    /**
     * Ante un fallo recuperable del servicio (timeout, 5xx) se programa un nuevo intento con
     * espera exponencial (10 s, 20 s, 40 s… hasta 5 min). Sin red no se programa nada: el
     * monitor de conectividad dispara la sincronización cuando la red vuelve.
     */
    private fun programarReintento(resultado: EstadoSincronizacionGlobal) {
        reintento?.cancel()
        if (resultado is EstadoSincronizacionGlobal.Fallida && resultado.recuperable) {
            fallosSeguidos++
            val espera = (ESPERA_BASE_MS shl (fallosSeguidos - 1).coerceAtMost(5)).coerceAtMost(ESPERA_MAXIMA_MS)
            registro("Reintento automático en ${espera / 1000} s")
            reintento = alcance.launch {
                delay(espera)
                solicitarSincronizacion("reintento automático")
            }
        } else if (resultado is EstadoSincronizacionGlobal.Completada) {
            fallosSeguidos = 0
        }
    }

    override suspend fun sincronizar(motivo: String): EstadoSincronizacionGlobal = mutex.withLock {
        if (!monitor.hayConexion()) {
            registro("Sincronización omitida ($motivo): sin conexión")
            return@withLock EstadoSincronizacionGlobal.SinConexion.also { _estado.value = it }
        }
        registro("Sincronización iniciada ($motivo)")
        _estado.value = EstadoSincronizacionGlobal.EnCurso
        val resultado = try {
            ejecutarSincronizacion()
        } catch (e: SQLException) {
            EstadoSincronizacionGlobal.Fallida("Error del almacenamiento local: ${e.message}", recuperable = true)
        }
        registro("Sincronización terminada: $resultado")
        _estado.value = resultado
        resultado
    }

    private suspend fun ejecutarSincronizacion(): EstadoSincronizacionGlobal {
        // 1) Subida: operaciones pendientes, en orden.
        var enviados = 0
        var rechazados = 0
        for (fila in dao.pendientesDeEnvio()) {
            val resultado = enviar(fila)
            when (resultado) {
                is ResultadoRemoto.Exito -> {
                    enviados++
                    if (fila.syncEstado == SyncEstado.PENDIENTE_ELIMINAR) {
                        dao.borrarConfirmado(fila.id, fila.version)
                    } else if (dao.marcarSincronizado(fila.id, fila.version) == 0) {
                        registro("#${fila.id} cambió durante el envío: queda pendiente la versión nueva")
                    }
                }
                is ResultadoRemoto.Fallo -> if (resultado.recuperable) {
                    // Red, timeout o 5xx: no tiene sentido seguir; se conserva todo para el próximo intento.
                    dao.registrarFalloTransitorio(fila.id, resultado.descripcion)
                    return falloGlobal(resultado)
                } else {
                    rechazados++
                    dao.marcarError(fila.id, fila.version, resultado.descripcion)
                }
            }
        }
        // 2) Bajada: lista remota → validación → Room.
        return when (val remotos = ejecutarLlamada { api.listar() }) {
            is ResultadoRemoto.Exito -> {
                val ahora = reloj()
                val validas = remotos.datos.mapNotNull { it.aEntidadOrNull(ahora) }
                val descartados = remotos.datos.size - validas.size
                if (descartados > 0) registro("$descartados registros remotos inválidos descartados")
                val resumen = dao.fusionarRemotos(validas, remotos.datos.mapTo(HashSet()) { it.id })
                registro("Fusión remota: $resumen")
                EstadoSincronizacionGlobal.Completada(
                    en = ahora.aLocalDateTime(),
                    enviados = enviados,
                    rechazados = rechazados,
                    recibidos = resumen.insertados + resumen.actualizados
                )
            }
            is ResultadoRemoto.Fallo -> falloGlobal(remotos)
        }
    }

    private suspend fun enviar(fila: RecordatorioEntity): ResultadoRemoto<*> =
        if (fila.syncEstado == SyncEstado.PENDIENTE_ELIMINAR) {
            when (val r = ejecutarSinCuerpo { api.eliminar(fila.uuid) }) {
                ResultadoRemoto.NoEncontrado -> ResultadoRemoto.Exito(Unit) // ya no existe: objetivo cumplido
                else -> r
            }
        } else {
            ejecutarLlamada { api.guardar(fila.uuid, fila.aDto()) }
        }.also { registro("${fila.syncEstado} #${fila.id} (${fila.uuid}) → $it") }

    private fun falloGlobal(fallo: ResultadoRemoto.Fallo): EstadoSincronizacionGlobal =
        if (fallo is ResultadoRemoto.SinConexion) {
            // El dispositivo tiene red pero el servicio no acepta conexiones: se reintenta solo.
            if (monitor.hayConexion()) {
                EstadoSincronizacionGlobal.Fallida("No se pudo contactar al servicio remoto", recuperable = true)
            } else {
                EstadoSincronizacionGlobal.SinConexion
            }
        } else {
            EstadoSincronizacionGlobal.Fallida(fallo.descripcion, fallo.recuperable)
        }

    private companion object {
        const val ESPERA_BASE_MS = 10_000L
        const val ESPERA_MAXIMA_MS = 300_000L
    }
}
