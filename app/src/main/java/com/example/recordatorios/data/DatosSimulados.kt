package com.example.recordatorios.data

import com.example.recordatorios.model.Aviso
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.model.Ubicacion
import java.time.LocalDateTime

/**
 * Fuente de datos simulada (sin persistencia ni red en esta etapa).
 * Muestra el uso de List, Set y Map de Kotlin.
 */
object DatosSimulados {

    private val trabajo = Categoria(1, "Trabajo")
    private val salud = Categoria(2, "Salud")
    private val hogar = Categoria(3, "Hogar")
    private val estudio = Categoria(4, "Estudio")
    private val compras = Categoria(5, "Compras")

    /**
     * Lista "cruda" de categorías tal como podría llegar de varias fuentes:
     * contiene repetidos a propósito.
     */
    private val categoriasRecibidas: List<Categoria> = listOf(
        trabajo, salud, hogar, trabajo, estudio, Categoria(2, "Salud"), compras, hogar
    )

    /** Set: elimina las categorías repetidas (data class compara por valor). */
    val categorias: Set<Categoria> = categoriasRecibidas.toSet()

    /** Map: asocia cada categoría con su etiqueta y color. */
    val estilosPorCategoria: Map<Categoria, EstiloCategoria> = mapOf(
        trabajo to EstiloCategoria("💼 Trabajo", 0xFF3F51B5),
        salud to EstiloCategoria("🩺 Salud", 0xFFE53935),
        hogar to EstiloCategoria("🏠 Hogar", 0xFF43A047),
        estudio to EstiloCategoria("📚 Estudio", 0xFFFB8C00),
        compras to EstiloCategoria("🛒 Compras", 0xFF8E24AA)
    )

    private val ahora: LocalDateTime = LocalDateTime.now()

    /** List: colección ordenada de recordatorios simulados. */
    val recordatorios: List<Recordatorio> = listOf(
        Recordatorio(
            id = 1, titulo = "Entregar informe trimestral", categoria = trabajo,
            prioridad = Prioridad.Alta,
            descripcion = "Enviar al área de planificación regional",
            fechaLimite = ahora.plusDays(1), aviso = Aviso(60)
        ),
        Recordatorio(
            id = 2, titulo = "Turno con el médico", categoria = salud,
            prioridad = Prioridad.Alta,
            fechaLimite = ahora.plusDays(3), aviso = Aviso(120),
            ubicacion = Ubicacion("Centro de salud", -34.6037, -58.3816)
        ),
        Recordatorio(
            id = 3, titulo = "Pagar factura de luz", categoria = hogar,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Vencido,
            fechaLimite = ahora.minusDays(2)
        ),
        Recordatorio(
            id = 4, titulo = "Repasar Kotlin: sealed classes", categoria = estudio,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Completado(ahora.minusHours(5))
        ),
        Recordatorio(
            id = 5, titulo = "Comprar frutas y verduras", categoria = compras,
            prioridad = Prioridad.Baja,
            ubicacion = Ubicacion("Mercado central", -34.6158, -58.4333)
        ),
        Recordatorio(
            id = 6, titulo = "Reunión de equipo", categoria = trabajo,
            prioridad = Prioridad.Media,
            estado = EstadoRecordatorio.Pospuesto(ahora.plusDays(2)),
            descripcion = "Revisar avance del módulo 1"
        ),
        Recordatorio(
            id = 7, titulo = "Regar las plantas", categoria = hogar,
            prioridad = Prioridad.Baja,
            estado = EstadoRecordatorio.Completado(ahora.minusDays(1))
        ),
        Recordatorio(
            id = 8, titulo = "Retirar análisis de laboratorio", categoria = salud,
            prioridad = Prioridad.Media,
            fechaLimite = ahora.plusDays(5)
        )
    )
}
