package com.example.taller3compumovil.domain.repository

import com.example.taller3compumovil.data.model.Presence
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

interface PresenceRepository {
    suspend fun createPresence(presence: Presence): Result<Unit>

    suspend fun publishLocation(
        uid: String,
        latitude: Double,
        longitude: Double,
        lastLocationAt: Timestamp
    ): Result<Unit>

    suspend fun markOffline(uid: String): Result<Unit>

    suspend fun updatePublicProfile(
        uid: String,
        name: String,
        photoUrl: String?
    ): Result<Unit>

    fun observePresence(uid: String): Flow<Presence?>

    fun observeOnlineUsers(): Flow<List<Presence>>
}
