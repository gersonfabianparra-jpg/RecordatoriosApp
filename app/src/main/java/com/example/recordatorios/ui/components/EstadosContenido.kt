package com.example.recordatorios.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Indicador de carga centrado, con texto para lectores de pantalla. */
@Composable
fun EstadoCarga(
    mensaje: String,
    modifier: Modifier = Modifier
) {
    PlantillaEstado(modifier) {
        CircularProgressIndicator()
        Text(mensaje, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Estado vacío: explica la situación y ofrece una acción. */
@Composable
fun EstadoVacio(
    titulo: String,
    mensaje: String,
    textoAccion: String,
    onAccion: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlantillaEstado(modifier) {
        Text("🗒️", style = MaterialTheme.typography.displayMedium)
        Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onAccion) { Text(textoAccion) }
    }
}

/** Estado de error: mensaje comprensible y opción de reintento. */
@Composable
fun EstadoError(
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlantillaEstado(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text("⚠️", style = MaterialTheme.typography.displayMedium)
        Text(
            "Algo salió mal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        OutlinedButton(onClick = onReintentar) { Text("Reintentar") }
    }
}

@Composable
private fun PlantillaEstado(modifier: Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        contenido()
    }
}

/** Aviso de error dentro de una pantalla con contenido (por ejemplo, al guardar). */
@Composable
fun BannerError(
    mensaje: String,
    onReintentar: () -> Unit,
    onDescartar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("⚠ No se pudo guardar", fontWeight = FontWeight.SemiBold)
            Text(mensaje, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onReintentar) { Text("Reintentar") }
                TextButton(onClick = onDescartar) { Text("Descartar") }
            }
        }
    }
}
