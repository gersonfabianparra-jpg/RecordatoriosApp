package com.example.recordatorios.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.ui.components.BotonVolver
import com.example.recordatorios.ui.components.EstadoCarga
import com.example.recordatorios.ui.components.EstadoError
import com.example.recordatorios.ui.components.EstadoVacio
import com.example.recordatorios.ui.components.EtiquetaChip
import com.example.recordatorios.ui.components.FilaDato
import com.example.recordatorios.ui.components.colorEstado
import com.example.recordatorios.ui.components.colorPrioridad
import com.example.recordatorios.ui.components.formatoFechaLarga
import com.example.recordatorios.ui.components.textoEstado
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun DetalleRoute(
    onVolver: () -> Unit,
    viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetalleScreen(uiState = uiState, onVolver = onVolver, onReintentar = viewModel::onReintentar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    uiState: DetalleUiState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = { BotonVolver(onVolver = onVolver) }
            )
        }
    ) { innerPadding ->
        val contenido = Modifier.padding(innerPadding)
        when (uiState) {
            DetalleUiState.Cargando -> EstadoCarga("Cargando recordatorio…", contenido)
            DetalleUiState.NoEncontrado -> EstadoVacio(
                titulo = "No encontramos este recordatorio",
                mensaje = "Puede que ya no exista en la fuente de datos.",
                textoAccion = "Volver al listado",
                onAccion = onVolver,
                modifier = contenido
            )
            is DetalleUiState.Error -> EstadoError(uiState.mensaje, onReintentar, contenido)
            is DetalleUiState.ConDatos -> ContenidoDetalle(uiState.recordatorio, uiState.estilo, contenido)
        }
    }
}

@Composable
private fun ContenidoDetalle(recordatorio: Recordatorio, estilo: EstiloCategoria, modifier: Modifier) {
    val colorCategoria = Color(estilo.colorArgb)
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colorCategoria, MaterialTheme.shapes.small)
        )
        Text(
            recordatorio.titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textDecoration = if (recordatorio.estado is EstadoRecordatorio.Completado) TextDecoration.LineThrough else null,
            modifier = Modifier.semantics { heading() }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EtiquetaChip(estilo.etiqueta, colorCategoria)
            EtiquetaChip("▲ ${recordatorio.prioridad.nombre}", colorPrioridad(recordatorio.prioridad))
            EtiquetaChip(textoEstado(recordatorio.estado), colorEstado(recordatorio.estado))
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FilaDato("Descripción", recordatorio.descripcion ?: "Sin descripción")
                HorizontalDivider()
                FilaDato("Categoría", estilo.etiqueta)
                HorizontalDivider()
                FilaDato("Prioridad", "${recordatorio.prioridad.nombre} (nivel ${recordatorio.prioridad.nivel} de 3)")
                HorizontalDivider()
                FilaDato("Estado", estadoDetallado(recordatorio.estado))
                HorizontalDivider()
                FilaDato(
                    "Fecha límite",
                    recordatorio.fechaLimite?.format(formatoFechaLarga) ?: "Sin fecha límite"
                )
                HorizontalDivider()
                FilaDato("Aviso", recordatorio.aviso?.let { "${it.minutosAntes} minutos antes" } ?: "Sin aviso")
                HorizontalDivider()
                FilaDato(
                    "Ubicación",
                    recordatorio.ubicacion?.let { "${it.nombre} (${it.latitud}, ${it.longitud})" } ?: "Sin ubicación"
                )
                HorizontalDivider()
                FilaDato("Identificador", "#${recordatorio.id}")
            }
        }
    }
}

private fun estadoDetallado(estado: EstadoRecordatorio): String = when (estado) {
    EstadoRecordatorio.Pendiente -> "Pendiente"
    EstadoRecordatorio.Vencido -> "Vencido"
    is EstadoRecordatorio.Completado -> "Completado el ${estado.completadoEn.format(formatoFechaLarga)}"
    is EstadoRecordatorio.Pospuesto -> "Pospuesto hasta el ${estado.hasta.format(formatoFechaLarga)}"
}

@Preview(showBackground = true)
@Composable
private fun DetallePreview() {
    val recordatorio = DatosSimulados.recordatorios[1]
    RecordatoriosTheme {
        DetalleScreen(
            DetalleUiState.ConDatos(recordatorio, DatosSimulados.estilosPorCategoria.getValue(recordatorio.categoria)),
            onVolver = {}, onReintentar = {}
        )
    }
}
