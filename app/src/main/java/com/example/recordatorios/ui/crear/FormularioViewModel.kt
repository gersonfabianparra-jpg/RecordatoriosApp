package com.example.recordatorios.ui.crear

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recordatorios.RecordatoriosApplication
import com.example.recordatorios.data.BorradorRecordatorio
import com.example.recordatorios.data.RepositorioRecordatorios
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.ValidadorRecordatorio
import com.example.recordatorios.ui.navegacion.Rutas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Formulario de creación y edición. Guardar escribe en Room a través del repositorio,
 * así que funciona igual con o sin conexión: la sincronización ocurre después.
 */
class FormularioViewModel(
    private val repositorio: RepositorioRecordatorios,
    /** null = crear; un id = editar ese recordatorio. */
    private val idEdicion: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FormularioUiState(
            categoriasDisponibles = repositorio.categorias.sortedBy { it.nombre },
            estilos = repositorio.estilos,
            modoEdicion = idEdicion != null,
            cargando = idEdicion != null
        )
    )
    val uiState: StateFlow<FormularioUiState> = _uiState.asStateFlow()

    /** Campos que la persona ya editó: recién ahí se muestran sus errores. */
    private var tocados: Set<Campo> = emptySet()

    init {
        if (idEdicion != null) cargarParaEditar(idEdicion)
    }

    private fun cargarParaEditar(id: Long) {
        viewModelScope.launch {
            val existente = try {
                repositorio.obtenerRecordatorio(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (existente == null) {
                _uiState.update {
                    it.copy(cargando = false, envio = EstadoEnvio.Error("No se encontró el recordatorio a editar."))
                }
                return@launch
            }
            tocados = Campo.entries.toSet()
            _uiState.update {
                revalidar(
                    it.copy(
                        titulo = existente.titulo,
                        descripcion = existente.descripcion.orEmpty(),
                        categoria = existente.categoria,
                        prioridad = existente.prioridad,
                        fechaLimite = existente.fechaLimite,
                        cargando = false
                    )
                )
            }
        }
    }

    fun onTituloChange(valor: String) = actualizar(Campo.TITULO) { it.copy(titulo = valor) }

    fun onDescripcionChange(valor: String) = actualizar(Campo.DESCRIPCION) { it.copy(descripcion = valor) }

    fun onCategoriaSeleccionada(categoria: Categoria) =
        actualizar(Campo.CATEGORIA) { it.copy(categoria = categoria) }

    fun onPrioridadSeleccionada(prioridad: Prioridad) =
        actualizar(Campo.PRIORIDAD) { it.copy(prioridad = prioridad) }

    /** Conserva la hora si ya había fecha; si no, usa las 09:00. */
    fun onFechaSeleccionada(fecha: LocalDate?) = actualizar(Campo.FECHA) { estado ->
        estado.copy(fechaLimite = fecha?.atTime(estado.fechaLimite?.toLocalTime() ?: LocalTime.of(9, 0)))
    }

    fun onErrorDescartado() {
        _uiState.update { if (it.envio is EstadoEnvio.Error) it.copy(envio = EstadoEnvio.Inactivo) else it }
    }

    fun onGuardar() {
        val actual = _uiState.value
        if (actual.enviando || actual.cargando) return
        if (!actual.esValido) {
            tocados = Campo.entries.toSet()
            _uiState.update { revalidar(it) }
            return
        }
        _uiState.update { it.copy(envio = EstadoEnvio.Enviando) }
        viewModelScope.launch {
            try {
                val borrador = BorradorRecordatorio(
                    titulo = actual.titulo,
                    descripcion = actual.descripcion,
                    categoria = actual.categoria!!,
                    prioridad = actual.prioridad!!,
                    fechaLimite = actual.fechaLimite
                )
                val guardado = if (idEdicion == null) {
                    repositorio.crear(borrador)
                } else {
                    repositorio.actualizar(idEdicion, borrador)
                }
                _uiState.update { it.copy(envio = EstadoEnvio.Exito(guardado.id)) }
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
                val handle: SavedStateHandle = createSavedStateHandle()
                FormularioViewModel(
                    (this[APPLICATION_KEY] as RecordatoriosApplication).contenedor.repositorio,
                    handle.get<Long>(Rutas.ARG_ID)
                )
            }
        }
    }
}
