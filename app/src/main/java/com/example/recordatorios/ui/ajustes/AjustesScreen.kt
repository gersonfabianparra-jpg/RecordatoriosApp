package com.example.recordatorios.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.preferencias.FiltroPrioridad
import com.example.recordatorios.data.preferencias.OrdenListado
import com.example.recordatorios.data.preferencias.PreferenciasUsuario
import com.example.recordatorios.data.preferencias.TemaApp
import com.example.recordatorios.ui.components.BotonVolver
import com.example.recordatorios.ui.components.EstadoCarga
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun AjustesRoute(
    onVolver: () -> Unit,
    viewModel: AjustesViewModel = viewModel(factory = AjustesViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AjustesScreen(
        uiState = uiState,
        onVolver = onVolver,
        onTema = viewModel::onTema,
        onMostrarCompletados = viewModel::onMostrarCompletados,
        onOrden = viewModel::onOrden,
        onFiltroPrioridad = viewModel::onFiltroPrioridad,
        onRestablecer = viewModel::onRestablecer,
        onGuardarTemaInvalido = viewModel::onGuardarTemaInvalido
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    uiState: AjustesUiState,
    onVolver: () -> Unit,
    onTema: (TemaApp) -> Unit,
    onMostrarCompletados: (Boolean) -> Unit,
    onOrden: (OrdenListado) -> Unit,
    onFiltroPrioridad: (FiltroPrioridad) -> Unit,
    onRestablecer: () -> Unit,
    onGuardarTemaInvalido: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Ajustes") }, navigationIcon = { BotonVolver(onVolver = onVolver) }) }
    ) { innerPadding ->
        if (uiState.cargando) {
            EstadoCarga("Leyendo preferencias…", Modifier.padding(innerPadding))
            return@Scaffold
        }
        val p = uiState.preferencias
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Seccion("Apariencia") {
                GrupoOpciones(TemaApp.entries, p.tema, { it.etiqueta }, onTema)
            }
            Seccion("Listado") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = p.mostrarCompletados, role = Role.Switch, onValueChange = onMostrarCompletados)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mostrar recordatorios completados", Modifier.weight(1f))
                    Switch(checked = p.mostrarCompletados, onCheckedChange = null)
                }
                HorizontalDivider()
                Text("Ordenar por", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                GrupoOpciones(OrdenListado.entries, p.orden, { it.etiqueta }, onOrden)
                HorizontalDivider()
                Text("Filtro de prioridad", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                GrupoOpciones(FiltroPrioridad.entries, p.filtroPrioridad, { it.etiqueta }, onFiltroPrioridad)
            }
            Seccion("Estado de las preferencias (DataStore)") {
                val predeterminadas = if (p.predeterminadas.isEmpty()) "ninguna" else p.predeterminadas.sorted().joinToString()
                Text("Usan el valor predeterminado: $predeterminadas", style = MaterialTheme.typography.bodySmall)
                if (p.invalidas.isEmpty()) {
                    Text("Valores inválidos: ninguno", style = MaterialTheme.typography.bodySmall)
                } else {
                    p.invalidas.forEach {
                        Text("⚠ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (p.errorLectura) {
                    Text("⚠ No se pudo leer el archivo de preferencias: se aplican los valores predeterminados.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Seccion("Servicio remoto") {
                Text("URL base: ${uiState.urlServicio}", style = MaterialTheme.typography.bodySmall)
                Text("Se configura en local.properties (ver config.example.properties).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Seccion("Herramientas de prueba") {
                Text("Guarda «MORADO» como tema para comprobar cómo responde la app ante un dato inválido.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    OutlinedButton(onClick = onGuardarTemaInvalido) { Text("Guardar tema inválido") }
                    OutlinedButton(onClick = onRestablecer) { Text("Restablecer") }
                }
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
            contenido()
        }
    }
}

@Composable
private fun <T> GrupoOpciones(opciones: List<T>, seleccionada: T, etiqueta: (T) -> String, onElegir: (T) -> Unit) {
    Column(Modifier.selectableGroup()) {
        opciones.forEach { opcion ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = opcion == seleccionada, role = Role.RadioButton, onClick = { onElegir(opcion) })
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = opcion == seleccionada, onClick = null)
                Text(etiqueta(opcion), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AjustesPreview() {
    RecordatoriosTheme {
        AjustesScreen(
            AjustesUiState(false, PreferenciasUsuario(predeterminadas = setOf("orden")), "http://10.0.2.2:8080/"),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}
