package com.example.recordatorios.data.remoto

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Servicio remoto de recordatorios (Retrofit). Todas las funciones son suspendidas y
 * devuelven Response<T> para que la capa de datos pueda leer el código HTTP.
 *
 * Crear y actualizar usan PUT /recordatorios/{id} con el uuid generado en el dispositivo:
 * PUT es idempotente, así que repetir la misma petición (por un reintento o porque dos
 * eventos dispararon la sincronización) deja un único registro en el servicio.
 */
interface RecordatoriosApi {

    @GET("recordatorios")
    suspend fun listar(): Response<List<RecordatorioDto>>

    /** Crea (201) o reemplaza (200) el recordatorio con ese id. */
    @PUT("recordatorios/{id}")
    suspend fun guardar(@Path("id") id: String, @Body recordatorio: RecordatorioDto): Response<RecordatorioDto>

    /** 204 si se eliminó; 404 si ya no existía (también cuenta como confirmado). */
    @DELETE("recordatorios/{id}")
    suspend fun eliminar(@Path("id") id: String): Response<Unit>
}
