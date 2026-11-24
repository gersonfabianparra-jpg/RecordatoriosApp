package com.example.recordatorios.datos

import com.example.recordatorios.data.BorradorRecordatorio
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.EstadoSincronizacionGlobal
import com.example.recordatorios.data.RepositorioRecordatoriosLocalFirst
import com.example.recordatorios.data.local.SyncEstado
import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.model.EstadoSincronizacion
import com.example.recordatorios.model.Prioridad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositorioLocalFirstTest {

    private val dao = DaoEnMemoria()
    private val servidor = ServidorFalso()
    private var reloj = 1_700_000_000_000L
    private var contadorUuid = 0
    private val registro = mutableListOf<String>()
    private val trabajos = mutableListOf<Job>()

    /** runTest que, al terminar, cancela el alcance del repositorio (y sus reintentos programados). */
    private fun probar(cuerpo: suspend TestScope.() -> Unit): TestResult = runTest {
        try {
            cuerpo()
        } finally {
            trabajos.forEach { it.cancel() }
        }
    }

    private fun TestScope.repositorio(conectado: Boolean): Pair<RepositorioRecordatoriosLocalFirst, MonitorFalso> {
        val monitor = MonitorFalso(conectado)
        val repo = RepositorioRecordatoriosLocalFirst(
            dao = dao,
            api = servidor,
            monitor = monitor,
            // Alcance "de aplicación" sobre el mismo planificador virtual: advanceUntilIdle lo ejecuta
            // (las corrutinas de backgroundScope no cuentan para advanceUntilIdle).
            alcance = CoroutineScope(coroutineContext.minusKey(Job) + Job().also { trabajos += it }),
            reloj = { reloj++ },
            generarUuid = { "uuid-${++contadorUuid}" },
            registro = { registro += it }
        )
        return repo to monitor
    }

    private fun borrador(titulo: String = "Llamar al banco") =
        BorradorRecordatorio(titulo, null, DatosSimulados.categorias.first(), Prioridad.Alta)

    @Test
    fun crearSinConexion_seGuardaEnRoomComoPendienteYNoLlamaAlServicio() = probar {
        val (repo, _) = repositorio(conectado = false)
        val creado = repo.crear(borrador())
        advanceUntilIdle()

        assertEquals(SyncEstado.PENDIENTE_CREAR, dao.filas.value.single().syncEstado)
        assertTrue(creado.sincronizacion is EstadoSincronizacion.Pendiente)
        assertEquals(0, servidor.puts)
        assertEquals(EstadoSincronizacionGlobal.SinConexion, repo.estadoSincronizacion.value)
        // La interfaz lo ve igual, porque lee de Room.
        assertEquals(1, repo.observarRecordatorios().first().size)
    }

    @Test
    fun alRecuperarLaRed_seEnviaLaOperacionYSeMarcaSincronizadaSinDuplicar() = probar {
        val (repo, monitor) = repositorio(conectado = false)
        repo.crear(borrador("Tarea uno"))
        repo.crear(borrador("Tarea dos"))
        advanceUntilIdle()

        monitor.estado.value = true
        advanceUntilIdle()

        assertEquals(2, servidor.datos.size)
        assertEquals(2, servidor.puts)
        assertTrue(dao.filas.value.all { it.syncEstado == SyncEstado.SINCRONIZADO })
        assertEquals(2, dao.filas.value.size) // la bajada no volvió a insertar lo mismo
        assertTrue(repo.estadoSincronizacion.value is EstadoSincronizacionGlobal.Completada)
    }

    @Test
    fun variosDisparosSimultaneos_noDuplicanEnvios() = probar {
        val (repo, monitor) = repositorio(conectado = false)
        repeat(3) { repo.crear(borrador("Tarea $it")) }
        advanceUntilIdle()

        monitor.estado.value = true
        // Red recuperada + botón + 5 llamadas directas al mismo tiempo.
        repo.solicitarSincronizacion("botón")
        repo.solicitarSincronizacion("pantalla")
        (1..5).map { async { repo.sincronizar("prueba $it") } }.awaitAll()
        advanceUntilIdle()

        assertEquals(3, servidor.datos.size)
        assertEquals("cada registro se envía una sola vez", 3, servidor.puts)
        assertEquals(3, dao.filas.value.size)
    }

    @Test
    fun respuestaPerdidaPorTimeout_alReintentarNoSeCreaUnSegundoRegistro() = probar {
        val (repo, _) = repositorio(conectado = true)
        advanceUntilIdle()
        servidor.modo = ServidorFalso.Modo.GUARDA_Y_TIMEOUT
        repo.crear(borrador())
        runCurrent()

        // El servidor lo guardó, pero el dispositivo no recibió la confirmación.
        assertEquals(1, servidor.datos.size)
        val fila = dao.filas.value.single()
        assertEquals(SyncEstado.PENDIENTE_CREAR, fila.syncEstado)
        assertEquals(1, fila.intentosSync)
        assertTrue(repo.estadoSincronizacion.value is EstadoSincronizacionGlobal.Fallida)

        servidor.modo = ServidorFalso.Modo.NORMAL
        advanceUntilIdle() // reintento automático con espera exponencial

        assertEquals("PUT idempotente: sigue habiendo un único registro", 1, servidor.datos.size)
        assertEquals(SyncEstado.SINCRONIZADO, dao.filas.value.single().syncEstado)
        assertEquals(1, dao.filas.value.size)
    }

    @Test
    fun error500_conservaLosDatosLocalesYQuedaPendiente() = probar {
        val (repo, _) = repositorio(conectado = true)
        advanceUntilIdle()
        servidor.modo = ServidorFalso.Modo.ERROR_500
        repo.crear(borrador())
        runCurrent()

        val estado = repo.estadoSincronizacion.value as EstadoSincronizacionGlobal.Fallida
        assertTrue(estado.recuperable)
        assertTrue(estado.mensaje.contains("500"))
        assertEquals(SyncEstado.PENDIENTE_CREAR, dao.filas.value.single().syncEstado)
        assertEquals(1, repo.observarRecordatorios().first().size)
    }

    @Test
    fun rechazo422_marcaErrorYReintentarLoVuelveAEncolar() = probar {
        val (repo, _) = repositorio(conectado = true)
        advanceUntilIdle()
        servidor.modo = ServidorFalso.Modo.RECHAZAR
        val creado = repo.crear(borrador())
        advanceUntilIdle()

        val fila = dao.filas.value.single()
        assertEquals(SyncEstado.ERROR, fila.syncEstado)
        assertTrue(fila.ultimoErrorSync!!.contains("422"))
        assertTrue(repo.obtenerRecordatorio(creado.id)!!.sincronizacion is EstadoSincronizacion.ConError)

        servidor.modo = ServidorFalso.Modo.NORMAL
        repo.reintentarSincronizacion(creado.id)
        advanceUntilIdle()
        assertEquals(SyncEstado.SINCRONIZADO, dao.filas.value.single().syncEstado)
        assertEquals(1, servidor.datos.size)
    }

    @Test
    fun cambioDuranteElEnvio_noSeMarcaComoSincronizadoYSeEnviaLaVersionNueva() = probar {
        val (repo, _) = repositorio(conectado = true)
        advanceUntilIdle()
        servidor.modo = ServidorFalso.Modo.SIN_RED
        val creado = repo.crear(borrador("Versión 1"))
        runCurrent()
        servidor.modo = ServidorFalso.Modo.NORMAL
        servidor.duranteElPut = {
            servidor.duranteElPut = null
            repo.actualizar(creado.id, borrador("Versión 2"))
        }
        repo.sincronizar("prueba")
        advanceUntilIdle()

        assertEquals("Versión 2", servidor.datos.values.single().titulo)
        assertEquals(SyncEstado.SINCRONIZADO, dao.filas.value.single().syncEstado)
        assertEquals(1, servidor.datos.size)
    }

    @Test
    fun eliminarSinConexion_ocultaYLuegoBorraEnElServicio() = probar {
        val (repo, monitor) = repositorio(conectado = true)
        val creado = repo.crear(borrador())
        advanceUntilIdle()
        assertEquals(1, servidor.datos.size)

        monitor.estado.value = false
        advanceUntilIdle()
        repo.eliminar(creado.id)
        advanceUntilIdle()
        assertTrue(repo.observarRecordatorios().first().isEmpty())
        assertEquals(SyncEstado.PENDIENTE_ELIMINAR, dao.filas.value.single().syncEstado)
        assertEquals(1, servidor.datos.size)

        monitor.estado.value = true
        advanceUntilIdle()
        assertTrue(servidor.datos.isEmpty())
        assertTrue(dao.filas.value.isEmpty())
    }

    @Test
    fun bajada_insertaLoRemotoYRespetaLosCambiosLocalesPendientes() = probar {
        servidor.datos["r-1"] = RecordatorioDto(id = "r-1", titulo = "Remoto uno", categoriaId = 1, prioridad = "MEDIA")
        servidor.datos["r-2"] = RecordatorioDto(id = "r-2", titulo = "Remoto dos", categoriaId = 2, prioridad = "BAJA")
        servidor.datos["malo"] = RecordatorioDto(id = "malo", titulo = "x", categoriaId = 1, prioridad = "URGENTE")
        val (repo, monitor) = repositorio(conectado = true)
        advanceUntilIdle()
        assertEquals("el registro inválido se descarta", 2, dao.filas.value.size)

        // Edición local sin red: el servidor cambia el mismo registro, pero el cambio local prevalece.
        monitor.estado.value = false
        advanceUntilIdle()
        val local = dao.filas.value.first { it.uuid == "r-1" }
        repo.actualizar(local.id, borrador("Editado en el teléfono"))
        servidor.datos["r-1"] = servidor.datos.getValue("r-1").copy(titulo = "Editado en otro lado")
        monitor.estado.value = true
        advanceUntilIdle()

        assertEquals("Editado en el teléfono", servidor.datos.getValue("r-1").titulo)
        assertEquals("Editado en el teléfono", dao.filas.value.first { it.uuid == "r-1" }.titulo)
        assertNull(dao.filas.value.firstOrNull { it.uuid == "malo" })
        assertEquals(3, servidor.datos.size)
    }
}
