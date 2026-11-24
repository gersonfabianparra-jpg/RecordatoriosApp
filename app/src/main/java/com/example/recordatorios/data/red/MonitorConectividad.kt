package com.example.recordatorios.data.red

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** Abstracción de la conectividad: permite usar un doble en las pruebas. */
interface MonitorConectividad {
    /** true mientras haya una red con acceso a internet. */
    val conectado: Flow<Boolean>
    fun hayConexion(): Boolean
}

class MonitorConectividadAndroid(context: Context) : MonitorConectividad {

    private val gestor = context.getSystemService(ConnectivityManager::class.java)

    override fun hayConexion(): Boolean =
        gestor.getNetworkCapabilities(gestor.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    override val conectado: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            // Con registerDefaultNetworkCallback, onLost significa que ya no hay red por defecto
            // (en ese instante activeNetwork todavía puede devolver la red perdida). Si otra red
            // toma su lugar, llega un nuevo onAvailable.
            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
                trySend(capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        trySend(hayConexion())
        gestor.registerDefaultNetworkCallback(callback)
        awaitClose { gestor.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()
}
