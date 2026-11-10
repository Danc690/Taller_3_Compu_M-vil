package com.example.taller3compumovil.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(email: String, password: String): Result<String>

    suspend fun signIn(email: String, password: String): Result<String>

    suspend fun signOut()

    fun getCurrentUid(): String?

    fun observeAuthState(): Flow<String?>
}
