package com.example.recordatorios

import com.example.recordatorios.data.BorradorRecordatorio
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.RepositorioRecordatoriosLocalFirst
import com.example.recordatorios.data.local.SyncEstado
import com.example.recordatorios.data.preferencias.FiltroPrioridad
import com.example.recordatorios.data.preferencias.FuentePreferencias
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.preferencias.PreferenciasUsuario
import com.example.recordatorios.data.preferencias.TemaApp
import com.example.recordatorios.datos.DaoEnMemoria
import com.example.recordatorios.datos.MonitorFalso
import com.example.recordatorios.datos.ServidorFalso
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.ui.crear.EstadoEnvio
import com.example.recordatorios.ui.crear.FormularioViewModel
import com.example.recordatorios.ui.lista.ListaUiState
import com.example.recordatorios.ui.lista.ListaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Preferencias en memoria (mismo contrato que DataStore). */
class PreferenciasFalsas : FuentePreferencias {
    val estado = MutableStateFlow(PreferenciasUsuario())
    override val preferencias = estado
    override suspend fun cambiarTema(tema: TemaApp) = estado.update { it.copy(tema = tema) }
    override suspend fun cambiarMostrarCompletados(mostrar: Boolean) = estado.update { it.copy(mostrarCompletados = mostrar) }
    override suspend fun cambiarOrden(orden: OrdenListado) = estado.update { it.copy(orden = orden) }
    override suspend fun cambiarFiltroPrioridad(filtro: FiltroPrioridad) = estado.update { it.copy(filtroPrioridad = filtro) }
    override suspend fun restablecer() = estado.update { PreferenciasUsuario() }
    override suspend fun guardarTemaInvalidoDePrueba() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = DaoEnMemoria()
    private val trabajo = Job()

    @Before
    fun configurar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() {
        trabajo.cancel()
        Dispatchers.resetMain()
    }

    private fun TestScope.repositorioSinRed() = RepositorioRecordatoriosLocalFirst(
        dao = dao, api = ServidorFalso(), monitor = MonitorFalso(false),
        alcance = CoroutineScope(coroutineContext.minusKey(Job) + trabajo)
    )

    @Test
    fun guardarSinConexion_terminaEnExitoYQuedaPendiente() = runTest(dispatcher) {
        val vm = FormularioViewModel(repositorioSinRed())
        assertFalse(vm.uiState.value.puedeGuardar)
        vm.onTituloChange("Llamar al banco")
        vm.onCategoriaSeleccionada(DatosSimulados.categorias.first())
        vm.onPrioridadSeleccionada(Prioridad.Alta)
        vm.onGuardar()
        assertEquals(EstadoEnvio.Enviando, vm.uiState.value.envio)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.envio is EstadoEnvio.Exito)
        assertEquals(SyncEstado.PENDIENTE_CREAR, dao.filas.value.single().syncEstado)
    }

    @Test
    fun edicion_cargaLosDatosYGuardaSobreElMismoRegistro() = runTest(dispatcher) {
        val repo = repositorioSinRed()
        val original = repo.crear(BorradorRecordatorio("Comprar pintura", "Blanca", DatosSimulados.categorias.first(), Prioridad.Baja))
        val vm = FormularioViewModel(repo, original.id)
        advanceUntilIdle()
        assertEquals("Comprar pintura", vm.uiState.value.titulo)
        assertTrue(vm.uiState.value.modoEdicion)
        vm.onTituloChange("Comprar pintura y rodillo")
        vm.onGuardar()
        advanceUntilIdle()
        assertEquals(1, dao.filas.value.size)
        assertEquals("Comprar pintura y rodillo", dao.filas.value.single().titulo)
        assertEquals(2L, dao.filas.value.single().version)
    }

    @Test
    fun listado_aplicaLosFiltrosGuardadosEnPreferencias() = runTest(dispatcher) {
        val repo = repositorioSinRed()
        val categoria = DatosSimulados.categorias.first()
        repo.crear(BorradorRecordatorio("Tarea alta", null, categoria, Prioridad.Alta))
        repo.crear(BorradorRecordatorio("Tarea baja", null, categoria, Prioridad.Baja))
        val completada = repo.crear(BorradorRecordatorio("Tarea completada", null, categoria, Prioridad.Alta))
        repo.cambiarCompletado(completada.id, true)
        val prefs = PreferenciasFalsas()
        val vm = ListaViewModel(repo, prefs)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(3, (vm.uiState.value as ListaUiState.ConDatos).recordatorios.size)

        vm.onFiltroPrioridad(FiltroPrioridad.ALTA)
        vm.onMostrarCompletados(false)
        advanceUntilIdle()
        assertEquals(listOf("Tarea alta"), (vm.uiState.value as ListaUiState.ConDatos).recordatorios.map { it.titulo })

        vm.onFiltroPrioridad(FiltroPrioridad.MEDIA)
        advanceUntilIdle()
        assertEquals(ListaUiState.Vacio(hayFiltros = true), vm.uiState.value)
    }
}
