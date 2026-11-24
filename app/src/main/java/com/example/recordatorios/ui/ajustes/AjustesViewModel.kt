package com.example.recordatorios.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.RecordatoriosApplication
import com.example.recordatorios.data.preferencias.FiltroPrioridad
import com.example.recordatorios.data.preferencias.FuentePreferencias
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.preferencias.PreferenciasUsuario
import com.example.recordatorios.data.preferencias.TemaApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AjustesUiState(
    val cargando: Boolean = true,
    val preferencias: PreferenciasUsuario = PreferenciasUsuario(),
    val urlServicio: String = ""
)

/** Lee y escribe preferencias. Cada escritura es una función suspendida en viewModelScope. */
class AjustesViewModel(
    private val preferencias: FuentePreferencias,
    urlServicio: String
) : ViewModel() {

    val uiState: StateFlow<AjustesUiState> = preferencias.preferencias
        .map { AjustesUiState(cargando = false, preferencias = it, urlServicio = urlServicio) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AjustesUiState(urlServicio = urlServicio))

    fun onTema(tema: TemaApp) = guardar { preferencias.cambiarTema(tema) }
    fun onMostrarCompletados(valor: Boolean) = guardar { preferencias.cambiarMostrarCompletados(valor) }
    fun onOrden(orden: OrdenListado) = guardar { preferencias.cambiarOrden(orden) }
    fun onFiltroPrioridad(filtro: FiltroPrioridad) = guardar { preferencias.cambiarFiltroPrioridad(filtro) }
    fun onRestablecer() = guardar { preferencias.restablecer() }
    fun onGuardarTemaInvalido() = guardar { preferencias.guardarTemaInvalidoDePrueba() }

    private fun guardar(accion: suspend () -> Unit) {
        viewModelScope.launch { accion() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val contenedor = (this[APPLICATION_KEY] as RecordatoriosApplication).contenedor
                AjustesViewModel(contenedor.preferencias, contenedor.urlServicio)
            }
        }
    }
}
