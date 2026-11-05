package com.example.recordatorios.data

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de la fuente de datos. Los ViewModels dependen solo de esta interfaz,
 * así que la implementación en memoria podrá cambiarse por Room o un servicio
 * remoto sin tocar pantallas ni componentes.
 */
interface RepositorioRecordatorios {

    /** Categorías válidas (Set: sin repetidas). */
    val categorias: Set<Categoria>

    /** Etiqueta y color de cada categoría. */
    val estilos: Map<Categoria, EstiloCategoria>

    /** Lista observable; vuelve a emitir cada vez que se guarda un recordatorio. */
    fun observarRecordatorios(): Flow<List<Recordatorio>>

    /** null si no existe un recordatorio con ese id. */
    suspend fun obtenerRecordatorio(id: Long): Recordatorio?

    /** Guarda y devuelve el recordatorio con el id asignado. */
    suspend fun guardar(borrador: BorradorRecordatorio): Recordatorio
}

/** Datos que ingresa la persona usuaria; el repositorio asigna el id. */
data class BorradorRecordatorio(
    val titulo: String,
    val descripcion: String?,
    val categoria: Categoria,
    val prioridad: Prioridad
)

/** Situaciones que se pueden forzar para comprobar cada estado de la interfaz. */
enum class ModoSimulacion(val etiqueta: String) {
    NORMAL("Datos normales"),
    VACIO("Lista vacía"),
    ERROR_CARGA("Error al cargar"),
    ERROR_GUARDADO("Error al guardar")
}

/** Permite cambiar el modo de simulación sin depender de la implementación. */
interface ControlSimulacion {
    val modo: ModoSimulacion
    fun cambiarModo(modo: ModoSimulacion)
}

class ErrorSimulado(mensaje: String) : Exception(mensaje)
