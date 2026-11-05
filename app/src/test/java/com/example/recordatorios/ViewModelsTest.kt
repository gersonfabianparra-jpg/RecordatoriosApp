package com.example.recordatorios

import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.ModoSimulacion
import com.example.recordatorios.data.RepositorioRecordatoriosEnMemoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.ui.crear.EstadoEnvio
import com.example.recordatorios.ui.crear.FormularioViewModel
import com.example.recordatorios.ui.lista.ListaUiState
import com.example.recordatorios.ui.lista.ListaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun configurar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun listaPasaDeCargandoAConDatos() = runTest(dispatcher) {
        val vm = RepositorioRecordatoriosEnMemoria().let { repo -> ListaViewModel(repo, repo) }
        assertEquals(ListaUiState.Cargando, vm.uiState.value)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is ListaUiState.ConDatos)
    }

    @Test
    fun listaMuestraVacioYErrorSegunLaSimulacion() = runTest(dispatcher) {
        val repo = RepositorioRecordatoriosEnMemoria()
        val vm = ListaViewModel(repo, repo)
        vm.onModoSimulacionElegido(ModoSimulacion.VACIO)
        advanceUntilIdle()
        assertEquals(ListaUiState.Vacio, vm.uiState.value)
        vm.onModoSimulacionElegido(ModoSimulacion.ERROR_CARGA)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is ListaUiState.Error)
        vm.onModoSimulacionElegido(ModoSimulacion.NORMAL)
        vm.onReintentar()
        advanceUntilIdle()
        assertEquals(DatosSimulados.recordatorios.size, (vm.uiState.value as ListaUiState.ConDatos).recordatorios.size)
    }

    @Test
    fun guardarQuedaDeshabilitadoHastaQueElFormularioEsValido() = runTest(dispatcher) {
        val vm = FormularioViewModel(RepositorioRecordatoriosEnMemoria())
        assertFalse(vm.uiState.value.puedeGuardar)
        vm.onTituloChange("  abc ")
        assertTrue(vm.uiState.value.errores.titulo != null)
        vm.onTituloChange("Llamar al banco")
        vm.onCategoriaSeleccionada(DatosSimulados.categorias.first())
        vm.onPrioridadSeleccionada(Prioridad.Media)
        assertTrue(vm.uiState.value.puedeGuardar)
    }

    @Test
    fun guardarPasaPorEnviandoYTerminaEnExito() = runTest(dispatcher) {
        val repo = RepositorioRecordatoriosEnMemoria()
        val vm = FormularioViewModel(repo)
        vm.onTituloChange("Llamar al banco")
        vm.onCategoriaSeleccionada(DatosSimulados.categorias.first())
        vm.onPrioridadSeleccionada(Prioridad.Alta)
        vm.onGuardar()
        assertEquals(EstadoEnvio.Enviando, vm.uiState.value.envio)
        assertFalse(vm.uiState.value.puedeGuardar)
        advanceUntilIdle()
        assertEquals(EstadoEnvio.Exito(9), vm.uiState.value.envio)
    }

    @Test
    fun errorSimuladoAlGuardarSeInformaComoErrorGeneral() = runTest(dispatcher) {
        val repo = RepositorioRecordatoriosEnMemoria().apply { cambiarModo(ModoSimulacion.ERROR_GUARDADO) }
        val vm = FormularioViewModel(repo)
        vm.onTituloChange("Llamar al banco")
        vm.onCategoriaSeleccionada(DatosSimulados.categorias.first())
        vm.onPrioridadSeleccionada(Prioridad.Baja)
        vm.onGuardar()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.envio is EstadoEnvio.Error)
        assertTrue(vm.uiState.value.puedeGuardar)
    }
}
