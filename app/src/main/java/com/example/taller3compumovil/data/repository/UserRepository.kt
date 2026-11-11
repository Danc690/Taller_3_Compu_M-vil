package com.example.taller3compumovil.data.repository

import com.example.taller3compumovil.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun createProfile(profile: UserProfile): Result<Unit>

    suspend fun getProfile(uid: String): Result<UserProfile?>

    fun observeProfile(uid: String): Flow<UserProfile?>

    suspend fun updateName(uid: String, name: String): Result<Unit>

    suspend fun updatePhone(uid: String, phone: String): Result<Unit>
}
