package com.example.recordatorios.model

/** Operación local que todavía no confirmó el servicio remoto. */
enum class OperacionPendiente(val etiqueta: String) {
    CREAR("creación"),
    ACTUALIZAR("modificación"),
    ELIMINAR("eliminación")
}

/**
 * Estado de sincronización de un recordatorio, en términos de dominio: la interfaz
 * no conoce los valores que usa Room ni los códigos HTTP.
 */
sealed interface EstadoSincronizacion {
    /** Confirmado por el servicio. */
    data object Sincronizado : EstadoSincronizacion

    /** Guardado en el dispositivo; se enviará cuando haya conexión. */
    data class Pendiente(
        val operacion: OperacionPendiente,
        val intentos: Int = 0,
        val ultimoError: String? = null
    ) : EstadoSincronizacion

    /** El servicio rechazó la operación: necesita un nuevo intento manual. */
    data class ConError(val mensaje: String) : EstadoSincronizacion
}
