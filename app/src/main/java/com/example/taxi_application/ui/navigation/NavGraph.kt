package com.example.taxi_application.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.taxi_application.ui.screens.AuthScreen
import com.example.taxi_application.ui.screens.HistoryScreen
import com.example.taxi_application.ui.screens.MainMapScreen
import com.example.taxi_application.ui.screens.ProfileScreen
import com.example.taxi_application.ui.viewmodel.AuthViewModel
import com.example.taxi_application.ui.viewmodel.MapViewModel
import com.example.taxi_application.ui.viewmodel.OrderViewModel

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Map : Screen("map")
    object History : Screen("history")
    object Profile : Screen("profile")
}

@Composable
fun TaxiNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    mapViewModel: MapViewModel,
    orderViewModel: OrderViewModel
) {
    val startDestination = if (authViewModel.isLoggedIn) Screen.Map.route else Screen.Auth.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Auth.route) {
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Map.route) {
            MainMapScreen(
                mapViewModel = mapViewModel,
                orderViewModel = orderViewModel,
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                orderViewModel = orderViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
