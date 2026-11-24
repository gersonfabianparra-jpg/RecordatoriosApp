package com.example.recordatorios.data.remoto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contrato JSON del servicio remoto. Solo lo conocen la capa remota y los mapeos:
 * nunca llega al ViewModel ni a las funciones componibles.
 *
 * Las fechas viajan como texto ISO-8601 local (2026-11-24T09:30:00) y los enumerados
 * como texto, porque así los define el servicio.
 */
@Serializable
data class RecordatorioDto(
    @SerialName("id") val id: String,
    @SerialName("titulo") val titulo: String,
    @SerialName("descripcion") val descripcion: String? = null,
    @SerialName("categoriaId") val categoriaId: Int,
    @SerialName("prioridad") val prioridad: String,
    @SerialName("estado") val estado: String = "PENDIENTE",
    @SerialName("estadoFecha") val estadoFecha: String? = null,
    @SerialName("fechaLimite") val fechaLimite: String? = null,
    @SerialName("avisoMinutos") val avisoMinutos: Int? = null,
    @SerialName("ubicacion") val ubicacion: UbicacionDto? = null,
    @SerialName("creadoEn") val creadoEn: String? = null,
    @SerialName("actualizadoEn") val actualizadoEn: String? = null
)

@Serializable
data class UbicacionDto(
    @SerialName("nombre") val nombre: String,
    @SerialName("latitud") val latitud: Double,
    @SerialName("longitud") val longitud: Double
)

/** Cuerpo de error que devuelve el servicio en respuestas 4xx/5xx. */
@Serializable
data class ErrorDto(@SerialName("error") val error: String? = null)
