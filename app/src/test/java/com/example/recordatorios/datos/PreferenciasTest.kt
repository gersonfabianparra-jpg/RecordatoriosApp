package com.example.recordatorios.datos

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import com.example.recordatorios.data.preferencias.ClavesPreferencias
import com.example.recordatorios.data.preferencias.FiltroPrioridad
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.preferencias.RepositorioPreferencias.Companion.leerPreferencias
import com.example.recordatorios.data.preferencias.TemaApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenciasTest {

    @Test
    fun sinDatosGuardados_usaLosValoresPredeterminados() {
        val p = leerPreferencias(emptyPreferences())
        assertEquals(TemaApp.SISTEMA, p.tema)
        assertTrue(p.mostrarCompletados)
        assertEquals(OrdenListado.PRIORIDAD, p.orden)
        assertEquals(FiltroPrioridad.TODAS, p.filtroPrioridad)
        assertEquals(setOf("tema", "orden", "filtro_prioridad", "mostrar_completados"), p.predeterminadas)
        assertTrue(p.invalidas.isEmpty())
    }

    @Test
    fun valoresGuardados_seConservan() {
        val p = leerPreferencias(
            preferencesOf(
                ClavesPreferencias.TEMA to "OSCURO",
                ClavesPreferencias.MOSTRAR_COMPLETADOS to false,
                ClavesPreferencias.ORDEN to "TITULO",
                ClavesPreferencias.FILTRO_PRIORIDAD to "ALTA"
            )
        )
        assertEquals(TemaApp.OSCURO, p.tema)
        assertFalse(p.mostrarCompletados)
        assertEquals(OrdenListado.TITULO, p.orden)
        assertEquals(FiltroPrioridad.ALTA, p.filtroPrioridad)
        assertTrue(p.predeterminadas.isEmpty())
    }

    @Test
    fun valorDesconocido_seReemplazaPorElPredeterminadoYSeInforma() {
        val p = leerPreferencias(preferencesOf(ClavesPreferencias.TEMA to "MORADO", ClavesPreferencias.ORDEN to "FECHA_LIMITE"))
        assertEquals(TemaApp.SISTEMA, p.tema)
        assertEquals(OrdenListado.FECHA_LIMITE, p.orden)
        assertEquals(1, p.invalidas.size)
        assertTrue(p.invalidas.single().contains("MORADO"))
    }
}
