package com.example.taller3compumovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.taller3compumovil.data.RegisterUserUseCase
import com.example.taller3compumovil.data.repository.FirebaseAuthRepository
import com.example.taller3compumovil.data.repository.FirebaseUserRepository
import com.example.taller3compumovil.data.repository.FirestoreAccountProvisioningRepository
import com.example.taller3compumovil.navigation.AppNavHost
import com.example.taller3compumovil.ui.theme.Taller_3_Compu_MovilTheme
import com.example.taller3compumovil.viewmodel.LoginViewModel
import com.example.taller3compumovil.viewmodel.ProfileViewModel
import com.example.taller3compumovil.viewmodel.RegisterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authRepository = FirebaseAuthRepository()
        val userRepository = FirebaseUserRepository()
        val registerUserUseCase = RegisterUserUseCase(
            authRepository = authRepository,
            accountProvisioningRepository = FirestoreAccountProvisioningRepository()
        )
        val loginViewModel = ViewModelProvider(
            this,
            LoginViewModel.Factory(
                authRepository = authRepository,
                userRepository = userRepository,
                registerUserUseCase = registerUserUseCase
            )
        )[LoginViewModel::class.java]
        val registerViewModel = ViewModelProvider(
            this,
            RegisterViewModel.Factory(
                authRepository = authRepository,
                registerUserUseCase = registerUserUseCase
            )
        )[RegisterViewModel::class.java]
        val profileViewModel = ViewModelProvider(
            this,
            ProfileViewModel.Factory(
                authRepository = authRepository,
                userRepository = userRepository
            )
        )[ProfileViewModel::class.java]

        setContent {
            Taller_3_Compu_MovilTheme {
                AppNavHost(
                    loginViewModel = loginViewModel,
                    registerViewModel = registerViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}
