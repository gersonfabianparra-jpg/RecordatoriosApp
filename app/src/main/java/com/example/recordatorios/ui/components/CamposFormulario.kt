package com.example.recordatorios.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.recordatorios.model.Categoria
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Prioridad

/**
 * Campo de texto controlado: el valor viene de afuera y cada cambio se informa
 * con onValorChange. El error se comunica con isError, un texto con ⚠ y la
 * propiedad semántica error (no solo con color).
 */
@Composable
fun CampoTextoValidado(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    error: String?,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    contador: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        supportingText = {
            TextoAyuda(error = error, ayuda = ayuda, contador = contador)
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (error != null) error(error) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorCategoria(
    seleccionada: Categoria?,
    opciones: List<Categoria>,
    estilos: Map<Categoria, EstiloCategoria>,
    onSeleccion: (Categoria) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { if (enabled) expandido = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = seleccionada?.let { estilos[it]?.etiqueta ?: it.nombre } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Categoría") },
            placeholder = { Text("Elegí una categoría") },
            isError = error != null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            supportingText = { TextoAyuda(error = error, ayuda = null, contador = null) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth()
                .semantics { if (error != null) error(error) }
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { categoria ->
                DropdownMenuItem(
                    text = { Text(estilos[categoria]?.etiqueta ?: categoria.nombre) },
                    onClick = {
                        onSeleccion(categoria)
                        expandido = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun SelectorPrioridad(
    seleccionada: Prioridad?,
    onSeleccion: (Prioridad) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val opciones = Prioridad.todas
    Column(modifier.semantics { if (error != null) error(error) }) {
        Text(
            "Prioridad",
            style = MaterialTheme.typography.labelLarge,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            opciones.forEachIndexed { indice, prioridad ->
                SegmentedButton(
                    selected = prioridad == seleccionada,
                    onClick = { onSeleccion(prioridad) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = indice, count = opciones.size)
                ) {
                    Text(prioridad.nombre)
                }
            }
        }
        Column(Modifier.padding(start = 16.dp, top = 4.dp)) {
            TextoAyuda(error = error, ayuda = null, contador = null)
        }
    }
}

/** Texto bajo un campo: el error tiene prioridad y lleva ⚠ además del color. */
@Composable
private fun TextoAyuda(error: String?, ayuda: String?, contador: String?) {
    val texto = when {
        error != null -> "⚠ $error"
        ayuda != null -> ayuda
        else -> null
    }
    if (texto == null && contador == null) return
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = texto.orEmpty(),
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        if (contador != null) {
            Text(contador, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BotonVolver(onVolver: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onVolver,
        modifier = modifier.semantics { contentDescription = "Volver" }
    ) {
        Text("←", style = MaterialTheme.typography.titleLarge)
    }
}
