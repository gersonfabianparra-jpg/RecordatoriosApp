package com.example.recordatorios.model

import java.time.LocalDateTime

/**
 * Estado del ciclo de vida de un recordatorio. Algunos estados llevan datos
 * propios (fecha de finalización, nueva fecha de aviso).
 */
sealed class EstadoRecordatorio(val nombre: String) {
    data object Pendiente : EstadoRecordatorio("Pendiente")
    data class Completado(val completadoEn: LocalDateTime) : EstadoRecordatorio("Completado")
    data class Pospuesto(val hasta: LocalDateTime) : EstadoRecordatorio("Pospuesto")
    data object Vencido : EstadoRecordatorio("Vencido")

    companion object {
        /** Nombres de los estados en el orden en que se muestran en los filtros. */
        val nombres: List<String> = listOf("Pendiente", "Pospuesto", "Vencido", "Completado")
    }
}
