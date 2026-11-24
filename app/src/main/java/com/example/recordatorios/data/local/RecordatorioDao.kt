package com.example.recordatorios.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Resultado de fusionar la lista remota con la base local. */
data class ResumenFusion(
    val insertados: Int = 0,
    val actualizados: Int = 0,
    val conservadosPorCambioLocal: Int = 0,
    val borradosEnServidor: Int = 0
)

/**
 * Acceso a la tabla `recordatorios`. Las consultas que observa la interfaz devuelven Flow:
 * Room vuelve a emitir cada vez que cambia la tabla. Las escrituras son funciones suspendidas
 * (Room las ejecuta fuera del hilo principal).
 *
 * Es una clase abstracta para poder incluir [fusionarRemotos], una operación compuesta que
 * se ejecuta dentro de una única transacción.
 */
@Dao
abstract class RecordatorioDao {

    // ---------- Consultas observables (interfaz) ----------

    @Query("SELECT * FROM recordatorios WHERE eliminado = 0 ORDER BY prioridad DESC, creadoEn DESC")
    abstract fun observarTodos(): Flow<List<RecordatorioEntity>>

    /**
     * Listado con filtro de prioridad (null = todas), filtro de estado (ocultar completados)
     * y orden elegido en las preferencias. El orden se resuelve en SQL con CASE.
     */
    @Query(
        """
        SELECT * FROM recordatorios
        WHERE eliminado = 0
          AND (:prioridad IS NULL OR prioridad = :prioridad)
          AND (:incluirCompletados = 1 OR estado != 'COMPLETADO')
        ORDER BY
          CASE WHEN :orden = 'PRIORIDAD' THEN prioridad END DESC,
          CASE WHEN :orden = 'FECHA_LIMITE' THEN (fechaLimite IS NULL) END ASC,
          CASE WHEN :orden = 'FECHA_LIMITE' THEN fechaLimite END ASC,
          CASE WHEN :orden = 'TITULO' THEN titulo END COLLATE NOCASE ASC,
          creadoEn DESC
        """
    )
    abstract fun observarFiltrados(prioridad: Int?, incluirCompletados: Boolean, orden: String): Flow<List<RecordatorioEntity>>

    /** Filtro por categoría. */
    @Query("SELECT * FROM recordatorios WHERE eliminado = 0 AND categoriaId = :categoriaId ORDER BY prioridad DESC")
    abstract fun observarPorCategoria(categoriaId: Int): Flow<List<RecordatorioEntity>>

    @Query("SELECT * FROM recordatorios WHERE id = :id AND eliminado = 0")
    abstract fun observarPorId(id: Long): Flow<RecordatorioEntity?>

    @Query("SELECT COUNT(*) FROM recordatorios WHERE syncEstado != 'SINCRONIZADO'")
    abstract fun observarCantidadSinSincronizar(): Flow<Int>

    // ---------- Lecturas puntuales ----------

    @Query("SELECT * FROM recordatorios WHERE id = :id AND eliminado = 0")
    abstract suspend fun obtenerPorId(id: Long): RecordatorioEntity?

    /** Incluye filas con borrado lógico (las necesita la sincronización). */
    @Query("SELECT * FROM recordatorios WHERE id = :id")
    abstract suspend fun obtenerPorIdIncluyendoEliminados(id: Long): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios WHERE uuid = :uuid")
    abstract suspend fun obtenerPorUuid(uuid: String): RecordatorioEntity?

    @Query("SELECT * FROM recordatorios")
    abstract suspend fun todosParaSincronizar(): List<RecordatorioEntity>

    /** Operaciones que hay que enviar, en el orden en que se hicieron. */
    @Query(
        """
        SELECT * FROM recordatorios
        WHERE syncEstado IN ('PENDIENTE_CREAR', 'PENDIENTE_ACTUALIZAR', 'PENDIENTE_ELIMINAR')
        ORDER BY actualizadoEn ASC
        """
    )
    abstract suspend fun pendientesDeEnvio(): List<RecordatorioEntity>

    // ---------- Escrituras ----------

