package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class LocalAuthenticationService(context: Context) : AuthService {
    private val prefs: SharedPreferences = context.getSharedPreferences("aura_local_auth_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val userAdapter = moshi.adapter(AuraUser::class.java)

    private val _currentUser = MutableStateFlow<AuraUser?>(null)
    override val currentUser: StateFlow<AuraUser?> = _currentUser.asStateFlow()

    override val isRealFirebase: Boolean = false
    override val currentDiagnosticMessage: String = "Operating in local sandbox mode. Configure Firebase in Secrets to go live."

    init {
        // Load persist login session from SharedPreferences
        val savedUserJson = prefs.getString("session_user_json", null)
        if (savedUserJson != null) {
            try {
                _currentUser.value = userAdapter.fromJson(savedUserJson)
            } catch (e: Exception) {
                // Ignore parsing errors and reset
                prefs.edit().remove("session_user_json").apply()
            }
        }
    }

    override suspend fun login(email: String, password: String): Result<AuraUser> {
        val emailKey = email.trim().lowercase()
        if (emailKey.isEmpty() || password.isEmpty()) {
            return Result.failure(Exception("Email and password cannot be empty."))
        }
        if (!prefs.contains("user_pass_$emailKey")) {
            return Result.failure(Exception("Account not found. Please sign up first."))
        }
        val savedPass = prefs.getString("user_pass_$emailKey", "")
        if (savedPass != password) {
            return Result.failure(Exception("Wrong password. Please try again."))
        }
        val userJson = prefs.getString("user_profile_$emailKey", null) ?: return Result.failure(Exception("Account data corrupted."))
        val user = userAdapter.fromJson(userJson) ?: return Result.failure(Exception("Failed parsing account data."))
        
        persistSession(user)
        return Result.success(user)
    }

    override suspend fun signUp(email: String, password: String, displayName: String?): Result<AuraUser> {
        val emailKey = email.trim().lowercase()
        if (emailKey.isBlank() || password.length < 6) {
            return Result.failure(Exception("Email must be valid and password must be at least 6 characters."))
        }
        if (prefs.contains("user_pass_$emailKey")) {
            return Result.failure(Exception("Email is already in use by another account."))
        }
        
        val user = AuraUser(
            uid = "local_" + UUID.randomUUID().toString().take(8),
            email = email.trim(),
            displayName = if (displayName.isNullOrBlank()) email.trim().substringBefore("@") else displayName,
            photoUrl = null,
            creationDate = System.currentTimeMillis()
        )
        
        prefs.edit()
            .putString("user_pass_$emailKey", password)
            .putString("user_profile_$emailKey", userAdapter.toJson(user))
            .apply()
            
        persistSession(user)
        return Result.success(user)
    }

    override suspend fun loginWithGoogle(idToken: String, email: String, displayName: String?, photoUrl: String?): Result<AuraUser> {
        val emailKey = email.trim().lowercase()
        val userJson = prefs.getString("user_profile_$emailKey", null)
        val user = if (userJson != null) {
            userAdapter.fromJson(userJson) ?: createGoogleUser(email, displayName, photoUrl)
        } else {
            createGoogleUser(email, displayName, photoUrl)
        }
        
        prefs.edit()
            .putString("user_profile_$emailKey", userAdapter.toJson(user))
            .putString("user_pass_$emailKey", "google_sign_in_external_token")
            .apply()
            
        persistSession(user)
        return Result.success(user)
    }

    private fun createGoogleUser(email: String, displayName: String?, photoUrl: String?): AuraUser {
        val name = displayName ?: email.substringBefore("@")
        val pic = photoUrl ?: "https://lh3.googleusercontent.com/a/default-user"
        return AuraUser(
            uid = "google_" + UUID.randomUUID().toString().take(8),
            email = email,
            displayName = name,
            photoUrl = pic,
            creationDate = System.currentTimeMillis()
        )
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        val emailKey = email.trim().lowercase()
        if (!prefs.contains("user_pass_$emailKey")) {
            return Result.failure(Exception("Account not found with this email address."))
        }
        return Result.success(Unit)
    }

    override suspend fun logout(): Result<Unit> {
        prefs.edit().remove("session_user_json").apply()
        _currentUser.value = null
        return Result.success(Unit)
    }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<AuraUser> {
        val user = _currentUser.value ?: return Result.failure(Exception("Not logged in."))
        val updatedUser = user.copy(
            displayName = displayName,
            photoUrl = photoUrl
        )
        val emailKey = user.email.trim().lowercase()
        prefs.edit()
            .putString("user_profile_$emailKey", userAdapter.toJson(updatedUser))
            .apply()
        persistSession(updatedUser)
        return Result.success(updatedUser)
    }

    private fun persistSession(user: AuraUser) {
        prefs.edit().putString("session_user_json", userAdapter.toJson(user)).apply()
        _currentUser.value = user
    }
}
