package com.example.taller3compumovil.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taller3compumovil.data.repository.AuthRepository
import com.example.taller3compumovil.data.repository.UserRepository
import com.example.taller3compumovil.model.UserProfile
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile? = null,
    val name: String = "",
    val phone: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val isUnauthenticated: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val originalError: Throwable? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var currentUid: String? = null

    fun loadProfile() {
        if (_uiState.value.isLoading || _uiState.value.isSaving) return

        val uid = authRepository.getCurrentUid()
        if (uid == null) {
            currentUid = null
            _uiState.value = ProfileUiState(
                isUnauthenticated = true,
                errorMessage = "Inicia sesión para consultar tu perfil."
            )
            return
        }

        currentUid = uid
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isUnauthenticated = false,
            errorMessage = null,
            successMessage = null,
            originalError = null
        )

        viewModelScope.launch {
            try {
                userRepository.getProfile(uid).fold(
                    onSuccess = { profile ->
                        if (profile == null) {
                            _uiState.value = ProfileUiState(
                                errorMessage = "No se encontró el perfil de esta cuenta."
                            )
                        } else {
                            _uiState.value = ProfileUiState(
                                profile = profile,
                                name = profile.name,
                                phone = profile.phone
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.value = ProfileUiState(
                            errorMessage = messageFor(exception),
                            originalError = exception
                        )
                    }
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.value = ProfileUiState(
                    errorMessage = "Ocurrió un error inesperado al consultar el perfil.",
                    originalError = exception
                )
            }
        }
    }

    fun onNameChanged(name: String) {
        if (!_uiState.value.isEditing || _uiState.value.isSaving) return
        _uiState.value = _uiState.value.copy(
            name = name,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onPhoneChanged(phone: String) {
        if (!_uiState.value.isEditing || _uiState.value.isSaving) return
        _uiState.value = _uiState.value.copy(
            phone = phone,
            errorMessage = null,
            successMessage = null
        )
    }

    fun startEditing() {
        if (_uiState.value.isSaving || _uiState.value.profile == null) return
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            errorMessage = null,
            successMessage = null
        )
    }

    fun cancelEditing() {
        if (_uiState.value.isSaving) return
        val profile = _uiState.value.profile ?: return
        _uiState.value = _uiState.value.copy(
            name = profile.name,
            phone = profile.phone,
            isEditing = false,
            errorMessage = null,
            successMessage = null,
            originalError = null
        )
    }

    fun saveChanges() {
        if (_uiState.value.isSaving || !_uiState.value.isEditing) return

        val profile = _uiState.value.profile ?: return
        val name = _uiState.value.name.trim()
        val phone = _uiState.value.phone.trim()
        if (name.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "El nombre no puede estar vacío.")
            return
        }
        if (phone.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "El teléfono no puede estar vacío.")
            return
        }

        val uid = currentUid
        if (uid == null || uid != authRepository.getCurrentUid()) {
            _uiState.value = _uiState.value.copy(
                isUnauthenticated = true,
                errorMessage = "La sesión ya no está disponible. Inicia sesión nuevamente."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            name = name,
            phone = phone,
            isSaving = true,
            errorMessage = null,
            successMessage = null,
            originalError = null
        )

        viewModelScope.launch {
            try {
                saveProfileChanges(uid, profile, name, phone)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Ocurrió un error inesperado al guardar el perfil.",
                    originalError = exception
                )
            }
        }
    }

    private suspend fun saveProfileChanges(
        uid: String,
        oldProfile: UserProfile,
        name: String,
        phone: String
    ) {
        var nameSaved = name == oldProfile.name
        var phoneSaved = phone == oldProfile.phone

        if (!nameSaved) {
            userRepository.updateName(uid, name).fold(
                onSuccess = { nameSaved = true },
                onFailure = { exception ->
                    finishSavingWithError(exception, messageFor(exception), nameSaved = false)
                    return
                }
            )
        }

        if (!phoneSaved) {
            userRepository.updatePhone(uid, phone).fold(
                onSuccess = { phoneSaved = true },
                onFailure = { exception ->
                    val message = if (nameSaved) {
                        "El nombre se guardó, pero no se pudo actualizar el teléfono. Puedes volver a intentar."
                    } else {
                        messageFor(exception)
                    }
                    finishSavingWithError(exception, message, nameSaved)
                    return
                }
            )
        }

        _uiState.value = _uiState.value.copy(
            profile = oldProfile.copy(name = name, phone = phone),
            name = name,
            phone = phone,
            isSaving = false,
            isEditing = false,
            errorMessage = null,
            successMessage = "Los cambios del perfil se guardaron correctamente.",
            originalError = null
        )
    }

    private fun finishSavingWithError(
        exception: Throwable,
        message: String,
        nameSaved: Boolean
    ) {
        val state = _uiState.value
        val profile = state.profile?.let {
            if (nameSaved) it.copy(name = state.name) else it
        }
        _uiState.value = state.copy(
            profile = profile,
            isSaving = false,
            isEditing = true,
            errorMessage = message,
            successMessage = null,
            originalError = exception
        )
    }

    private fun messageFor(exception: Throwable): String = when (exception) {
        is FirebaseNetworkException ->
            "No hay conexión. Revisa tu conexión a internet e inténtalo de nuevo."

        is FirebaseFirestoreException -> when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "No tienes permiso para consultar o actualizar este perfil."

            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Firestore no está disponible en este momento. Inténtalo de nuevo."

            FirebaseFirestoreException.Code.NOT_FOUND -> "No se encontró el perfil en Firestore."
            else -> "No se pudo completar la operación del perfil. Inténtalo de nuevo."
        }

        else -> "No se pudo completar la operación del perfil. Inténtalo de nuevo."
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                throw IllegalArgumentException("No se puede crear el ViewModel solicitado.")
            }
            return ProfileViewModel(authRepository, userRepository) as T
        }
    }
}
