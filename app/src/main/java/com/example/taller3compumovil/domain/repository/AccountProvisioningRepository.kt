package com.example.taller3compumovil.domain.repository

import com.example.taller3compumovil.data.model.Presence
import com.example.taller3compumovil.data.model.UserProfile

interface AccountProvisioningRepository {
    suspend fun ensureAccountDocuments(
        profile: UserProfile,
        presence: Presence
    ): Result<Unit>
}
