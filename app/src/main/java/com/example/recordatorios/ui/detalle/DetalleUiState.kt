package com.example.recordatorios.ui.detalle

import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Recordatorio

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data class ConDatos(val recordatorio: Recordatorio, val estilo: EstiloCategoria) : DetalleUiState
    data object NoEncontrado : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
}
