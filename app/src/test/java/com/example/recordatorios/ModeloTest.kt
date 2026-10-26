package com.example.recordatorios

import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.RepositorioRecordatorios
import com.example.recordatorios.model.Aviso
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.model.ResultadoValidacion
import com.example.recordatorios.model.Ubicacion
import com.example.recordatorios.model.ValidadorRecordatorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeloTest {

    private val trabajo = Categoria(1, "Trabajo")

    @Test(expected = IllegalArgumentException::class)
    fun tituloVacio_lanzaExcepcion() {
        Recordatorio(id = 1, titulo = "", categoria = trabajo, prioridad = Prioridad.Alta)
    }

    @Test(expected = IllegalArgumentException::class)
    fun copyInvalido_tambienSeValida() {
        val r = Recordatorio(id = 1, titulo = "Ok", categoria = trabajo, prioridad = Prioridad.Alta)
        r.copy(titulo = "x".repeat(ValidadorRecordatorio.TITULO_MAX + 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun latitudFueraDeRango_lanzaExcepcion() {
        Ubicacion("Polo", 95.0, 0.0)
    }

    @Test
    fun avisoSinFecha_esInvalido() {
        val resultado = ValidadorRecordatorio.crear(
            id = 2, titulo = "Llamar", categoria = trabajo, prioridad = Prioridad.Media, aviso = Aviso(10)
        )
        assertTrue(resultado is ResultadoValidacion.Invalido)
    }

    @Test
    fun setEliminaCategoriasRepetidas() {
        assertEquals(5, DatosSimulados.categorias.size)
    }

    @Test
    fun todaCategoriaTieneEstiloEnElMap() {
        assertTrue(DatosSimulados.categorias.all { it in DatosSimulados.estilosPorCategoria })
    }

    @Test
    fun filtrosPorEstadoYPrioridad() {
        val repo = RepositorioRecordatorios()
        assertTrue(repo.porPrioridad(Prioridad.Alta).all { it.prioridad == Prioridad.Alta })
        assertEquals(2, repo.porEstado("Completado").size)
        assertEquals(1, repo.filtrar("Vencido", Prioridad.Media).size)
    }
}
