package com.example.tachometr

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlin.reflect.KClass

@Composable
fun AppNavigation(
    locationDao: LocationDao,
    sessionDao: SessionDao,
    locationTracker: LocationTracker
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "tachometer") {
        composable("tachometer") {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    return TachometerViewModel(locationDao, sessionDao, locationTracker) as T
                }
            }
            val viewModel: TachometerViewModel = viewModel(factory = factory)
            TachometerScreen(
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToDetail = { sessionId -> navController.navigate("detail/$sessionId") }
            )
        }

        composable("history") {
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    return HistoryViewModel(sessionDao, locationDao) as T
                }
            }
            val viewModel: HistoryViewModel = viewModel(factory = factory)
            HistoryScreen(
                viewModel = viewModel,
                onPathClick = { sessionId -> 
                    navController.navigate("detail/$sessionId") 
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "detail/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    return PathDetailViewModel(sessionId, sessionDao, locationDao) as T
                }
            }
            val viewModel: PathDetailViewModel = viewModel(factory = factory)
            PathDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}