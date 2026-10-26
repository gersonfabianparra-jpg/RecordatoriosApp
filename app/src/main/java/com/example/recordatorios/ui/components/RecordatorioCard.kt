package com.example.recordatorios.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Prioridad
import com.example.recordatorios.model.Recordatorio
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM HH:mm")

@Composable
fun RecordatorioCard(
    recordatorio: Recordatorio,
    estilo: EstiloCategoria,
    modifier: Modifier = Modifier
) {
    val colorCategoria = Color(estilo.colorArgb)
    val completado = recordatorio.estado is EstadoRecordatorio.Completado

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Franja con el color de la categoría (obtenido del Map)
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .heightIn(min = 72.dp)
                    .background(colorCategoria)
            )
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = recordatorio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (completado) TextDecoration.LineThrough else null
                )
                recordatorio.descripcion?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Etiqueta(estilo.etiqueta, colorCategoria)
                    Etiqueta("▲ ${recordatorio.prioridad.nombre}", colorPrioridad(recordatorio.prioridad))
                    Etiqueta(textoEstado(recordatorio.estado), colorEstado(recordatorio.estado))
                }
                val extras = listOfNotNull(
                    recordatorio.fechaLimite?.let { "📅 ${it.format(formatoFecha)}" },
                    recordatorio.aviso?.let { "🔔 ${it.minutosAntes} min antes" },
                    recordatorio.ubicacion?.let { "📍 ${it.nombre}" }
                )
                if (extras.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = extras.joinToString("   "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Surface(
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

private fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.Alta -> Color(0xFFD32F2F)
    Prioridad.Media -> Color(0xFFF57C00)
    Prioridad.Baja -> Color(0xFF388E3C)
}

private fun colorEstado(estado: EstadoRecordatorio): Color = when (estado) {
    EstadoRecordatorio.Pendiente -> Color(0xFF1976D2)
    is EstadoRecordatorio.Pospuesto -> Color(0xFF7B1FA2)
    EstadoRecordatorio.Vencido -> Color(0xFFC62828)
    is EstadoRecordatorio.Completado -> Color(0xFF2E7D32)
}

private fun textoEstado(estado: EstadoRecordatorio): String = when (estado) {
    EstadoRecordatorio.Pendiente -> "Pendiente"
    is EstadoRecordatorio.Pospuesto -> "Pospuesto ${estado.hasta.format(formatoFecha)}"
    EstadoRecordatorio.Vencido -> "Vencido"
    is EstadoRecordatorio.Completado -> "✓ Completado"
}
