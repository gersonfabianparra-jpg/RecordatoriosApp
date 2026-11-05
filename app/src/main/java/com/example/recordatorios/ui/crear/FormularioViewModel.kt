package com.example.recordatorios.ui.crear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.RecordatoriosApplication
import com.example.recordatorios.data.BorradorRecordatorio
import com.example.recordatorios.data.RepositorioRecordatorios
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.ValidadorRecordatorio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FormularioViewModel(
    private val repositorio: RepositorioRecordatorios
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FormularioUiState(
            categoriasDisponibles = repositorio.categorias.sortedBy { it.nombre },
            estilos = repositorio.estilos
        )
    )
    val uiState: StateFlow<FormularioUiState> = _uiState.asStateFlow()

    /** Campos que la persona ya editó: recién ahí se muestran sus errores. */
    private var tocados: Set<Campo> = emptySet()

    fun onTituloChange(valor: String) = actualizar(Campo.TITULO) { it.copy(titulo = valor) }

    fun onDescripcionChange(valor: String) = actualizar(Campo.DESCRIPCION) { it.copy(descripcion = valor) }

    fun onCategoriaSeleccionada(categoria: Categoria) =
        actualizar(Campo.CATEGORIA) { it.copy(categoria = categoria) }

    fun onPrioridadSeleccionada(prioridad: Prioridad) =
        actualizar(Campo.PRIORIDAD) { it.copy(prioridad = prioridad) }

    fun onErrorDescartado() {
        _uiState.update { if (it.envio is EstadoEnvio.Error) it.copy(envio = EstadoEnvio.Inactivo) else it }
    }

    fun onGuardar() {
        val actual = _uiState.value
        if (actual.enviando) return
        if (!actual.esValido) {
            // Mostrar todos los errores pendientes si se intenta guardar igual.
            tocados = Campo.entries.toSet()
            _uiState.update { revalidar(it) }
            return
        }
        _uiState.update { it.copy(envio = EstadoEnvio.Enviando) }
        viewModelScope.launch {
            try {
                val creado = repositorio.guardar(
                    BorradorRecordatorio(
                        titulo = actual.titulo,
                        descripcion = actual.descripcion,
                        categoria = actual.categoria!!,
                        prioridad = actual.prioridad!!
                    )
                )
                _uiState.update { it.copy(envio = EstadoEnvio.Exito(creado.id)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(envio = EstadoEnvio.Error("${e.message ?: "Ocurrió un problema inesperado."} Tus datos siguen en el formulario."))
                }
            }
        }
    }

    private fun actualizar(campo: Campo, cambio: (FormularioUiState) -> FormularioUiState) {
        if (_uiState.value.enviando) return
        tocados = tocados + campo
        _uiState.update { revalidar(cambio(it)) }
    }

    private fun revalidar(estado: FormularioUiState): FormularioUiState {
        val todos = validar(estado)
        val visibles = ErroresFormulario(
            titulo = todos.titulo.takeIf { Campo.TITULO in tocados },
            descripcion = todos.descripcion.takeIf { Campo.DESCRIPCION in tocados },
            categoria = todos.categoria.takeIf { Campo.CATEGORIA in tocados },
            prioridad = todos.prioridad.takeIf { Campo.PRIORIDAD in tocados }
        )
        return estado.copy(errores = visibles, esValido = !todos.hayErrores)
    }

    private fun validar(estado: FormularioUiState) = ErroresFormulario(
        titulo = ValidadorRecordatorio.validarTitulo(estado.titulo),
        descripcion = ValidadorRecordatorio.validarDescripcion(estado.descripcion),
        categoria = when {
            estado.categoria == null -> "Elegí una categoría"
            estado.categoria !in repositorio.categorias -> "La categoría no es válida"
            else -> null
        },
        prioridad = if (estado.prioridad == null) "Elegí una prioridad" else null
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                FormularioViewModel((this[APPLICATION_KEY] as RecordatoriosApplication).contenedor.repositorio)
            }
        }
    }
}
