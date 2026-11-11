package com.example.taller3compumovil.model

import com.google.firebase.Timestamp

data class Presence(
    val uid: String,
    val name: String,
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastLocationAt: Timestamp? = null
)
