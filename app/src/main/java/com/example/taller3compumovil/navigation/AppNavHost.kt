package com.example.taller3compumovil.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taller3compumovil.ui.screens.LoginScreen
import com.example.taller3compumovil.ui.screens.MapScreen
import com.example.taller3compumovil.ui.screens.ProfileScreen
import com.example.taller3compumovil.ui.screens.RegisterScreen
import com.example.taller3compumovil.viewmodel.LoginViewModel
import com.example.taller3compumovil.viewmodel.MapViewModel
import com.example.taller3compumovil.viewmodel.ProfileViewModel
import com.example.taller3compumovil.viewmodel.RegisterViewModel

@Composable
fun AppNavHost(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    profileViewModel: ProfileViewModel,
    navController: NavHostController = rememberNavController()
) {
    val mapViewModel: MapViewModel = viewModel()

    Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(contentPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    viewModel = loginViewModel,
                    onRegisterClick = { navController.navigate(Routes.REGISTER) },
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    viewModel = registerViewModel,
                    onBackToLogin = { navController.popBackStack() },
                    onRegistrationSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Pantalla Principal (Mapa)
            composable(Routes.HOME) {
                val loginState by loginViewModel.uiState.collectAsState()
                val registerState by registerViewModel.uiState.collectAsState()

                if (loginState.canOpenMainScreen || registerState.isRegistrationComplete) {
                    MapScreen(
                        viewModel = mapViewModel,
                        onNavigateToProfile = { navController.navigate(Routes.PROFILE) }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }

            // Pantalla de Perfil
            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onUnauthenticated = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBackToMap = { navController.popBackStack() } // Volver al mapa
                )
            }
        }
    }
}