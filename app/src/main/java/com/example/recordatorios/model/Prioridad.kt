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
        // by lazy: si la lista se creara al inicializar Prioridad, y lo primero en
        // usarse fuera Alta, Alta aún sería null en ese momento (ciclo de inicialización).
        val todas: List<Prioridad> by lazy { listOf(Alta, Media, Baja) }
    }
}
