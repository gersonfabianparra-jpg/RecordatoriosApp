package com.example.recordatorios.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.Prioridad
import java.time.format.DateTimeFormatter

val formatoFechaCorta: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
val formatoFechaLarga: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Píldora con texto: el texto siempre acompaña al color. */
@Composable
fun EtiquetaChip(texto: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.Alta -> Color(0xFFD32F2F)
    Prioridad.Media -> Color(0xFFF57C00)
    Prioridad.Baja -> Color(0xFF388E3C)
}

fun colorEstado(estado: EstadoRecordatorio): Color = when (estado) {
    EstadoRecordatorio.Pendiente -> Color(0xFF1976D2)
    is EstadoRecordatorio.Pospuesto -> Color(0xFF7B1FA2)
    EstadoRecordatorio.Vencido -> Color(0xFFC62828)
    is EstadoRecordatorio.Completado -> Color(0xFF2E7D32)
}

fun textoEstado(estado: EstadoRecordatorio): String = when (estado) {
    EstadoRecordatorio.Pendiente -> "Pendiente"
    is EstadoRecordatorio.Pospuesto -> "Pospuesto ${estado.hasta.format(formatoFechaCorta)}"
    EstadoRecordatorio.Vencido -> "Vencido"
    is EstadoRecordatorio.Completado -> "✓ Completado"
}
