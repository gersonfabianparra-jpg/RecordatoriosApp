package com.example.recordatorios.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.recordatorios.model.EstadoRecordatorio
import com.example.recordatorios.model.EstiloCategoria
import com.example.recordatorios.model.Recordatorio

/** Un recordatorio del listado. Solo informa el clic; no sabe nada de navegación. */
@Composable
fun RecordatorioCard(
    recordatorio: Recordatorio,
    estilo: EstiloCategoria,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorCategoria = Color(estilo.colorArgb)
    val completado = recordatorio.estado is EstadoRecordatorio.Completado

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { onClick(label = "Ver detalle", action = null) },
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
                    EtiquetaChip(estilo.etiqueta, colorCategoria)
                    EtiquetaChip("▲ ${recordatorio.prioridad.nombre}", colorPrioridad(recordatorio.prioridad))
                    EtiquetaChip(textoEstado(recordatorio.estado), colorEstado(recordatorio.estado))
                }
                val extras = listOfNotNull(
                    recordatorio.fechaLimite?.let { "📅 ${it.format(formatoFechaCorta)}" },
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
