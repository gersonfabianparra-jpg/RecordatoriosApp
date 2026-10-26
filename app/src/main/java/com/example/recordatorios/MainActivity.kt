package com.example.recordatorios

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.RepositorioRecordatorios
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.ResultadoValidacion
import com.example.recordatorios.model.ValidadorRecordatorio
import com.example.recordatorios.ui.RecordatoriosScreen
import com.example.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registrarDiagnostico()
        setContent {
            RecordatoriosTheme {
                RecordatoriosScreen()
            }
        }
    }

    /** Deja en Logcat (filtro: tag:Recordatorios) evidencia de colecciones y validaciones. */
    private fun registrarDiagnostico() {
        val repo = RepositorioRecordatorios()
        Log.d(TAG, "Recordatorios cargados: ${DatosSimulados.recordatorios.size}")
        Log.d(TAG, "Categorías únicas (Set): ${repo.categorias.map { it.nombre }}")
        Log.d(TAG, "Conteo por estado (Map): ${repo.conteoPorEstado()}")
        Log.d(TAG, "Prioridad alta: ${repo.porPrioridad(Prioridad.Alta).map { it.titulo }}")

        val intento = ValidadorRecordatorio.crear(
            id = 99, titulo = "   ", categoria = Categoria(1, "Trabajo"), prioridad = Prioridad.Baja
        )
        when (intento) {
            is ResultadoValidacion.Valido -> Log.d(TAG, "Válido: ${intento.recordatorio}")
            is ResultadoValidacion.Invalido -> Log.w(TAG, "Recordatorio rechazado: ${intento.errores}")
        }
    }

    private companion object {
        const val TAG = "Recordatorios"
    }
}
