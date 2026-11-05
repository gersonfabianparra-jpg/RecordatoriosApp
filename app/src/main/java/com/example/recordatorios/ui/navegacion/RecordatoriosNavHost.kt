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
import com.example.recordatorios.ui.crear.CrearRecordatorioRoute
import com.example.recordatorios.ui.detalle.DetalleRoute
import com.example.recordatorios.ui.lista.ListaRoute

/** Rutas del grafo. Al detalle solo viaja el identificador. */
object Rutas {
    const val LISTA = "lista"
    const val CREAR = "crear"
    const val ARG_ID = "recordatorioId"
    const val DETALLE = "detalle/{$ARG_ID}"

    fun detalle(id: Long) = "detalle/$id"
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
                }
            )
        }
        composable(Rutas.CREAR) {
            CrearRecordatorioRoute(
                // popBackStack vuelve al listado existente: no se apila una copia nueva.
                onGuardado = { navController.popBackStack() },
                onCancelar = dropUnlessResumed { navController.popBackStack() }
            )
        }
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.LongType })
        ) {
            DetalleRoute(onVolver = dropUnlessResumed { navController.popBackStack() })
        }
    }
}
