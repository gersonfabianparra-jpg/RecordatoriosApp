package com.example.recordatorios.data

import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDateTime

/**
 * Contrato de la capa de datos que ven los ViewModel. Solo expone modelos de dominio:
 * ni entidades de Room ni DTO de Retrofit.
 *
 * Estrategia local-first: todas las lecturas salen de Room (Flow) y todas las escrituras se
 * aplican primero en Room; la sincronización con el servicio remoto ocurre después, en
 * segundo plano, sin bloquear a la interfaz.
 */
interface RepositorioRecordatorios {

    /** Categorías válidas (Set: sin repetidas). */
    val categorias: Set<Categoria>

    /** Etiqueta y color de cada categoría. */
    val estilos: Map<Categoria, EstiloCategoria>

    /** Listado filtrado y ordenado; vuelve a emitir con cada cambio en Room. */
    fun observarRecordatorios(filtro: FiltroListado = FiltroListado()): Flow<List<Recordatorio>>

    /** null si no existe o fue eliminado. */
    fun observarRecordatorio(id: Long): Flow<Recordatorio?>

    suspend fun obtenerRecordatorio(id: Long): Recordatorio?

    /** Guarda en Room (pendiente de sincronizar) y devuelve el recordatorio con su id local. */
    suspend fun crear(borrador: BorradorRecordatorio): Recordatorio

    suspend fun actualizar(id: Long, borrador: BorradorRecordatorio): Recordatorio

    suspend fun cambiarCompletado(id: Long, completado: Boolean)

    suspend fun eliminar(id: Long)

    /** Vuelve a poner en cola una operación que el servicio rechazó. */
    suspend fun reintentarSincronizacion(id: Long)

    /** Cantidad de registros con cambios todavía no confirmados por el servicio. */
    val cambiosSinSincronizar: Flow<Int>

    /** Estado general de la última sincronización (para el banner de la interfaz). */
    val estadoSincronizacion: StateFlow<EstadoSincronizacionGlobal>

    val conectado: Flow<Boolean>

    /** Pide una sincronización; varias solicitudes seguidas se combinan en una sola. */
    fun solicitarSincronizacion(motivo: String)

    /** Sincroniza ahora y espera el resultado (serializada: nunca corren dos a la vez). */
    suspend fun sincronizar(motivo: String = "manual"): EstadoSincronizacionGlobal
}

/** Filtro del listado, derivado de las preferencias guardadas en DataStore. */
data class FiltroListado(
    val prioridad: Prioridad? = null,
    val mostrarCompletados: Boolean = true,
    val orden: OrdenListado = OrdenListado.PRIORIDAD
)

/** Datos que ingresa la persona usuaria; el repositorio asigna id local y uuid. */
data class BorradorRecordatorio(
    val titulo: String,
    val descripcion: String?,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val fechaLimite: LocalDateTime? = null
)

/** Resultado global de la sincronización, en términos que la interfaz puede mostrar. */
sealed interface EstadoSincronizacionGlobal {
    data object Inactiva : EstadoSincronizacionGlobal
    data object EnCurso : EstadoSincronizacionGlobal
    data object SinConexion : EstadoSincronizacionGlobal
    data class Completada(
        val en: LocalDateTime,
        val enviados: Int,
        val rechazados: Int,
        val recibidos: Int
    ) : EstadoSincronizacionGlobal
    data class Fallida(val mensaje: String, val recuperable: Boolean) : EstadoSincronizacionGlobal
}

/** Error al leer o escribir en la base local (por ejemplo, almacenamiento lleno). */
class ErrorAlmacenamientoLocal(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

/** Datos que no cumplen las reglas de negocio. */
class DatosInvalidos(mensaje: String) : Exception(mensaje)