    /** ABORT: si ya existiera el mismo uuid, falla en lugar de crear un duplicado. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertar(entidad: RecordatorioEntity): Long

    @Update
    abstract suspend fun actualizar(entidad: RecordatorioEntity): Int

    @Query("DELETE FROM recordatorios WHERE id = :id")
    abstract suspend fun borrarPorId(id: Long): Int

    // ---------- Estado de sincronización (condicionado a la versión enviada) ----------

    /**
     * Marca como sincronizada solo si la fila no cambió desde que se envió ([version]).
     * Si la persona editó el recordatorio mientras viajaba la petición, la fila sigue
     * pendiente y se enviará la versión nueva en la próxima sincronización.
     */
    @Query(
        """
        UPDATE recordatorios
        SET syncEstado = 'SINCRONIZADO', intentosSync = 0, ultimoErrorSync = NULL
        WHERE id = :id AND version = :version
        """
    )
    abstract suspend fun marcarSincronizado(id: Long, version: Long): Int

    /** Borra definitivamente una fila cuya eliminación confirmó el servicio. */
    @Query("DELETE FROM recordatorios WHERE id = :id AND version = :version AND eliminado = 1")
    abstract suspend fun borrarConfirmado(id: Long, version: Long): Int

    /** Error recuperable (sin red, timeout, 5xx): sigue pendiente y suma un intento. */
    @Query("UPDATE recordatorios SET intentosSync = intentosSync + 1, ultimoErrorSync = :mensaje WHERE id = :id")
    abstract suspend fun registrarFalloTransitorio(id: Long, mensaje: String): Int

    /** Error definitivo (4xx): deja de reintentarse automáticamente. */
    @Query(
        """
        UPDATE recordatorios
        SET syncEstado = 'ERROR', intentosSync = intentosSync + 1, ultimoErrorSync = :mensaje
        WHERE id = :id AND version = :version
        """
    )
    abstract suspend fun marcarError(id: Long, version: Long, mensaje: String): Int

    /** «Reintentar» desde la interfaz: vuelve a poner en cola una fila con error. */
    @Query(
        """
        UPDATE recordatorios
        SET syncEstado = CASE WHEN eliminado = 1 THEN 'PENDIENTE_ELIMINAR' ELSE 'PENDIENTE_ACTUALIZAR' END,
            intentosSync = 0, ultimoErrorSync = NULL
        WHERE id = :id AND syncEstado = 'ERROR'
        """
    )
    abstract suspend fun volverAEncolar(id: Long): Int

    /**
     * Incorpora la lista recibida del servicio en una sola transacción:
     * - uuid desconocido → se inserta (nunca se duplica: uuid es único);
     * - fila local SINCRONIZADA → se actualiza con los datos remotos;
     * - fila con cambios locales pendientes → se conserva (el cambio local se enviará después);
     * - fila SINCRONIZADA que ya no existe en el servicio → se borra.
     *
     * [uuidsRemotos] incluye también los registros remotos descartados por inválidos, para no
     * borrar localmente algo que sí existe en el servicio.
     */
    @Transaction
    open suspend fun fusionarRemotos(
        remotos: List<RecordatorioEntity>,
        uuidsRemotos: Set<String> = remotos.mapTo(HashSet()) { it.uuid }
    ): ResumenFusion {
        val locales = todosParaSincronizar().associateBy { it.uuid }
        var insertados = 0
        var actualizados = 0
        var conservados = 0
        for (remoto in remotos) {
            val local = locales[remoto.uuid]
            when {
                local == null -> {
                    insertar(remoto.copy(id = 0))
                    insertados++
                }
                local.syncEstado == SyncEstado.SINCRONIZADO -> {
                    val candidato = remoto.copy(id = local.id, version = local.version, creadoEn = local.creadoEn)
                    if (candidato != local) {
                        actualizar(candidato)
                        actualizados++
                    }
                }
                else -> conservados++
            }
        }
        var borrados = 0
        locales.values
            .filter { it.syncEstado == SyncEstado.SINCRONIZADO && it.uuid !in uuidsRemotos }
            .forEach { borrados += borrarPorId(it.id) }
        return ResumenFusion(insertados, actualizados, conservados, borrados)
    }
}
