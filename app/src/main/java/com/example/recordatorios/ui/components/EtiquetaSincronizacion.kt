package com.example.recordatorios.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.recordatorios.model.EstadoSincronizacion

/** Texto + color: el estado de sincronización nunca depende solo del color. */
fun textoSincronizacion(estado: EstadoSincronizacion): String = when (estado) {
    EstadoSincronizacion.Sincronizado -> "☁ Sincronizado"
    is EstadoSincronizacion.Pendiente -> buildString {
        append("⏳ Pendiente de sincronizar (${estado.operacion.etiqueta})")
        if (estado.intentos > 0) append(" · ${estado.intentos} intento(s)")
    }
    is EstadoSincronizacion.ConError -> "⚠ No sincronizado: requiere reintento"
}

fun colorSincronizacion(estado: EstadoSincronizacion): Color = when (estado) {
    EstadoSincronizacion.Sincronizado -> Color(0xFF2E7D32)
    is EstadoSincronizacion.Pendiente -> Color(0xFFB26A00)
    is EstadoSincronizacion.ConError -> Color(0xFFC62828)
}

@Composable
fun EtiquetaSincronizacion(estado: EstadoSincronizacion, modifier: Modifier = Modifier) {
    Text(
        text = textoSincronizacion(estado),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = colorSincronizacion(estado),
        modifier = modifier
    )
}
