package com.example.taller3compumovil.domain.usecase

import com.example.taller3compumovil.data.model.Presence
import com.example.taller3compumovil.data.model.UserProfile
import com.example.taller3compumovil.domain.repository.AccountProvisioningRepository
import com.example.taller3compumovil.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException

class RegisterUserUseCase(
    private val authRepository: AuthRepository,
    private val accountProvisioningRepository: AccountProvisioningRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        phone: String
    ): Result<String> {
        val uid = authRepository.register(email, password).getOrElse { exception ->
            return failureOrRethrowCancellation(exception)
        }

        if (authRepository.getCurrentUid() != uid) {
            return Result.failure(
                IllegalStateException("La cuenta se creó, pero no coincide con la sesión actual.")
            )
        }

        val profile = UserProfile(
            uid = uid,
            name = name,
            email = email,
            phone = phone,
            photoUrl = null
        )
        val presence = Presence(
            uid = uid,
            name = name,
            photoUrl = null,
            isOnline = false,
            latitude = null,
            longitude = null,
            lastLocationAt = null
        )

        return provision(uid, profile, presence)
    }

    suspend fun retryProvisioning(name: String, phone: String): Result<String> {
        val uid = authRepository.getCurrentUid()
            ?: return Result.failure(
                IllegalStateException("No hay una sesión activa para reintentar la creación del perfil.")
            )
        val email = authRepository.getCurrentEmail()
            ?: return Result.failure(
                IllegalStateException("La sesión actual no tiene un correo electrónico disponible.")
            )

        val profile = UserProfile(
            uid = uid,
            name = name,
            email = email,
            phone = phone,
            photoUrl = null
        )
        val presence = Presence(
            uid = uid,
            name = name,
            photoUrl = null,
            isOnline = false,
            latitude = null,
            longitude = null,
            lastLocationAt = null
        )

        return provision(uid, profile, presence)
    }

    private suspend fun provision(
        uid: String,
        profile: UserProfile,
        presence: Presence
    ): Result<String> = accountProvisioningRepository
        .ensureAccountDocuments(profile, presence)
        .fold(
            onSuccess = { Result.success(uid) },
            onFailure = { exception -> failureOrRethrowCancellation(exception) }
        )

    private fun <T> failureOrRethrowCancellation(exception: Throwable): Result<T> {
        if (exception is CancellationException) throw exception
        return Result.failure(exception)
    }
}
