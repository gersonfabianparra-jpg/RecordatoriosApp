package com.example.recordatorios.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val formatoDia: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Fecha límite opcional con DatePicker de Material 3. Solo informa la fecha elegida. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(
    fecha: LocalDate?,
    onFechaSeleccionada: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Fecha límite: " + (fecha?.format(formatoDia) ?: "sin fecha"),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = { abierto = true }, enabled = enabled) { Text("📅 Elegir") }
        if (fecha != null) {
            TextButton(onClick = { onFechaSeleccionada(null) }, enabled = enabled) { Text("Quitar") }
        }
    }
    if (abierto) {
        // DatePicker trabaja con milisegundos UTC a medianoche.
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    abierto = false
                    estado.selectedDateMillis?.let {
                        onFechaSeleccionada(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}
