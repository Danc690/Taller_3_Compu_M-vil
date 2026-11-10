package com.example.taller3compumovil.data.model

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastLocationAt: Timestamp? = null
)
