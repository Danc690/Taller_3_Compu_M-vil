package com.example.taller3compumovil.data.repository

import com.example.taller3compumovil.model.Presence
import com.example.taller3compumovil.model.UserProfile

interface AccountProvisioningRepository {
    suspend fun ensureAccountDocuments(
        profile: UserProfile,
        presence: Presence
    ): Result<Unit>
}
