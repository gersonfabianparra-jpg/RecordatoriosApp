package com.example.recordatorios.data

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio

/** Operaciones de consulta sobre las colecciones simuladas. */
class RepositorioRecordatorios(
    private val recordatorios: List<Recordatorio> = DatosSimulados.recordatorios,
    val categorias: Set<Categoria> = DatosSimulados.categorias,
    private val estilos: Map<Categoria, EstiloCategoria> = DatosSimulados.estilosPorCategoria
) {
    /** Todos los recordatorios, primero los de mayor prioridad. */
    fun todos(): List<Recordatorio> =
        recordatorios.sortedWith(compareByDescending<Recordatorio> { it.prioridad.nivel }.thenBy { it.id })

    fun porEstado(nombreEstado: String): List<Recordatorio> =
        todos().filter { it.estado.nombre == nombreEstado }

    fun porPrioridad(prioridad: Prioridad): List<Recordatorio> =
        todos().filter { it.prioridad == prioridad }

    /** Aplica ambos filtros; null significa "sin filtrar". */
    fun filtrar(nombreEstado: String?, prioridad: Prioridad?): List<Recordatorio> =
        todos().filter { r ->
            (nombreEstado == null || r.estado.nombre == nombreEstado) &&
                (prioridad == null || r.prioridad == prioridad)
        }

    fun pendientes(): List<Recordatorio> =
        todos().filter { it.estado is EstadoRecordatorio.Pendiente }

    /** Map de conteo: cuántos recordatorios hay en cada estado. */
    fun conteoPorEstado(): Map<String, Int> =
        recordatorios.groupingBy { it.estado.nombre }.eachCount()

    /** Map de agrupación: recordatorios por categoría. */
    fun agrupadosPorCategoria(): Map<Categoria, List<Recordatorio>> =
        recordatorios.groupBy { it.categoria }

    fun estiloDe(categoria: Categoria): EstiloCategoria =
        estilos[categoria] ?: EstiloCategoria(categoria.nombre, 0xFF757575)
}
