package com.example.recordatorios.datos

import com.example.recordatorios.data.remoto.ClienteRemoto
import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.data.remoto.RecordatoriosApi
import com.example.recordatorios.data.remoto.ResultadoRemoto
import com.example.recordatorios.data.remoto.ejecutarLlamada
import com.example.recordatorios.data.remoto.ejecutarSinCuerpo
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Clasificación de respuestas HTTP y errores de red con un servidor HTTP real (MockWebServer). */
class ResultadoRemotoTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: RecordatoriosApi
    private val dto = RecordatorioDto(id = "u1", titulo = "Comprar pan", categoriaId = 5, prioridad = "BAJA")

    @Before
    fun iniciar() {
        servidor = MockWebServer().apply { start() }
        api = ClienteRemoto.crearApi(
            servidor.url("/").toString(),
            ClienteRemoto.crearOkHttp(registrarTrafico = false, timeoutSegundos = 1)
        )
    }

    @After
    fun detener() = servidor.shutdown()

    private fun json(codigo: Int, cuerpo: String) =
        MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo)

    @Test
    fun respuesta200_esExitoConElDtoConvertido() = runTest {
        servidor.enqueue(json(200, """[{"id":"u1","titulo":"Comprar pan","categoriaId":5,"prioridad":"BAJA","campoNuevo":1}]"""))
        val r = ejecutarLlamada { api.listar() }
        assertEquals(ResultadoRemoto.Exito(listOf(dto)), r)
    }

    @Test
    fun put201_esExitoYEnviaElCuerpoAlIdCorrecto() = runTest {
        servidor.enqueue(json(201, """{"id":"u1","titulo":"Comprar pan","categoriaId":5,"prioridad":"BAJA"}"""))
        val r = ejecutarLlamada { api.guardar("u1", dto) }
        assertTrue(r is ResultadoRemoto.Exito)
        val peticion = servidor.takeRequest()
        assertEquals("PUT", peticion.method)
        assertEquals("/recordatorios/u1", peticion.path)
    }

    @Test
    fun error422_esSolicitudInvalidaConElMensajeDelServicio() = runTest {
        servidor.enqueue(json(422, """{"error":"Prioridad desconocida"}"""))
        val r = ejecutarLlamada { api.guardar("u1", dto) }
        assertEquals(ResultadoRemoto.SolicitudInvalida(422, "Prioridad desconocida"), r)
        assertTrue(!(r as ResultadoRemoto.Fallo).recuperable)
    }

    @Test
    fun error400_tambienEsSolicitudInvalida() = runTest {
        servidor.enqueue(json(400, """{"error":"JSON inválido"}"""))
        assertTrue(ejecutarLlamada { api.listar() } is ResultadoRemoto.SolicitudInvalida)
    }

    @Test
    fun error500_esErrorDelServidorRecuperable() = runTest {
        servidor.enqueue(json(503, """{"error":"mantenimiento"}"""))
        val r = ejecutarLlamada { api.listar() }
        assertEquals(ResultadoRemoto.ErrorServidor(503), r)
        assertTrue((r as ResultadoRemoto.Fallo).recuperable)
    }

    @Test
    fun delete404_seInformaComoNoEncontrado() = runTest {
        servidor.enqueue(json(404, """{"error":"No existe"}"""))
        assertEquals(ResultadoRemoto.NoEncontrado, ejecutarSinCuerpo { api.eliminar("u1") })
    }

    @Test
    fun delete204_esExitoSinCuerpo() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(204))
        assertEquals(ResultadoRemoto.Exito(Unit), ejecutarSinCuerpo { api.eliminar("u1") })
    }

    @Test
    fun servidorQueNoRespondeATiempo_esTimeout() = runTest {
        servidor.enqueue(json(200, "[]").setHeadersDelay(3, TimeUnit.SECONDS))
        assertEquals(ResultadoRemoto.Timeout, ejecutarLlamada { api.listar() })
    }

    @Test
    fun sinServidorEscuchando_esSinConexion() = runTest {
        servidor.shutdown()
        assertEquals(ResultadoRemoto.SinConexion, ejecutarLlamada { api.listar() })
    }

    @Test
    fun jsonQueNoRespetaElContrato_esRespuestaInvalida() = runTest {
        servidor.enqueue(json(200, """[{"titulo":"sin id"}]"""))
        assertTrue(ejecutarLlamada { api.listar() } is ResultadoRemoto.RespuestaInvalida)
    }
}
