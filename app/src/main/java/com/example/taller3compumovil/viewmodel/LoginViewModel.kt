package com.example.taller3compumovil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taller3compumovil.data.RegisterUserUseCase
import com.example.taller3compumovil.data.repository.AuthRepository
import com.example.taller3compumovil.data.repository.UserRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetryProvisioning: Boolean = false,
    val canOpenMainScreen: Boolean = false,
    val originalError: Throwable? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val registerUserUseCase: RegisterUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (_uiState.value.isLoading) return

        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            showValidationError("Ingresa tu correo electrónico.")
            return
        }
        if (password.isEmpty()) {
            showValidationError("Ingresa tu contraseña.")
            return
        }

        _uiState.value = LoginUiState(isLoading = true)
        viewModelScope.launch {
            try {
                authRepository.signIn(cleanEmail, password).fold(
                    onSuccess = { returnedUid ->
                        val sessionUid = authRepository.getCurrentUid()
                        if (sessionUid == null || sessionUid != returnedUid) {
                            showError(
                                IllegalStateException(
                                    "La sesión actual no coincide con la cuenta autenticada."
                                ),
                                canRetry = false
                            )
                        } else {
                            checkProfileAndProvision()
                        }
                    },
                    onFailure = { exception -> showError(exception, canRetry = false) }
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                showError(exception, canRetry = false)
            }
        }
    }

    fun retryProvisioning() {
        if (_uiState.value.isLoading || !_uiState.value.canRetryProvisioning) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            originalError = null
        )
        viewModelScope.launch {
            try {
                checkProfileAndProvision()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                showError(exception, canRetry = true)
            }
        }
    }

    private suspend fun checkProfileAndProvision() {
        val uid = authRepository.getCurrentUid()
        if (uid == null) {
            showError(
                IllegalStateException("No hay una sesión activa para comprobar el perfil."),
                canRetry = false
            )
            return
        }

        userRepository.getProfile(uid).fold(
            onSuccess = { profile ->
                if (profile == null) {
                    _uiState.value = LoginUiState(
                        errorMessage = "La cuenta necesita completar su registro. No se encontró un perfil asociado.",
                        canRetryProvisioning = false
                    )
                    return@fold
                }

                registerUserUseCase.retryProvisioning(profile.name, profile.phone).fold(
                    onSuccess = {
                        _uiState.value = LoginUiState(canOpenMainScreen = true)
                    },
                    onFailure = { exception -> showError(exception, canRetry = true) }
                )
            },
            onFailure = { exception -> showError(exception, canRetry = true) }
        )
    }

    private fun showValidationError(message: String) {
        _uiState.value = LoginUiState(errorMessage = message)
    }

    private fun showError(exception: Throwable, canRetry: Boolean) {
        _uiState.value = LoginUiState(
            errorMessage = messageFor(exception),
            canRetryProvisioning = canRetry,
            originalError = exception
        )
    }

    private fun messageFor(exception: Throwable): String = when (exception) {
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> "El correo o la contraseña son incorrectos."

        is FirebaseAuthException -> when (exception.errorCode) {
            "ERROR_TOO_MANY_REQUESTS" ->
                "Se realizaron demasiados intentos. Espera un momento y vuelve a intentarlo."

            else -> "No se pudo completar la autenticación. Revisa tus datos e inténtalo de nuevo."
        }
        is FirebaseNetworkException -> "No hay conexión. Revisa tu conexión a internet e inténtalo de nuevo."
        is FirebaseFirestoreException -> when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "No se pudo acceder al perfil por falta de permisos."

            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Firestore no está disponible en este momento. Inténtalo de nuevo."

            else -> "No se pudo consultar o completar el perfil. Inténtalo de nuevo."
        }

        is IllegalStateException ->
            "No se pudo completar el perfil porque la sesión o los datos guardados presentan un conflicto."

        else -> "No se pudo completar el inicio de sesión. Inténtalo de nuevo."
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val registerUserUseCase: RegisterUserUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                throw IllegalArgumentException("No se puede crear el ViewModel solicitado.")
            }
            return LoginViewModel(
                authRepository = authRepository,
                userRepository = userRepository,
                registerUserUseCase = registerUserUseCase
            ) as T
        }
    }
}