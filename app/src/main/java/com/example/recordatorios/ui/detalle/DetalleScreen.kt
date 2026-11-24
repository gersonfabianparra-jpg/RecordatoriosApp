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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.recordatorios.model.EstadoSincronizacion
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Recordatorio
import com.example.recordatorios.ui.components.BannerError
import com.example.recordatorios.ui.components.BotonVolver
import com.example.recordatorios.ui.components.EstadoCarga
import com.example.recordatorios.ui.components.EstadoError
import com.example.recordatorios.ui.components.EstadoVacio
import com.example.recordatorios.ui.components.EtiquetaChip
import com.example.recordatorios.ui.components.EtiquetaSincronizacion
import com.example.recordatorios.ui.components.colorSincronizacion
import com.example.recordatorios.ui.components.FilaDato
import com.example.recordatorios.ui.components.colorEstado
import com.example.recordatorios.ui.components.colorPrioridad
import com.example.recordatorios.ui.components.formatoFechaLarga
import com.example.recordatorios.ui.components.textoEstado
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun DetalleRoute(
    onVolver: () -> Unit,
    onEditar: (Long) -> Unit,
    viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    // Tras eliminar, se vuelve al listado una sola vez.
    LaunchedEffect(uiState) {
        if (uiState is DetalleUiState.Eliminado) onVolver()
    }
    DetalleScreen(
        uiState = uiState,
        mensaje = mensaje,
        onVolver = onVolver,
        onReintentar = viewModel::onReintentar,
        onEditar = onEditar,
        onAlternarCompletado = viewModel::onAlternarCompletado,
        onEliminar = viewModel::onEliminar,
        onReintentarSincronizacion = viewModel::onReintentarSincronizacion,
        onMensajeDescartado = viewModel::onMensajeDescartado
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    uiState: DetalleUiState,
    mensaje: String?,
    onVolver: () -> Unit,
    onReintentar: () -> Unit,
    onEditar: (Long) -> Unit,
    onAlternarCompletado: (Boolean) -> Unit,
    onEliminar: () -> Unit,
    onReintentarSincronizacion: () -> Unit,
    onMensajeDescartado: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmarEliminacion by rememberSaveable { mutableStateOf(false) }
    if (confirmarEliminacion) {
        AlertDialog(
            onDismissRequest = { confirmarEliminacion = false },
            title = { Text("¿Eliminar recordatorio?") },
            text = { Text("Se borra del dispositivo ahora y del servicio en la próxima sincronización.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminacion = false
                    onEliminar()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminacion = false }) { Text("Cancelar") } }
        )
    }
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
            DetalleUiState.Eliminado -> EstadoCarga("Eliminando…", contenido)
            DetalleUiState.NoEncontrado -> EstadoVacio(
                titulo = "No encontramos este recordatorio",
                mensaje = "Puede que ya no exista en la fuente de datos.",
                textoAccion = "Volver al listado",
                onAccion = onVolver,
                modifier = contenido
            )
            is DetalleUiState.Error -> EstadoError(uiState.mensaje, onReintentar, contenido)
            is DetalleUiState.ConDatos -> ContenidoDetalle(
                recordatorio = uiState.recordatorio,
                estilo = uiState.estilo,
                mensaje = mensaje,
                onEditar = { onEditar(uiState.recordatorio.id) },
                onAlternarCompletado = onAlternarCompletado,
                onEliminar = { confirmarEliminacion = true },
                onReintentarSincronizacion = onReintentarSincronizacion,
                onMensajeDescartado = onMensajeDescartado,
                modifier = contenido
            )
        }
    }
}

@Composable
private fun ContenidoDetalle(
    recordatorio: Recordatorio,
    estilo: EstiloCategoria,
    mensaje: String?,
    onEditar: () -> Unit,
    onAlternarCompletado: (Boolean) -> Unit,
    onEliminar: () -> Unit,
    onReintentarSincronizacion: () -> Unit,
    onMensajeDescartado: () -> Unit,
    modifier: Modifier
) {
    val completado = recordatorio.estado is EstadoRecordatorio.Completado
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
                FilaDato("Creado", recordatorio.creadoEn?.format(formatoFechaLarga) ?: "—")
                HorizontalDivider()
                FilaDato("Identificador local", "#${recordatorio.id}")
            }
        }
        TarjetaSincronizacion(recordatorio.sincronizacion, onReintentarSincronizacion)
        if (mensaje != null) {
            BannerError(mensaje = mensaje, onReintentar = onMensajeDescartado, onDescartar = onMensajeDescartado)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEditar, modifier = Modifier.weight(1f)) { Text("✎ Editar") }
            Button(onClick = { onAlternarCompletado(!completado) }, modifier = Modifier.weight(1f)) {
                Text(if (completado) "↺ Reabrir" else "✓ Completar")
            }
        }
        OutlinedButton(
            onClick = onEliminar,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("🗑 Eliminar") }
    }
}

@Composable
private fun TarjetaSincronizacion(estado: EstadoSincronizacion, onReintentar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorSincronizacion(estado).copy(alpha = 0.10f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Sincronización", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            EtiquetaSincronizacion(estado)
            val detalle = when (estado) {
                EstadoSincronizacion.Sincronizado -> "El servicio remoto confirmó este registro."
                is EstadoSincronizacion.Pendiente -> "Guardado en el dispositivo. Se enviará automáticamente cuando haya conexión." +
                    (estado.ultimoError?.let { "\nÚltimo intento: $it" } ?: "")
                is EstadoSincronizacion.ConError -> estado.mensaje
            }
            Text(detalle, style = MaterialTheme.typography.bodySmall)
            if (estado is EstadoSincronizacion.ConError) {
                TextButton(onClick = onReintentar) { Text("Reintentar sincronización") }
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
            mensaje = null, onVolver = {}, onReintentar = {}, onEditar = {}, onAlternarCompletado = {},
            onEliminar = {}, onReintentarSincronizacion = {}, onMensajeDescartado = {}
        )
    }
}
