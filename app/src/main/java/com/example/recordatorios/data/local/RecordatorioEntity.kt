package com.example.recordatorios.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Estado de sincronización de una fila con el servicio remoto.
 * Room guarda los enum por su nombre (TEXT).
 */
enum class SyncEstado {
    /** Confirmado por el servicio: la copia local y la remota coinciden. */
    SINCRONIZADO,
    /** Creado localmente; todavía no lo confirmó el servicio. */
    PENDIENTE_CREAR,
    /** Modificado localmente; el cambio todavía no se envió. */
    PENDIENTE_ACTUALIZAR,
    /** Eliminado localmente (fila oculta); falta confirmar la eliminación remota. */
    PENDIENTE_ELIMINAR,
    /** El servicio rechazó la operación (4xx): no se reintenta sola, requiere acción. */
    ERROR
}

/**
 * Esquema de Room. No es el modelo de la interfaz ni el contrato del servicio:
 * - [id] es la clave local autogenerada que usan la navegación y las pantallas.
 * - [uuid] es el identificador estable compartido con el servicio. Se genera una sola vez
 *   al crear el registro y nunca cambia, por eso reenviar la misma operación no duplica nada.
 * - [version] aumenta con cada cambio local; la confirmación del servicio solo marca como
 *   sincronizada la versión que realmente se envió.
 * Las fechas se guardan como milisegundos epoch (Long) para poder ordenarlas en SQL.
 */
@Entity(
    tableName = "recordatorios",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["syncEstado"]),
        Index(value = ["prioridad"])
    ]
)
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val titulo: String,
    val descripcion: String?,
    val categoriaId: Int,
    /** Nivel de prioridad: 3 alta, 2 media, 1 baja (permite ORDER BY). */
    val prioridad: Int,
    /** PENDIENTE, COMPLETADO, POSPUESTO o VENCIDO. */
    val estado: String,
    /** Fecha asociada al estado (completado en / pospuesto hasta). */
    val estadoFecha: Long?,
    val fechaLimite: Long?,
    val avisoMinutos: Int?,
    val ubicacionNombre: String?,
    val latitud: Double?,
    val longitud: Double?,
    val creadoEn: Long,
    val actualizadoEn: Long,
    val version: Long = 1,
    val syncEstado: SyncEstado,
    val intentosSync: Int = 0,
    val ultimoErrorSync: String? = null,
    /** Borrado lógico mientras la eliminación remota está pendiente. */
    val eliminado: Boolean = false
)
