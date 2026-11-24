package com.example.recordatorios.ui.lista

import com.example.recordatorios.data.EstadoSincronizacionGlobal
import com.example.recordatorios.data.preferencias.FiltroPrioridad
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Recordatorio

/** Estados mutuamente excluyentes del contenido del listado. */
sealed interface ListaUiState {
    data object Cargando : ListaUiState
    /** [hayFiltros]: la base tiene datos, pero los filtros activos los ocultan. */
    data class Vacio(val hayFiltros: Boolean) : ListaUiState
    data class ConDatos(
        val recordatorios: List<Recordatorio>,
        val estilos: Map<Categoria, EstiloCategoria>,
        val conteoPorEstado: Map<String, Int>
    ) : ListaUiState
    data class Error(val mensaje: String) : ListaUiState
}

/** Filtros rápidos del listado (vienen de DataStore). */
data class FiltrosUiState(
    val prioridad: FiltroPrioridad = FiltroPrioridad.TODAS,
    val mostrarCompletados: Boolean = true
)

/** Información de sincronización para el banner superior. */
data class SincronizacionUiState(
    val estado: EstadoSincronizacionGlobal = EstadoSincronizacionGlobal.Inactiva,
    val cambiosPendientes: Int = 0,
    val conectado: Boolean = true
)
