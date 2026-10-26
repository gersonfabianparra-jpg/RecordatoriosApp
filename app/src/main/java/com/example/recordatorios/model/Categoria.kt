package com.example.recordatorios.model

/**
 * Categoría de un recordatorio. Al ser data class, dos categorías con el mismo
 * id y nombre son iguales: eso permite que un Set elimine las repetidas.
 */
data class Categoria(
    val id: Int,
    val nombre: String
) {
    init {
        require(id > 0) { "El id de la categoría debe ser positivo" }
        require(nombre.isNotBlank()) { "La categoría necesita un nombre" }
    }
}

/** Etiqueta visible y color (ARGB) asociados a una categoría mediante un Map. */
data class EstiloCategoria(
    val etiqueta: String,
    val colorArgb: Long
)
