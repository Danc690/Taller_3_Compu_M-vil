package com.example.taller3compumovil.data.model

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val photoUrl: String? = null
)
