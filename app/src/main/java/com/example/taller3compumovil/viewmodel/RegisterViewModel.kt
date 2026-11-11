package com.example.taller3compumovil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taller3compumovil.data.RegisterUserUseCase
import com.example.taller3compumovil.data.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetryProvisioning: Boolean = false,
    val isRegistrationComplete: Boolean = false,
    val originalError: Throwable? = null
)

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val registerUserUseCase: RegisterUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var pendingName = ""
    private var pendingEmail = ""
    private var pendingPhone = ""

    fun register(name: String, email: String, phone: String, password: String) {
        if (_uiState.value.isLoading) return

        pendingName = name.trim()
        pendingEmail = email.trim()
        pendingPhone = phone.trim()

        val validationMessage = when {
            pendingName.isEmpty() -> "Ingresa tu nombre."
            pendingEmail.isEmpty() -> "Ingresa tu correo electrónico."
            pendingPhone.isEmpty() -> "Ingresa tu teléfono."
            password.isEmpty() -> "Ingresa una contraseña."
            else -> null
        }
        if (validationMessage != null) {
            _uiState.value = RegisterUiState(errorMessage = validationMessage)
            return
        }

        if (authRepository.getCurrentUid() != null) {
            if (authRepository.getCurrentEmail().equals(pendingEmail, ignoreCase = true)) {
                retryProvisioning()
            } else {
                _uiState.value = RegisterUiState(
                    errorMessage = "Hay otra cuenta activa. Vuelve al inicio de sesión antes de registrar una cuenta diferente."
                )
            }
            return
        }

        _uiState.value = RegisterUiState(isLoading = true)
        viewModelScope.launch {
            try {
                registerUserUseCase(
                    name = pendingName,
                    email = pendingEmail,
                    password = password,
                    phone = pendingPhone
                ).fold(
                    onSuccess = {
                        _uiState.value = RegisterUiState(isRegistrationComplete = true)
                    },
                    onFailure = { exception ->
                        showError(exception, canRetry = hasMatchingSession())
                    }
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                showError(exception, canRetry = hasMatchingSession())
            }
        }
    }

    fun retryProvisioning() {
        if (_uiState.value.isLoading || pendingName.isBlank() || pendingPhone.isBlank()) return
        if (!hasMatchingSession()) {
            _uiState.value = RegisterUiState(
                errorMessage = "No hay una sesión activa para reintentar la creación del perfil."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            originalError = null
        )
        viewModelScope.launch {
            try {
                registerUserUseCase.retryProvisioning(pendingName, pendingPhone).fold(
                    onSuccess = {
                        _uiState.value = RegisterUiState(isRegistrationComplete = true)
                    },
                    onFailure = { exception ->
                        showError(exception, canRetry = hasMatchingSession())
                    }
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                showError(exception, canRetry = hasMatchingSession())
            }
        }
    }

    private fun hasMatchingSession(): Boolean =
        authRepository.getCurrentUid() != null &&
            authRepository.getCurrentEmail().equals(pendingEmail, ignoreCase = true)

    private fun showError(exception: Throwable, canRetry: Boolean) {
        _uiState.value = RegisterUiState(
            errorMessage = messageFor(exception),
            canRetryProvisioning = canRetry,
            originalError = exception
        )
    }

    private fun messageFor(exception: Throwable): String = when (exception) {
        is FirebaseAuthException -> when (exception.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "Este correo ya está registrado. Inicia sesión con esa cuenta."

            "ERROR_INVALID_EMAIL" -> "El formato del correo electrónico no es válido."
            "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado sencilla. Elige una más segura."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Se realizaron demasiados intentos. Espera un momento y vuelve a intentarlo."

            else -> "No se pudo crear la cuenta en este momento. Revisa tus datos e inténtalo de nuevo."
        }

        is FirebaseNetworkException ->
            "No hay conexión. Revisa tu conexión a internet e inténtalo de nuevo."

        is FirebaseFirestoreException -> when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "La cuenta se creó, pero no hay permisos para completar el perfil."

            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "La cuenta se creó, pero Firestore no está disponible. Puedes reintentar la provisión."

            else -> "La cuenta se creó, pero no se pudieron completar sus datos. Puedes reintentar la provisión."
        }

        is IllegalStateException ->
            "No se pudo completar el perfil porque la sesión o los datos guardados presentan un conflicto."

        else -> "No se pudo completar el registro. Inténtalo de nuevo."
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val registerUserUseCase: RegisterUserUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
                throw IllegalArgumentException("No se puede crear el ViewModel solicitado.")
            }
            return RegisterViewModel(
                authRepository = authRepository,
                registerUserUseCase = registerUserUseCase
            ) as T
        }
    }
}
