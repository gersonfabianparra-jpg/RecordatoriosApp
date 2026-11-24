package com.example.recordatorios.data.preferencias

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Única instancia de DataStore para toda la app: el delegado `preferencesDataStore`
 * garantiza un solo objeto por archivo. Tener dos instancias sobre el mismo archivo
 * provocaría una IllegalStateException.
 *
 * Solo guarda configuraciones simples (tema, filtro, orden). Los recordatorios van a Room,
 * y en DataStore no se guardan credenciales ni tokens.
 */
val Context.dataStorePreferencias: DataStore<Preferences> by preferencesDataStore(name = "preferencias_usuario")

/** Claves tipadas. */
object ClavesPreferencias {
    val TEMA = stringPreferencesKey("tema")
    val MOSTRAR_COMPLETADOS = booleanPreferencesKey("mostrar_completados")
    val ORDEN = stringPreferencesKey("orden")
    val FILTRO_PRIORIDAD = stringPreferencesKey("filtro_prioridad")
}

interface FuentePreferencias {
    val preferencias: Flow<PreferenciasUsuario>
    suspend fun cambiarTema(tema: TemaApp)
    suspend fun cambiarMostrarCompletados(mostrar: Boolean)
    suspend fun cambiarOrden(orden: OrdenListado)
    suspend fun cambiarFiltroPrioridad(filtro: FiltroPrioridad)
    suspend fun restablecer()
    /** Solo para pruebas: guarda un tema que no existe para comprobar el valor de respaldo. */
    suspend fun guardarTemaInvalidoDePrueba()
}

class RepositorioPreferencias(private val dataStore: DataStore<Preferences>) : FuentePreferencias {

    /**
     * Flujo frío: emite al suscribirse y cada vez que cambia el archivo. Un error de lectura
     * recuperable (IOException, por ejemplo archivo dañado) no rompe la app: se emiten los
     * valores predeterminados y se marca [PreferenciasUsuario.errorLectura].
     */
    override val preferencias: Flow<PreferenciasUsuario> = dataStore.data
        .map { leerPreferencias(it) }
        .catch { e ->
            if (e is IOException) {
                Log.w(TAG, "No se pudieron leer las preferencias; se usan las predeterminadas", e)
                emit(leerPreferencias(emptyPreferences()).copy(errorLectura = true))
            } else {
                throw e
            }
        }

    // edit {} es una función suspendida que escribe de forma atómica fuera del hilo principal.
    override suspend fun cambiarTema(tema: TemaApp) {
        dataStore.edit { it[ClavesPreferencias.TEMA] = tema.name }
    }

    override suspend fun cambiarMostrarCompletados(mostrar: Boolean) {
        dataStore.edit { it[ClavesPreferencias.MOSTRAR_COMPLETADOS] = mostrar }
    }

    override suspend fun cambiarOrden(orden: OrdenListado) {
        dataStore.edit { it[ClavesPreferencias.ORDEN] = orden.name }
    }

    override suspend fun cambiarFiltroPrioridad(filtro: FiltroPrioridad) {
        dataStore.edit { it[ClavesPreferencias.FILTRO_PRIORIDAD] = filtro.name }
    }

    override suspend fun restablecer() {
        dataStore.edit { it.clear() }
    }

    override suspend fun guardarTemaInvalidoDePrueba() {
        dataStore.edit { it[ClavesPreferencias.TEMA] = "MORADO" }
    }

    companion object {
        private const val TAG = "Recordatorios"

        /**
         * Transforma las preferencias crudas en el modelo propio. Es una función pura,
         * por eso se prueba sin Android: ausente → predeterminado; desconocido → predeterminado
         * y se registra como inválido.
         */
        fun leerPreferencias(crudas: Preferences): PreferenciasUsuario {
            val predeterminadas = mutableSetOf<String>()
            val invalidas = mutableListOf<String>()
            val base = PreferenciasUsuario()

            fun <E : Enum<E>> enumOPredeterminado(
                nombre: String,
                guardado: String?,
                valores: Array<E>,
                porDefecto: E
            ): E {
                if (guardado == null) {
                    predeterminadas += nombre
                    return porDefecto
                }
                return valores.firstOrNull { it.name == guardado } ?: porDefecto.also {
                    invalidas += "$nombre: «$guardado» no es una opción válida; se usa ${porDefecto.name}"
                }
            }

            val tema = enumOPredeterminado("tema", crudas[ClavesPreferencias.TEMA], TemaApp.entries.toTypedArray(), base.tema)
            val orden = enumOPredeterminado("orden", crudas[ClavesPreferencias.ORDEN], OrdenListado.entries.toTypedArray(), base.orden)
            val filtro = enumOPredeterminado(
                "filtro_prioridad", crudas[ClavesPreferencias.FILTRO_PRIORIDAD],
                FiltroPrioridad.entries.toTypedArray(), base.filtroPrioridad
            )
            val mostrar = crudas[ClavesPreferencias.MOSTRAR_COMPLETADOS]
                ?: base.mostrarCompletados.also { predeterminadas += "mostrar_completados" }

            return PreferenciasUsuario(
                tema = tema,
                mostrarCompletados = mostrar,
                orden = orden,
                filtroPrioridad = filtro,
                predeterminadas = predeterminadas,
                invalidas = invalidas
            )
        }
    }
}
