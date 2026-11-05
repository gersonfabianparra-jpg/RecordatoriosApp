package com.example.recordatorios.ui.crear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatorios.data.DatosSimulados
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.ValidadorRecordatorio
import com.example.recordatorios.ui.components.BannerError
import com.example.recordatorios.ui.components.BotonVolver
import com.example.recordatorios.ui.components.CampoTextoValidado
import com.example.recordatorios.ui.components.SelectorCategoria
import com.example.recordatorios.ui.components.SelectorPrioridad
import com.example.recordatorios.ui.theme.RecordatoriosTheme

@Composable
fun CrearRecordatorioRoute(
    onGuardado: (Long) -> Unit,
    onCancelar: () -> Unit,
    viewModel: FormularioViewModel = viewModel(factory = FormularioViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Cuando el guardado termina bien, la navegación vuelve al listado una sola vez.
    val envio = uiState.envio
    LaunchedEffect(envio) {
        if (envio is EstadoEnvio.Exito) onGuardado(envio.idCreado)
    }
    CrearRecordatorioScreen(
        uiState = uiState,
        onTituloChange = viewModel::onTituloChange,
        onDescripcionChange = viewModel::onDescripcionChange,
        onCategoriaSeleccionada = viewModel::onCategoriaSeleccionada,
        onPrioridadSeleccionada = viewModel::onPrioridadSeleccionada,
        onGuardar = viewModel::onGuardar,
        onCancelar = onCancelar,
        onErrorDescartado = viewModel::onErrorDescartado
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearRecordatorioScreen(
    uiState: FormularioUiState,
    onTituloChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaSeleccionada: (Categoria) -> Unit,
    onPrioridadSeleccionada: (Prioridad) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    onErrorDescartado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habilitado = !uiState.enviando
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Nuevo recordatorio") },
                navigationIcon = { BotonVolver(onVolver = onCancelar) }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Los campos marcados con * son obligatorios.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CampoTextoValidado(
                valor = uiState.titulo,
                onValorChange = onTituloChange,
                etiqueta = "Título *",
                error = uiState.errores.titulo,
                ayuda = "Mínimo ${ValidadorRecordatorio.TITULO_MIN} caracteres",
                enabled = habilitado
            )
            CampoTextoValidado(
                valor = uiState.descripcion,
                onValorChange = onDescripcionChange,
                etiqueta = "Descripción",
                error = uiState.errores.descripcion,
                ayuda = "Opcional",
                contador = "${uiState.descripcion.length}/${ValidadorRecordatorio.DESCRIPCION_MAX}",
                singleLine = false,
                minLines = 3,
                enabled = habilitado
            )
            SelectorCategoria(
                seleccionada = uiState.categoria,
                opciones = uiState.categoriasDisponibles,
                estilos = uiState.estilos,
                onSeleccion = onCategoriaSeleccionada,
                error = uiState.errores.categoria,
                enabled = habilitado
            )
            SelectorPrioridad(
                seleccionada = uiState.prioridad,
                onSeleccion = onPrioridadSeleccionada,
                error = uiState.errores.prioridad,
                enabled = habilitado
            )

            val envio = uiState.envio
            if (envio is EstadoEnvio.Error) {
                BannerError(
                    mensaje = envio.mensaje,
                    onReintentar = onGuardar,
                    onDescartar = onErrorDescartado,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onGuardar,
                    enabled = uiState.puedeGuardar,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.enviando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardando…")
                    } else {
                        Text("Guardar")
                    }
                }
            }
            if (!uiState.esValido && !uiState.enviando) {
                Text(
                    "Completá los campos obligatorios para habilitar «Guardar».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Formulario con errores")
@Composable
private fun FormularioErroresPreview() {
    RecordatoriosTheme {
        CrearRecordatorioScreen(
            uiState = FormularioUiState(
                titulo = "Ok",
                categoriasDisponibles = DatosSimulados.categorias.toList(),
                estilos = DatosSimulados.estilosPorCategoria,
                errores = ErroresFormulario(titulo = "El título debe tener al menos 4 caracteres")
            ),
            onTituloChange = {}, onDescripcionChange = {}, onCategoriaSeleccionada = {},
            onPrioridadSeleccionada = {}, onGuardar = {}, onCancelar = {}, onErrorDescartado = {}
        )
    }
}
