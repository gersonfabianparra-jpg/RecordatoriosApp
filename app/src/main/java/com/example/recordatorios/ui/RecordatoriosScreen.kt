package com.example.recordatorios.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recordatorios.R
import com.example.recordatorios.data.RepositorioRecordatorios
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.RecordatoriosUiState
import com.example.recordatorios.ui.components.RecordatorioCard
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordatoriosScreen(repositorio: RepositorioRecordatorios = remember { RepositorioRecordatorios() }) {
    var filtroEstado by rememberSaveable { mutableStateOf<String?>(null) }
    var filtroPrioridadNivel by rememberSaveable { mutableStateOf<Int?>(null) }
    val filtroPrioridad = Prioridad.todas.firstOrNull { it.nivel == filtroPrioridadNivel }

    val uiState: RecordatoriosUiState = try {
        val lista = repositorio.filtrar(filtroEstado, filtroPrioridad)
        if (lista.isEmpty()) RecordatoriosUiState.Vacio else RecordatoriosUiState.Exito(lista)
    } catch (e: IllegalArgumentException) {
        RecordatoriosUiState.Error(e.message ?: "Datos inválidos")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.titulo_pantalla), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.subtitulo_pantalla),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Resumen(repositorio.conteoPorEstado(), repositorio.categorias.size)

            // Filtro por estado
            FilaFiltros(
                opciones = EstadoRecordatorio.nombres,
                seleccionada = filtroEstado,
                onSeleccion = { filtroEstado = it }
            )
            // Filtro por prioridad
            FilaFiltros(
                opciones = Prioridad.todas.map { it.nombre },
                seleccionada = filtroPrioridad?.nombre,
                onSeleccion = { nombre ->
                    filtroPrioridadNivel = Prioridad.todas.firstOrNull { it.nombre == nombre }?.nivel
                }
            )

            when (uiState) {
                RecordatoriosUiState.Cargando -> Centrado { CircularProgressIndicator() }
                RecordatoriosUiState.Vacio -> Centrado { Text(stringResource(R.string.lista_vacia)) }
                is RecordatoriosUiState.Error -> Centrado {
                    Text(uiState.mensaje, color = MaterialTheme.colorScheme.error)
                }
                is RecordatoriosUiState.Exito -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.recordatorios, key = { it.id }) { recordatorio ->
                        RecordatorioCard(
                            recordatorio = recordatorio,
                            estilo = repositorio.estiloDe(recordatorio.categoria)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Resumen(conteo: Map<String, Int>, totalCategorias: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Dato("Pendientes", conteo["Pendiente"] ?: 0)
        Dato("Vencidos", conteo["Vencido"] ?: 0)
        Dato("Completados", conteo["Completado"] ?: 0)
        Dato("Categorías", totalCategorias)
    }
}

@Composable
private fun Dato(etiqueta: String, valor: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$valor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FilaFiltros(opciones: List<String>, seleccionada: String?, onSeleccion: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = seleccionada == null,
                onClick = { onSeleccion(null) },
                label = { Text(stringResource(R.string.filtro_todos)) }
            )
        }
        items(opciones) { opcion ->
            FilterChip(
                selected = seleccionada == opcion,
                onClick = { onSeleccion(if (seleccionada == opcion) null else opcion) },
                label = { Text(opcion) }
            )
        }
    }
}

@Composable
private fun Centrado(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RecordatoriosScreenPreview() {
    RecordatoriosTheme { RecordatoriosScreen() }
}
