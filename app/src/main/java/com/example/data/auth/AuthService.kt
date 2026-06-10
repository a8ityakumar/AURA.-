package com.example.data.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthService {
    val currentUser: StateFlow<AuraUser?>
    val isRealFirebase: Boolean
    val currentDiagnosticMessage: String?

    suspend fun login(email: String, password: String): Result<AuraUser>
    suspend fun signUp(email: String, password: String, displayName: String?): Result<AuraUser>
    suspend fun loginWithGoogle(idToken: String, email: String, displayName: String?, photoUrl: String?): Result<AuraUser>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<AuraUser>
}
