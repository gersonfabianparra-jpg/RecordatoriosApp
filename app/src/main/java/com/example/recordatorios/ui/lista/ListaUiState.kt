package com.example.recordatorios.ui.lista

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Recordatorio

/** Estados mutuamente excluyentes del listado: nunca hay carga, datos y error a la vez. */
sealed interface ListaUiState {
    data object Cargando : ListaUiState
    data object Vacio : ListaUiState
    data class ConDatos(
        val recordatorios: List<Recordatorio>,
        val estilos: Map<Categoria, EstiloCategoria>,
        val conteoPorEstado: Map<String, Int>
    ) : ListaUiState
    data class Error(val mensaje: String) : ListaUiState
}
