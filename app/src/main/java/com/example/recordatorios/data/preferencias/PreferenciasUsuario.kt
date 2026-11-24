package com.example.recordatorios.data.preferencias

import com.example.recordatorios.model.Prioridad

/** Tema visual elegido por la persona usuaria. */
enum class TemaApp(val etiqueta: String) {
    SISTEMA("Según el sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}

/** Criterio de ordenamiento del listado. El nombre se usa también en la consulta de Room. */
enum class OrdenListado(val etiqueta: String) {
    PRIORIDAD("Prioridad (alta primero)"),
    FECHA_LIMITE("Fecha límite (más próxima primero)"),
    TITULO("Título (A–Z)"),
    CREACION("Más recientes primero")
}

/** Filtro de prioridad del listado. */
enum class FiltroPrioridad(val etiqueta: String, val prioridad: Prioridad?) {
    TODAS("Todas", null),
    ALTA("Alta", Prioridad.Alta),
    MEDIA("Media", Prioridad.Media),
    BAJA("Baja", Prioridad.Baja)
}

/**
 * Modelo propio de preferencias: la interfaz nunca ve claves ni valores crudos de DataStore.
 *
 * [predeterminadas] indica qué preferencias no tenían un valor guardado y usan el valor por defecto.
 * [invalidas] describe valores guardados que no se pudieron interpretar (y que se reemplazaron por el
 * valor predeterminado). Se usan para mostrar y documentar el comportamiento en la pantalla de Ajustes.
 */
data class PreferenciasUsuario(
    val tema: TemaApp = TemaApp.SISTEMA,
    val mostrarCompletados: Boolean = true,
    val orden: OrdenListado = OrdenListado.PRIORIDAD,
    val filtroPrioridad: FiltroPrioridad = FiltroPrioridad.TODAS,
    val predeterminadas: Set<String> = emptySet(),
    val invalidas: List<String> = emptyList(),
    val errorLectura: Boolean = false
)
