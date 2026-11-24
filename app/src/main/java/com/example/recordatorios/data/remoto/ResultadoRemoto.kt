package com.example.recordatorios.data.remoto

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Resultado tipado de una llamada remota. Toda la interpretación de códigos HTTP y
 * excepciones de red se hace acá, una sola vez; las pantallas nunca ven un código HTTP.
 */
sealed interface ResultadoRemoto<out T> {
    data class Exito<T>(val datos: T) : ResultadoRemoto<T>

    sealed interface Fallo : ResultadoRemoto<Nothing> {
        /** true: conviene reintentar más tarde sin intervención (red, timeout, 5xx). */
        val recuperable: Boolean
        val descripcion: String
    }

    /** 400, 409, 422 u otro 4xx: la petición no es válida; repetirla igual no sirve. */
    data class SolicitudInvalida(val codigo: Int, val mensaje: String?) : Fallo {
        override val recuperable = false
        override val descripcion = "El servicio rechazó los datos (HTTP $codigo)" + (mensaje?.let { ": $it" } ?: "")
    }

    /** 404: el recurso no existe en el servicio. */
    data object NoEncontrado : Fallo {
        override val recuperable = false
        override val descripcion = "El recurso no existe en el servicio (HTTP 404)"
    }

    /** 5xx, 408 o 429: problema del lado del servicio, temporal. */
    data class ErrorServidor(val codigo: Int) : Fallo {
        override val recuperable = true
        override val descripcion = "El servicio no está disponible (HTTP $codigo)"
    }

    /** El servicio no respondió dentro del tiempo configurado. */
    data object Timeout : Fallo {
        override val recuperable = true
        override val descripcion = "El servicio tardó demasiado en responder"
    }

    /** No hay red, no se resuelve el host o se rechazó la conexión. */
    data object SinConexion : Fallo {
        override val recuperable = true
        override val descripcion = "Sin conexión con el servicio"
    }

    /** La respuesta llegó pero no respeta el contrato (JSON inválido o cuerpo vacío). */
    data class RespuestaInvalida(val detalle: String?) : Fallo {
        override val recuperable = true
        override val descripcion = "Respuesta inesperada del servicio" + (detalle?.let { ": $it" } ?: "")
    }
}

private val jsonErrores = Json { ignoreUnknownKeys = true }

/** Ejecuta una llamada que debe devolver cuerpo (GET, PUT). */
suspend fun <T : Any> ejecutarLlamada(llamada: suspend () -> Response<T>): ResultadoRemoto<T> =
    capturarExcepciones {
        val respuesta = llamada()
        clasificarFallo(respuesta) ?: respuesta.body()?.let { ResultadoRemoto.Exito(it) }
            ?: ResultadoRemoto.RespuestaInvalida("cuerpo vacío")
    }

/** Ejecuta una llamada cuyo cuerpo no interesa (DELETE con 204). */
suspend fun ejecutarSinCuerpo(llamada: suspend () -> Response<*>): ResultadoRemoto<Unit> =
    capturarExcepciones {
        clasificarFallo(llamada()) ?: ResultadoRemoto.Exito(Unit)
    }

/** null si la respuesta es 2xx. */
fun clasificarFallo(respuesta: Response<*>): ResultadoRemoto.Fallo? {
    val codigo = respuesta.code()
    return when {
        respuesta.isSuccessful -> null
        codigo == 404 -> ResultadoRemoto.NoEncontrado
        codigo == 408 || codigo == 429 || codigo >= 500 -> ResultadoRemoto.ErrorServidor(codigo)
        else -> ResultadoRemoto.SolicitudInvalida(codigo, mensajeDeError(respuesta))
    }
}

private fun mensajeDeError(respuesta: Response<*>): String? = try {
    respuesta.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let {
        runCatching { jsonErrores.decodeFromString(ErrorDto.serializer(), it).error }.getOrNull() ?: it.take(120)
    }
} catch (_: IOException) {
    null
}

/** Traduce las excepciones de red a fallos tipados. El orden importa: timeout es un IOException. */
private inline fun <T> capturarExcepciones(bloque: () -> ResultadoRemoto<T>): ResultadoRemoto<T> = try {
    bloque()
} catch (e: CancellationException) {
    throw e
} catch (_: SocketTimeoutException) {
    ResultadoRemoto.Timeout
} catch (e: InterruptedIOException) {
    // OkHttp lanza InterruptedIOException("timeout") cuando vence callTimeout.
    if (e.message?.contains("timeout", ignoreCase = true) == true) ResultadoRemoto.Timeout else ResultadoRemoto.SinConexion
} catch (_: UnknownHostException) {
    ResultadoRemoto.SinConexion
} catch (_: ConnectException) {
    ResultadoRemoto.SinConexion
} catch (_: NoRouteToHostException) {
    ResultadoRemoto.SinConexion
} catch (_: IOException) {
    ResultadoRemoto.SinConexion
} catch (e: SerializationException) {
    ResultadoRemoto.RespuestaInvalida(e.message?.take(120))
} catch (e: IllegalArgumentException) {
    // Retrofit/conversores envuelven algunos errores de formato en IllegalArgumentException.
    ResultadoRemoto.RespuestaInvalida(e.message?.take(120))
}
