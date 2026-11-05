package com.example.recordatorios.ui.crear

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Prioridad

/** Estado completo del formulario de creación. Inmutable: cada cambio crea una copia. */
data class FormularioUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null,
    val categoriasDisponibles: List<Categoria> = emptyList(),
    val estilos: Map<Categoria, EstiloCategoria> = emptyMap(),
    /** Mensajes visibles: solo de los campos que la persona ya tocó. */
    val errores: ErroresFormulario = ErroresFormulario(),
    val esValido: Boolean = false,
    val envio: EstadoEnvio = EstadoEnvio.Inactivo
) {
    val enviando: Boolean get() = envio is EstadoEnvio.Enviando
    val puedeGuardar: Boolean get() = esValido && !enviando
}

data class ErroresFormulario(
    val titulo: String? = null,
    val descripcion: String? = null,
    val categoria: String? = null,
    val prioridad: String? = null
) {
    val hayErrores: Boolean get() = listOfNotNull(titulo, descripcion, categoria, prioridad).isNotEmpty()
}

/** Ciclo del guardado: excluyente, por eso es sealed. */
sealed interface EstadoEnvio {
    data object Inactivo : EstadoEnvio
    data object Enviando : EstadoEnvio
    data class Exito(val idCreado: Long) : EstadoEnvio
    data class Error(val mensaje: String) : EstadoEnvio
}

enum class Campo { TITULO, DESCRIPCION, CATEGORIA, PRIORIDAD }
