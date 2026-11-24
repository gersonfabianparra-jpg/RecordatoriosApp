package com.example.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatorios.data.preferencias.PreferenciasUsuario
import com.example.recordatorios.data.preferencias.TemaApp
import com.example.recordatorios.ui.navegacion.RecordatoriosNavHost
import com.example.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencias = (application as RecordatoriosApplication).contenedor.preferencias.preferencias
        setContent {
            // El tema guardado en DataStore se aplica a toda la app y se observa con el ciclo de vida.
            val prefs by preferencias.collectAsStateWithLifecycle(initialValue = PreferenciasUsuario())
            val oscuro = when (prefs.tema) {
                TemaApp.SISTEMA -> isSystemInDarkTheme()
                TemaApp.CLARO -> false
                TemaApp.OSCURO -> true
            }
            RecordatoriosTheme(oscuro = oscuro) {
                RecordatoriosNavHost()
            }
        }
    }
}
