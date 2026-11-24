package com.example.recordatorios.data.remoto

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Construye el cliente HTTP y el servicio Retrofit. La URL base llega desde BuildConfig. */
object ClienteRemoto {

    val json = Json {
        ignoreUnknownKeys = true // campos nuevos del servicio no rompen la app
        explicitNulls = false
        encodeDefaults = true
    }

    fun crearOkHttp(registrarTrafico: Boolean, timeoutSegundos: Long = 8): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(timeoutSegundos, TimeUnit.SECONDS)
            .writeTimeout(timeoutSegundos, TimeUnit.SECONDS)
            .callTimeout(timeoutSegundos + 5, TimeUnit.SECONDS)
            // Se deja activo: recupera conexiones del pool que el servidor ya cerró. Sin esto,
            // la segunda petición de cada sincronización fallaba con un servidor HTTP/1.0.
            // Los reintentos ante errores HTTP, timeouts o falta de red los decide el repositorio.
            .retryOnConnectionFailure(true)
            .apply {
                if (registrarTrafico) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

    fun crearApi(baseUrl: String, cliente: OkHttpClient): RecordatoriosApi {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(url)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RecordatoriosApi::class.java)
    }
}
