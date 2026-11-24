package com.example.recordatorios.ui.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recordatorios.ui.ajustes.AjustesRoute
import com.example.recordatorios.ui.crear.CrearRecordatorioRoute
import com.example.recordatorios.ui.detalle.DetalleRoute
import com.example.recordatorios.ui.lista.ListaRoute

/** Rutas del grafo. Al detalle y a la edición solo viaja el identificador local. */
object Rutas {
    const val LISTA = "lista"
    const val CREAR = "crear"
    const val AJUSTES = "ajustes"
    const val ARG_ID = "recordatorioId"
    const val DETALLE = "detalle/{$ARG_ID}"
    const val EDITAR = "editar/{$ARG_ID}"

    fun detalle(id: Long) = "detalle/$id"
    fun editar(id: Long) = "editar/$id"
}

/**
 * El NavController vive acá, en el nivel más alto de la UI. Las pantallas solo
 * reciben lambdas (onCrearRecordatorio, onRecordatorioClick, onVolver...).
 */
@Composable
fun RecordatoriosNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val argumentoId = listOf(navArgument(Rutas.ARG_ID) { type = NavType.LongType })
    NavHost(
        navController = navController,
        startDestination = Rutas.LISTA,
        modifier = modifier
    ) {
        composable(Rutas.LISTA) {
            ListaRoute(
                onCrearRecordatorio = dropUnlessResumed {
                    navController.navigate(Rutas.CREAR) { launchSingleTop = true }
                },
                onRecordatorioClick = { id ->
                    navController.navigate(Rutas.detalle(id)) { launchSingleTop = true }
                },
                onAbrirAjustes = dropUnlessResumed {
                    navController.navigate(Rutas.AJUSTES) { launchSingleTop = true }
                }
            )
        }
        composable(Rutas.CREAR) {
            CrearRecordatorioRoute(
                onGuardado = { navController.popBackStack() },
                onCancelar = dropUnlessResumed { navController.popBackStack() }
            )
        }
        composable(Rutas.EDITAR, arguments = argumentoId) {
            CrearRecordatorioRoute(
                // Vuelve al detalle, que ya muestra los cambios porque observa Room.
                onGuardado = { navController.popBackStack() },
                onCancelar = dropUnlessResumed { navController.popBackStack() }
            )
        }
        composable(Rutas.DETALLE, arguments = argumentoId) {
            DetalleRoute(
                onVolver = { navController.popBackStack(Rutas.LISTA, inclusive = false) },
                onEditar = { id -> navController.navigate(Rutas.editar(id)) { launchSingleTop = true } }
            )
        }
        composable(Rutas.AJUSTES) {
            AjustesRoute(onVolver = dropUnlessResumed { navController.popBackStack() })
        }
    }
}
