package com.example.recordatorios.model

/**
 * Prioridad cerrada: solo existen estos tres valores, por lo que un `when`
 * sobre Prioridad es exhaustivo y el compilador avisa si falta un caso.
 */
sealed class Prioridad(val nivel: Int, val nombre: String) {
    data object Alta : Prioridad(3, "Alta")
    data object Media : Prioridad(2, "Media")
    data object Baja : Prioridad(1, "Baja")

    companion object {
        val todas: List<Prioridad> = listOf(Alta, Media, Baja)
    }
}
