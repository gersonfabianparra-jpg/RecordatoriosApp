package com.example.recordatorios.model

import java.time.LocalDateTime

/** Ubicación opcional asociada a un recordatorio (módulos siguientes: geovallas). */
data class Ubicacion(
    val nombre: String,
    val latitud: Double,
    val longitud: Double
) {
    init {
        require(latitud in -90.0..90.0) { "Latitud fuera de rango: $latitud" }
        require(longitud in -180.0..180.0) { "Longitud fuera de rango: $longitud" }
    }
}

/** Aviso opcional: cuántos minutos antes de la fecha límite se notifica. */
data class Aviso(val minutosAntes: Int) {
    init {
        require(minutosAntes in 1..MAX_MINUTOS) { "El aviso debe estar entre 1 y $MAX_MINUTOS minutos" }
    }

    companion object {
        const val MAX_MINUTOS = 7 * 24 * 60
    }
}

/**
 * Recordatorio inmutable: todas sus propiedades son `val`. Para "modificarlo"
 * se crea una copia con `copy(...)`, y el bloque init vuelve a validar la copia,
 * de modo que nunca puede existir un Recordatorio en estado inválido.
 *
 * Los datos opcionales (descripción, fecha límite, ubicación, aviso) son tipos
 * anulables con valor por defecto null.
 */
data class Recordatorio(
    val id: Long,
    val titulo: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: EstadoRecordatorio = EstadoRecordatorio.Pendiente,
    val descripcion: String? = null,
    val fechaLimite: LocalDateTime? = null,
    val ubicacion: Ubicacion? = null,
    val aviso: Aviso? = null
) {
    init {
        val errores = ValidadorRecordatorio.errores(id, titulo, descripcion, fechaLimite, aviso)
        require(errores.isEmpty()) { errores.joinToString("; ") }
    }
}

/** Resultado de intentar crear un recordatorio sin lanzar excepciones. */
sealed class ResultadoValidacion {
    data class Valido(val recordatorio: Recordatorio) : ResultadoValidacion()
    data class Invalido(val errores: List<String>) : ResultadoValidacion()
}

/** Reglas de negocio iniciales definidas por la organización. */
object ValidadorRecordatorio {
    const val TITULO_MAX = 60
    const val DESCRIPCION_MAX = 250

    fun errores(
        id: Long,
        titulo: String,
        descripcion: String?,
        fechaLimite: LocalDateTime?,
        aviso: Aviso?
    ): List<String> = buildList {
        if (id <= 0) add("El id debe ser positivo")
        if (titulo.isBlank()) add("El título no puede estar vacío")
        if (titulo.length > TITULO_MAX) add("El título supera los $TITULO_MAX caracteres")
        if (descripcion != null && descripcion.length > DESCRIPCION_MAX) {
            add("La descripción supera los $DESCRIPCION_MAX caracteres")
        }
        if (aviso != null && fechaLimite == null) add("Un aviso requiere una fecha límite")
    }

    /** Versión segura para formularios: devuelve los errores en lugar de lanzar. */
    fun crear(
        id: Long,
        titulo: String,
        categoria: Categoria,
        prioridad: Prioridad,
        estado: EstadoRecordatorio = EstadoRecordatorio.Pendiente,
        descripcion: String? = null,
        fechaLimite: LocalDateTime? = null,
        ubicacion: Ubicacion? = null,
        aviso: Aviso? = null
    ): ResultadoValidacion {
        val errores = errores(id, titulo.trim(), descripcion, fechaLimite, aviso)
        return if (errores.isEmpty()) {
            ResultadoValidacion.Valido(
                Recordatorio(
                    id, titulo.trim(), categoria, prioridad, estado,
                    descripcion?.trim()?.ifBlank { null }, fechaLimite, ubicacion, aviso
                )
            )
        } else {
            ResultadoValidacion.Invalido(errores)
        }
    }
}
