package com.example.recordatorios

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.recordatorios.data.RepositorioRecordatorios
import com.example.recordatorios.data.RepositorioRecordatoriosLocalFirst
import com.example.recordatorios.data.local.RecordatoriosDatabase
import com.example.recordatorios.data.preferencias.FuentePreferencias
import com.example.recordatorios.data.preferencias.RepositorioPreferencias
import com.example.recordatorios.data.preferencias.dataStorePreferencias
import com.example.recordatorios.data.red.MonitorConectividad
import com.example.recordatorios.data.red.MonitorConectividadAndroid
import com.example.recordatorios.data.remoto.ClienteRemoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Contenedor de dependencias manual. Cada pieza se crea una sola vez para toda la app:
 * una base de datos Room, una instancia de DataStore, un cliente OkHttp/Retrofit y un repositorio.
 */
class ContenedorApp(context: Context) {

    /** Alcance de aplicación: la sincronización sobrevive a las pantallas y a los ViewModel. */
    private val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val baseDeDatos = RecordatoriosDatabase.obtener(context)

    val monitorConectividad: MonitorConectividad = MonitorConectividadAndroid(context)

    val preferencias: FuentePreferencias = RepositorioPreferencias(context.dataStorePreferencias)

    val urlServicio: String = BuildConfig.API_BASE_URL

    private val api = ClienteRemoto.crearApi(
        baseUrl = urlServicio,
        cliente = ClienteRemoto.crearOkHttp(registrarTrafico = BuildConfig.DEBUG)
    )

    val repositorio: RepositorioRecordatorios = RepositorioRecordatoriosLocalFirst(
        dao = baseDeDatos.recordatorioDao(),
        api = api,
        monitor = monitorConectividad,
        alcance = alcanceApp,
        registro = { Log.d("Recordatorios", it) }
    )
}

class RecordatoriosApplication : Application() {
    val contenedor: ContenedorApp by lazy { ContenedorApp(this) }

    override fun onCreate() {
        super.onCreate()
        // Crear el repositorio al iniciar: dispara la primera sincronización.
        contenedor.repositorio
    }
}
