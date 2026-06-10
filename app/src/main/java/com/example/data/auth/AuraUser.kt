package com.example.data.auth

import java.io.Serializable

data class AuraUser(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val creationDate: Long
) : Serializable
