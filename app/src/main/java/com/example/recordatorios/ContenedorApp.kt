package com.example.recordatorios

import android.app.Application
import com.example.recordatorios.data.ControlSimulacion
import com.example.recordatorios.data.RepositorioRecordatorios
import com.example.recordatorios.data.RepositorioRecordatoriosEnMemoria

/**
 * Contenedor de dependencias manual: una sola instancia del repositorio para toda
 * la app, expuesta por sus interfaces.
 */
class ContenedorApp {
    private val enMemoria = RepositorioRecordatoriosEnMemoria()
    val repositorio: RepositorioRecordatorios = enMemoria
    val simulacion: ControlSimulacion = enMemoria
}

class RecordatoriosApplication : Application() {
    val contenedor: ContenedorApp by lazy { ContenedorApp() }
}
