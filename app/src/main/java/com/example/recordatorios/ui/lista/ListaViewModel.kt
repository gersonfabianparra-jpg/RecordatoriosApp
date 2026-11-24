package com.example.recordatorios.ui.lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.RecordatoriosApplication
import com.example.recordatorios.data.FiltroListado
import com.example.recordatorios.data.RepositorioRecordatorios
import com.example.recordatorios.data.conteoPorEstado
import com.example.recordatorios.data.preferencias.FiltroPrioridad
import com.example.recordatorios.data.preferencias.FuentePreferencias
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Combina las preferencias (DataStore) con el listado (Room). Cuando cambia un filtro,
 * flatMapLatest cancela la consulta anterior y se suscribe a la nueva.
 * El ViewModel no sabe si hay red: muestra siempre lo que hay en Room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListaViewModel(
    private val repositorio: RepositorioRecordatorios,
    private val preferencias: FuentePreferencias
) : ViewModel() {

    private val recargas = MutableStateFlow(0)

    private val filtro = preferencias.preferencias
        .map { FiltroListado(it.filtroPrioridad.prioridad, it.mostrarCompletados, it.orden) }
        .distinctUntilChanged()

    val uiState: StateFlow<ListaUiState> = combine(filtro, recargas) { f, _ -> f }
        .flatMapLatest { f ->
            repositorio.observarRecordatorios(f)
                .map<_, ListaUiState> { lista ->
                    if (lista.isEmpty()) {
                        ListaUiState.Vacio(hayFiltros = f.prioridad != null || !f.mostrarCompletados)
                    } else {
                        ListaUiState.ConDatos(lista, repositorio.estilos, lista.conteoPorEstado())
                    }
                }
                .catch { e ->
                    emit(ListaUiState.Error("No pudimos leer los recordatorios guardados. ${e.message.orEmpty()}".trim()))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListaUiState.Cargando)

    val filtros: StateFlow<FiltrosUiState> = preferencias.preferencias
        .map { FiltrosUiState(it.filtroPrioridad, it.mostrarCompletados) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FiltrosUiState())

    val sincronizacion: StateFlow<SincronizacionUiState> = combine(
        repositorio.estadoSincronizacion,
        repositorio.cambiosSinSincronizar.onStart { emit(0) },
        repositorio.conectado.onStart { emit(true) }
    ) { estado, pendientes, conectado -> SincronizacionUiState(estado, pendientes, conectado) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SincronizacionUiState())

    fun onReintentar() = recargas.update { it + 1 }

    fun onSincronizar() = repositorio.solicitarSincronizacion("botón Sincronizar")

    fun onFiltroPrioridad(filtro: FiltroPrioridad) {
        viewModelScope.launch { preferencias.cambiarFiltroPrioridad(filtro) }
    }

    fun onMostrarCompletados(mostrar: Boolean) {
        viewModelScope.launch { preferencias.cambiarMostrarCompletados(mostrar) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val contenedor = (this[APPLICATION_KEY] as RecordatoriosApplication).contenedor
                ListaViewModel(contenedor.repositorio, contenedor.preferencias)
            }
        }
    }
}
