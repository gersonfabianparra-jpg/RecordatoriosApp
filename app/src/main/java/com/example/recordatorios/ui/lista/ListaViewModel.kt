package com.example.recordatorios.ui.lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.RecordatoriosApplication
import com.example.recordatorios.data.ControlSimulacion
import com.example.recordatorios.data.ModoSimulacion
import com.example.recordatorios.data.RepositorioRecordatorios
import com.example.recordatorios.data.conteoPorEstado
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListaViewModel(
    private val repositorio: RepositorioRecordatorios,
    private val simulacion: ControlSimulacion
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListaUiState>(ListaUiState.Cargando)
    val uiState: StateFlow<ListaUiState> = _uiState.asStateFlow()

    private val _modoSimulacion = MutableStateFlow(simulacion.modo)
    val modoSimulacion: StateFlow<ModoSimulacion> = _modoSimulacion.asStateFlow()

    private var carga: Job? = null

    init {
        cargar()
    }

    fun onReintentar() = cargar()

    fun onModoSimulacionElegido(modo: ModoSimulacion) {
        simulacion.cambiarModo(modo)
        _modoSimulacion.update { modo }
        cargar()
    }

    private fun cargar() {
        carga?.cancel()
        _uiState.update { ListaUiState.Cargando }
        carga = viewModelScope.launch {
            repositorio.observarRecordatorios()
                .catch { e ->
                    _uiState.update {
                        ListaUiState.Error("No pudimos cargar los recordatorios. ${e.message.orEmpty()}".trim())
                    }
                }
                .collect { lista ->
                    _uiState.update {
                        if (lista.isEmpty()) {
                            ListaUiState.Vacio
                        } else {
                            ListaUiState.ConDatos(lista, repositorio.estilos, lista.conteoPorEstado())
                        }
                    }
                }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val contenedor = (this[APPLICATION_KEY] as RecordatoriosApplication).contenedor
                ListaViewModel(contenedor.repositorio, contenedor.simulacion)
            }
        }
    }
}
