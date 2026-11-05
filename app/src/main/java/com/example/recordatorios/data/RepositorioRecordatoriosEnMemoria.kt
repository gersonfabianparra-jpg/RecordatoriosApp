package com.example.recordatorios.data

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Recordatorio
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Implementación en memoria. Las demoras imitan una fuente lenta para que se
 * vean los estados de carga y envío; el modo de simulación fuerza vacío o errores.
 */
class RepositorioRecordatoriosEnMemoria(
    iniciales: List<Recordatorio> = DatosSimulados.recordatorios,
    override val categorias: Set<Categoria> = DatosSimulados.categorias,
    override val estilos: Map<Categoria, EstiloCategoria> = DatosSimulados.estilosPorCategoria,
    private val demoraMs: Long = 1_200
) : RepositorioRecordatorios, ControlSimulacion {

    private val almacen = MutableStateFlow(iniciales)

    /** Recordatorios apartados mientras se simula la lista vacía. */
    private var apartados: List<Recordatorio> = emptyList()

    override var modo: ModoSimulacion = ModoSimulacion.NORMAL
        private set

    override fun cambiarModo(modo: ModoSimulacion) {
        val anterior = this.modo
        this.modo = modo
        if (modo == ModoSimulacion.VACIO && anterior != ModoSimulacion.VACIO) {
            apartados = almacen.value
            almacen.value = emptyList()
        } else if (anterior == ModoSimulacion.VACIO && modo != ModoSimulacion.VACIO) {
            almacen.update { creadosMientrasVacio -> apartados + creadosMientrasVacio }
            apartados = emptyList()
        }
    }

    override fun observarRecordatorios(): Flow<List<Recordatorio>> = flow {
        delay(demoraMs)
        if (modo == ModoSimulacion.ERROR_CARGA) throw ErrorSimulado("No se pudo conectar con la fuente de datos.")
        emitAll(almacen.map { lista -> lista.ordenadosPorPrioridad() })
    }

    override suspend fun obtenerRecordatorio(id: Long): Recordatorio? {
        delay(demoraMs / 2)
        if (modo == ModoSimulacion.ERROR_CARGA) throw ErrorSimulado("No se pudo cargar el recordatorio.")
        return almacen.value.firstOrNull { it.id == id }
    }

    override suspend fun guardar(borrador: BorradorRecordatorio): Recordatorio {
        delay(demoraMs)
        if (modo == ModoSimulacion.ERROR_GUARDADO) throw ErrorSimulado("El servidor simulado rechazó el guardado.")
        lateinit var nuevo: Recordatorio
        almacen.update { lista ->
            nuevo = Recordatorio(
                id = ((lista + apartados).maxOfOrNull { it.id } ?: 0) + 1,
                titulo = borrador.titulo.trim(),
                descripcion = borrador.descripcion?.trim()?.ifBlank { null },
                categoria = borrador.categoria,
                prioridad = borrador.prioridad
            )
            lista + nuevo
        }
        return nuevo
    }
}
