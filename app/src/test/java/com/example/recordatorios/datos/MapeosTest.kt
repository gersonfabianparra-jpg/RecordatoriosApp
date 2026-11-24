package com.example.recordatorios.datos

import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.local.RecordatorioEntity
import com.example.recordatorios.data.local.SyncEstado
import com.example.recordatorios.data.mapeo.aDominio
import com.example.recordatorios.data.mapeo.aDto
import com.example.recordatorios.data.mapeo.aEntidadOrNull
import com.example.recordatorios.data.mapeo.aEpochMillis
import com.example.recordatorios.data.remoto.RecordatorioDto
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstadoSincronizacion
import com.example.recordatorios.model.OperacionPendiente
import com.example.recordatorios.model.Prioridad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class MapeosTest {

    private val creado = LocalDateTime.of(2026, 11, 20, 10, 30).aEpochMillis()
    private val entidad = RecordatorioEntity(
        id = 7, uuid = "abc-123", titulo = "Pagar expensas", descripcion = "Antes del 10",
        categoriaId = 3, prioridad = 3, estado = "COMPLETADO", estadoFecha = creado + 3_600_000,
        fechaLimite = LocalDateTime.of(2026, 12, 10, 9, 0).aEpochMillis(), avisoMinutos = 30,
        ubicacionNombre = null, latitud = null, longitud = null,
        creadoEn = creado, actualizadoEn = creado + 3_600_000, syncEstado = SyncEstado.SINCRONIZADO
    )

    @Test
    fun entidadADominio_traduceTodosLosCampos() {
        val r = entidad.aDominio(DatosSimulados.categorias)
        assertEquals(7L, r.id)
        assertEquals("Hogar", r.categoria.nombre)
        assertEquals(Prioridad.Alta, r.prioridad)
        assertTrue(r.estado is EstadoRecordatorio.Completado)
        assertEquals(30, r.aviso?.minutosAntes)
        assertEquals(EstadoSincronizacion.Sincronizado, r.sincronizacion)
    }

    @Test
    fun estadoDeSincronizacion_seTraduceAlDominio() {
        val pendiente = entidad.copy(syncEstado = SyncEstado.PENDIENTE_ELIMINAR, intentosSync = 2).aDominio(DatosSimulados.categorias)
        assertEquals(EstadoSincronizacion.Pendiente(OperacionPendiente.ELIMINAR, 2, null), pendiente.sincronizacion)
        val error = entidad.copy(syncEstado = SyncEstado.ERROR, ultimoErrorSync = "HTTP 422").aDominio(DatosSimulados.categorias)
        assertEquals(EstadoSincronizacion.ConError("HTTP 422"), error.sincronizacion)
    }

    @Test
    fun entidadDtoEntidad_idaYVueltaSinPerdidas() {
        val dto = entidad.aDto()
        assertEquals("abc-123", dto.id)
        assertEquals("ALTA", dto.prioridad)
        assertEquals("2026-12-10T09:00", dto.fechaLimite)
        val deVuelta = dto.aEntidadOrNull(ahora = 0)!!
        assertEquals(entidad.copy(id = 0), deVuelta)
    }

    @Test
    fun dtoInvalido_seDescarta() {
        val valido = RecordatorioDto(id = "x", titulo = "Título válido", categoriaId = 1, prioridad = "BAJA")
        assertTrue(valido.aEntidadOrNull(0) != null)
        assertNull(valido.copy(prioridad = "URGENTE").aEntidadOrNull(0))
        assertNull(valido.copy(titulo = "ab").aEntidadOrNull(0))
        assertNull(valido.copy(estado = "BORRADO").aEntidadOrNull(0))
        assertNull(valido.copy(fechaLimite = "mañana").aEntidadOrNull(0))
        assertNull(valido.copy(categoriaId = 0).aEntidadOrNull(0))
    }
}
