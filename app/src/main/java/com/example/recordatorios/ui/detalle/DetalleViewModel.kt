package com.example.recordatorios.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.RecordatoriosApplication
import com.example.recordatorios.data.RepositorioRecordatorios
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.ui.navegacion.Rutas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetalleViewModel(
    savedStateHandle: SavedStateHandle,
    private val repositorio: RepositorioRecordatorios
) : ViewModel() {

    /** Único dato recibido por navegación: el identificador. */
    private val recordatorioId: Long = checkNotNull(savedStateHandle[Rutas.ARG_ID])

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun onReintentar() = cargar()

    private fun cargar() {
        _uiState.update { DetalleUiState.Cargando }
        viewModelScope.launch {
            val nuevo = try {
                val recordatorio = repositorio.obtenerRecordatorio(recordatorioId)
                if (recordatorio == null) {
                    DetalleUiState.NoEncontrado
                } else {
                    val estilo = repositorio.estilos[recordatorio.categoria]
                        ?: EstiloCategoria(recordatorio.categoria.nombre, 0xFF757575)
                    DetalleUiState.ConDatos(recordatorio, estilo)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetalleUiState.Error("No pudimos abrir el recordatorio. ${e.message.orEmpty()}".trim())
            }
            _uiState.update { nuevo }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                DetalleViewModel(
                    createSavedStateHandle(),
                    (this[APPLICATION_KEY] as RecordatoriosApplication).contenedor.repositorio
                )
            }
        }
    }
}
