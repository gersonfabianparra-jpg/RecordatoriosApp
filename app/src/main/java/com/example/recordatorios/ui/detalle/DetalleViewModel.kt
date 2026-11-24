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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Observa el recordatorio en Room: si se modifica, se completa o la sincronización cambia
 * su estado, la pantalla se actualiza sola.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleViewModel(
    savedStateHandle: SavedStateHandle,
    private val repositorio: RepositorioRecordatorios
) : ViewModel() {

    /** Único dato recibido por navegación: el identificador. */
    private val recordatorioId: Long = checkNotNull(savedStateHandle[Rutas.ARG_ID])

    private val recargas = MutableStateFlow(0)
    private val eliminado = MutableStateFlow(false)

    private val _mensaje = MutableStateFlow<String?>(null)
    /** Error puntual de una acción (por ejemplo, almacenamiento lleno). */
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    val uiState: StateFlow<DetalleUiState> = recargas
        .flatMapLatest {
            combine(repositorio.observarRecordatorio(recordatorioId), eliminado) { recordatorio, borrado ->
                when {
                    borrado -> DetalleUiState.Eliminado
                    recordatorio == null -> DetalleUiState.NoEncontrado
                    else -> DetalleUiState.ConDatos(
                        recordatorio,
                        repositorio.estilos[recordatorio.categoria]
                            ?: EstiloCategoria(recordatorio.categoria.nombre, 0xFF757575)
                    )
                }
            }.catch { e -> emit(DetalleUiState.Error("No pudimos abrir el recordatorio. ${e.message.orEmpty()}".trim())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState.Cargando)

    fun onReintentar() = recargas.update { it + 1 }

    fun onAlternarCompletado(completado: Boolean) = ejecutar { repositorio.cambiarCompletado(recordatorioId, completado) }

    fun onEliminar() = ejecutar {
        repositorio.eliminar(recordatorioId)
        eliminado.value = true
    }

    fun onReintentarSincronizacion() = ejecutar { repositorio.reintentarSincronizacion(recordatorioId) }

    fun onMensajeDescartado() = _mensaje.update { null }

    private fun ejecutar(accion: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                accion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "No se pudo completar la acción."
            }
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
