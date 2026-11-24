package com.example.recordatorios.ui.lista

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.EstadoSincronizacionGlobal
import com.example.recordatorios.data.conteoPorEstado
import com.example.recordatorios.data.ordenadosPorPrioridad
import com.example.recordatorios.data.preferencias.FiltroPrioridad
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.ui.components.EstadoCarga
import com.example.recordatorios.ui.components.EstadoError
import com.example.recordatorios.ui.components.EstadoVacio
import com.example.recordatorios.ui.components.RecordatorioCard
import com.example.recordatorios.ui.theme.RecordatoriosTheme
import java.time.format.DateTimeFormatter

/** Conecta el ViewModel con la pantalla. La recolección respeta el ciclo de vida. */
@Composable
fun ListaRoute(
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    onAbrirAjustes: () -> Unit,
    viewModel: ListaViewModel = viewModel(factory = ListaViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filtros by viewModel.filtros.collectAsStateWithLifecycle()
    val sincronizacion by viewModel.sincronizacion.collectAsStateWithLifecycle()
    ListaScreen(
        uiState = uiState,
        filtros = filtros,
        sincronizacion = sincronizacion,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onAbrirAjustes = onAbrirAjustes,
        onReintentar = viewModel::onReintentar,
        onSincronizar = viewModel::onSincronizar,
        onFiltroPrioridad = viewModel::onFiltroPrioridad,
        onMostrarCompletados = viewModel::onMostrarCompletados
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaScreen(
    uiState: ListaUiState,
    filtros: FiltrosUiState,
    sincronizacion: SincronizacionUiState,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    onAbrirAjustes: () -> Unit,
    onReintentar: () -> Unit,
    onSincronizar: () -> Unit,
    onFiltroPrioridad: (FiltroPrioridad) -> Unit,
    onMostrarCompletados: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mis recordatorios", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(
                        onClick = onSincronizar,
                        enabled = sincronizacion.estado !is EstadoSincronizacionGlobal.EnCurso
                    ) { Text("⟳ Sincronizar") }
                    TextButton(onClick = onAbrirAjustes) { Text("⚙") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState !is ListaUiState.Vacio) {
                ExtendedFloatingActionButton(
                    onClick = onCrearRecordatorio,
                    icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
                    text = { Text("Nuevo") }
                )
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            BannerSincronizacion(sincronizacion)
            FiltrosRapidos(filtros, onFiltroPrioridad, onMostrarCompletados)
            when (uiState) {
                ListaUiState.Cargando -> EstadoCarga("Leyendo recordatorios guardados…")
                is ListaUiState.Vacio -> if (uiState.hayFiltros) {
                    EstadoVacio(
                        titulo = "Nada para mostrar",
                        mensaje = "Hay filtros activos. Probá con «Todas» o mostrando los completados.",
                        textoAccion = "Quitar filtros",
                        onAccion = {
                            onFiltroPrioridad(FiltroPrioridad.TODAS)
                            onMostrarCompletados(true)
                        }
                    )
                } else {
                    EstadoVacio(
                        titulo = "Todavía no hay recordatorios",
                        mensaje = "Creá el primero. Se guarda en el dispositivo aunque no haya conexión.",
                        textoAccion = "Crear recordatorio",
                        onAccion = onCrearRecordatorio
                    )
                }
                is ListaUiState.Error -> EstadoError(uiState.mensaje, onReintentar)
                is ListaUiState.ConDatos -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { ResumenEstados(uiState.conteoPorEstado, uiState.recordatorios.size) }
                    items(uiState.recordatorios, key = { it.id }) { recordatorio ->
                        RecordatorioCard(
                            recordatorio = recordatorio,
                            estilo = uiState.estilos[recordatorio.categoria]
                                ?: EstiloCategoria(recordatorio.categoria.nombre, 0xFF757575),
                            onClick = { onRecordatorioClick(recordatorio.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

private val formatoHora: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/** Mensaje de sincronización: siempre con texto, nunca solo color. */
fun textoBanner(s: SincronizacionUiState): Pair<String, Boolean> {
    val pendientes = when (s.cambiosPendientes) {
        0 -> "sin cambios pendientes"
        1 -> "1 cambio pendiente"
        else -> "${s.cambiosPendientes} cambios pendientes"
    }
    return when {
        !s.conectado || s.estado is EstadoSincronizacionGlobal.SinConexion ->
            if (s.cambiosPendientes == 0) {
                "📴 Sin conexión · mostrando los datos guardados en el dispositivo." to true
            } else {
                "📴 Sin conexión · $pendientes. Se enviarán al recuperar la red." to true
            }
        s.estado is EstadoSincronizacionGlobal.EnCurso -> "⟳ Sincronizando… · $pendientes" to false
        s.estado is EstadoSincronizacionGlobal.Fallida ->
            "⚠ ${s.estado.mensaje}. ${if (s.estado.recuperable) "Se reintentará automáticamente" else "Revisá los elementos marcados"} · $pendientes" to true
        s.estado is EstadoSincronizacionGlobal.Completada -> {
            val c = s.estado
            val rechazados = if (c.rechazados > 0) " · ${c.rechazados} rechazado(s)" else ""
            "☁ Sincronizado a las ${c.en.format(formatoHora)}$rechazados · $pendientes" to (c.rechazados > 0 || s.cambiosPendientes > 0)
        }
        else -> "☁ Esperando sincronización · $pendientes" to false
    }
}

@Composable
private fun BannerSincronizacion(sincronizacion: SincronizacionUiState) {
    val (texto, alerta) = textoBanner(sincronizacion)
    Surface(
        color = if (alerta) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (alerta) Color(0xFF6D3A00) else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Text(texto, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
private fun FiltrosRapidos(
    filtros: FiltrosUiState,
    onFiltroPrioridad: (FiltroPrioridad) -> Unit,
    onMostrarCompletados: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FiltroPrioridad.entries.forEach { opcion ->
            FilterChip(
                selected = filtros.prioridad == opcion,
                onClick = { onFiltroPrioridad(opcion) },
                label = { Text(opcion.etiqueta) }
            )
        }
        // Altura fija: dentro de una fila desplazable, el divisor ocuparía todo el alto disponible.
        VerticalDivider(Modifier.height(28.dp))
        FilterChip(
            selected = filtros.mostrarCompletados,
            onClick = { onMostrarCompletados(!filtros.mostrarCompletados) },
            label = { Text(if (filtros.mostrarCompletados) "✓ Completados" else "Completados ocultos") }
        )
    }
}

@Composable
private fun ResumenEstados(conteo: Map<String, Int>, total: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Dato("Total", total)
        Dato("Pendientes", conteo["Pendiente"] ?: 0)
        Dato("Vencidos", conteo["Vencido"] ?: 0)
        Dato("Completados", conteo["Completado"] ?: 0)
    }
}

@Composable
private fun Dato(etiqueta: String, valor: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$valor",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, name = "Con datos")
@Composable
private fun ListaConDatosPreview() {
    val lista = DatosSimulados.recordatorios.ordenadosPorPrioridad()
    RecordatoriosTheme {
        ListaScreen(
            ListaUiState.ConDatos(lista, DatosSimulados.estilosPorCategoria, lista.conteoPorEstado()),
            FiltrosUiState(), SincronizacionUiState(EstadoSincronizacionGlobal.SinConexion, 2, false),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}
