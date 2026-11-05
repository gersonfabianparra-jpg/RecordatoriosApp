package com.example.recordatorios.data

import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio

// Consultas sobre colecciones de la Actividad 1, ahora como extensiones reutilizables.

fun List<Recordatorio>.ordenadosPorPrioridad(): List<Recordatorio> =
    sortedWith(compareByDescending<Recordatorio> { it.prioridad.nivel }.thenBy { it.id })

fun List<Recordatorio>.porEstado(nombreEstado: String): List<Recordatorio> =
    filter { it.estado.nombre == nombreEstado }

fun List<Recordatorio>.porPrioridad(prioridad: Prioridad): List<Recordatorio> =
    filter { it.prioridad == prioridad }

/** Map de conteo: cuántos recordatorios hay en cada estado. */
fun List<Recordatorio>.conteoPorEstado(): Map<String, Int> =
    groupingBy { it.estado.nombre }.eachCount()

fun List<Recordatorio>.agrupadosPorCategoria(): Map<Categoria, List<Recordatorio>> =
    groupBy { it.categoria }
