package com.example.recordatorios.model

/** Estados posibles de la pantalla principal. */
sealed interface RecordatoriosUiState {
    data object Cargando : RecordatoriosUiState
    data object Vacio : RecordatoriosUiState
    data class Exito(val recordatorios: List<Recordatorio>) : RecordatoriosUiState
    data class Error(val mensaje: String) : RecordatoriosUiState
}
