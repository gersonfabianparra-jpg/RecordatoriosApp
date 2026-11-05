package com.example.recordatorios.ui.lista

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.data.ModoSimulacion
import com.example.recordatorios.data.conteoPorEstado
import com.example.recordatorios.data.ordenadosPorPrioridad
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.ui.components.EstadoCarga
import com.example.recordatorios.ui.components.EstadoError
import com.example.recordatorios.ui.components.EstadoVacio
import com.example.recordatorios.ui.components.RecordatorioCard
import com.example.recordatorios.ui.theme.RecordatoriosTheme

/** Conecta el ViewModel con la pantalla. La recolección respeta el ciclo de vida. */
@Composable
fun ListaRoute(
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    viewModel: ListaViewModel = viewModel(factory = ListaViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val modoSimulacion by viewModel.modoSimulacion.collectAsStateWithLifecycle()
    ListaScreen(
        uiState = uiState,
        modoSimulacion = modoSimulacion,
        onCrearRecordatorio = onCrearRecordatorio,
        onRecordatorioClick = onRecordatorioClick,
        onReintentar = viewModel::onReintentar,
        onModoSimulacion = viewModel::onModoSimulacionElegido
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaScreen(
    uiState: ListaUiState,
    modoSimulacion: ModoSimulacion,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (Long) -> Unit,
    onReintentar: () -> Unit,
    onModoSimulacion: (ModoSimulacion) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis recordatorios", fontWeight = FontWeight.Bold)
                        Text(
                            "Simulación: ${modoSimulacion.etiqueta}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = { MenuSimulacion(modoSimulacion, onModoSimulacion) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            // En el estado vacío la acción principal ya está en el centro.
            if (uiState is ListaUiState.ConDatos) {
                ExtendedFloatingActionButton(
                    onClick = onCrearRecordatorio,
                    icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
                    text = { Text("Nuevo") }
                )
            }
        }
    ) { innerPadding ->
        val contenido = Modifier.padding(innerPadding)
        when (uiState) {
            ListaUiState.Cargando -> EstadoCarga("Cargando recordatorios…", contenido)
            ListaUiState.Vacio -> EstadoVacio(
                titulo = "Todavía no hay recordatorios",
                mensaje = "Creá el primero para empezar a organizar tus tareas.",
                textoAccion = "Crear recordatorio",
                onAccion = onCrearRecordatorio,
                modifier = contenido
            )
            is ListaUiState.Error -> EstadoError(uiState.mensaje, onReintentar, contenido)
            is ListaUiState.ConDatos -> LazyColumn(
                modifier = contenido.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
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

@Composable
private fun MenuSimulacion(actual: ModoSimulacion, onElegir: (ModoSimulacion) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { abierto = true }) { Text("Simular ▾") }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            ModoSimulacion.entries.forEach { modo ->
                DropdownMenuItem(
                    text = { Text((if (modo == actual) "✓ " else "    ") + modo.etiqueta) },
                    onClick = {
                        abierto = false
                        onElegir(modo)
                    }
                )
            }
        }
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
            ModoSimulacion.NORMAL, {}, {}, {}, {}
        )
    }
}

@Preview(showBackground = true, name = "Vacío")
@Composable
private fun ListaVaciaPreview() {
    RecordatoriosTheme { ListaScreen(ListaUiState.Vacio, ModoSimulacion.VACIO, {}, {}, {}, {}) }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun ListaErrorPreview() {
    RecordatoriosTheme {
        ListaScreen(ListaUiState.Error("No se pudo conectar."), ModoSimulacion.ERROR_CARGA, {}, {}, {}, {})
    }
}
