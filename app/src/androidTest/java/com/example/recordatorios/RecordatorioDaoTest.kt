package com.example.recordatorios

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.recordatorios.data.local.RecordatorioDao
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.local.RecordatoriosDatabase
import com.example.recordatorios.data.local.SyncEstado
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Pruebas del DAO sobre una base Room real (en memoria) en el emulador. */
@RunWith(AndroidJUnit4::class)
class RecordatorioDaoTest {

    private lateinit var db: RecordatoriosDatabase
    private lateinit var dao: RecordatorioDao

    @Before
    fun crear() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            RecordatoriosDatabase::class.java
        ).build()
        dao = db.recordatorioDao()
    }

    @After
    fun cerrar() = db.close()

    private fun fila(uuid: String, titulo: String, prioridad: Int, estado: String = "PENDIENTE", creado: Long = 1, limite: Long? = null) =
        RecordatorioEntity(
            uuid = uuid, titulo = titulo, descripcion = null, categoriaId = 1, prioridad = prioridad,
            estado = estado, estadoFecha = null, fechaLimite = limite, avisoMinutos = null,
            ubicacionNombre = null, latitud = null, longitud = null,
            creadoEn = creado, actualizadoEn = creado, syncEstado = SyncEstado.PENDIENTE_CREAR
        )

    @Test
    fun insertarActualizarYEliminar() = runBlocking<Unit> {
        val id = dao.insertar(fila("a", "Primera tarea", 2))
        assertEquals("Primera tarea", dao.obtenerPorId(id)!!.titulo)
        dao.actualizar(dao.obtenerPorId(id)!!.copy(titulo = "Tarea editada"))
        assertEquals("Tarea editada", dao.observarPorId(id).first()!!.titulo)
        dao.borrarPorId(id)
        assertNull(dao.obtenerPorId(id))
    }

    @Test
    fun uuidRepetido_noSePuedeInsertar() = runBlocking<Unit> {
        dao.insertar(fila("mismo", "Primera tarea", 2))
        try {
            dao.insertar(fila("mismo", "Duplicada", 2))
            fail("Se esperaba un error por uuid duplicado")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
        }
        assertEquals(1, dao.observarTodos().first().size)
    }

    @Test
    fun filtroPorPrioridadEstadoYOrden() = runBlocking<Unit> {
        dao.insertar(fila("1", "Beta", 3, creado = 1, limite = 300))
        dao.insertar(fila("2", "alfa", 1, creado = 2, limite = 100))
        dao.insertar(fila("3", "Gamma", 3, estado = "COMPLETADO", creado = 3))
        assertEquals(listOf("Gamma", "Beta", "alfa"), dao.observarFiltrados(null, true, "PRIORIDAD").first().map { it.titulo })
        assertEquals(listOf("Beta"), dao.observarFiltrados(3, false, "PRIORIDAD").first().map { it.titulo })
        assertEquals(listOf("alfa", "Beta", "Gamma"), dao.observarFiltrados(null, true, "TITULO").first().map { it.titulo })
        assertEquals(listOf("alfa", "Beta", "Gamma"), dao.observarFiltrados(null, true, "FECHA_LIMITE").first().map { it.titulo })
    }

    @Test
    fun confirmacionSoloParaLaVersionEnviada() = runBlocking<Unit> {
        val id = dao.insertar(fila("v", "Versionado", 2))
        dao.actualizar(dao.obtenerPorId(id)!!.copy(version = 2, syncEstado = SyncEstado.PENDIENTE_ACTUALIZAR))
        assertEquals(0, dao.marcarSincronizado(id, version = 1))
        assertEquals(SyncEstado.PENDIENTE_ACTUALIZAR, dao.obtenerPorId(id)!!.syncEstado)
        assertEquals(1, dao.marcarSincronizado(id, version = 2))
        assertEquals(SyncEstado.SINCRONIZADO, dao.obtenerPorId(id)!!.syncEstado)
        assertEquals(0, dao.observarCantidadSinSincronizar().first())
    }

    @Test
    fun fusionRemota_noDuplicaYRespetaPendientes() = runBlocking<Unit> {
        val pendiente = dao.insertar(fila("p", "Pendiente local", 2))
        val remotoPendiente = fila("p", "Versión del servidor", 2).copy(syncEstado = SyncEstado.SINCRONIZADO)
        val nuevo = fila("n", "Nuevo remoto", 1).copy(syncEstado = SyncEstado.SINCRONIZADO)
        val resumen = dao.fusionarRemotos(listOf(remotoPendiente, nuevo))
        assertEquals(1, resumen.insertados)
        assertEquals(1, resumen.conservadosPorCambioLocal)
        assertEquals("Pendiente local", dao.obtenerPorId(pendiente)!!.titulo)
        dao.fusionarRemotos(listOf(remotoPendiente, nuevo))
        assertEquals(2, dao.todosParaSincronizar().size)
        assertTrue(dao.todosParaSincronizar().map { it.uuid }.containsAll(listOf("p", "n")))
    }
}
