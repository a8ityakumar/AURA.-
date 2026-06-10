package com.example.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseAuthenticationService : AuthService {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val _currentUser = MutableStateFlow<AuraUser?>(null)
    override val currentUser: StateFlow<AuraUser?> = _currentUser.asStateFlow()

    override val isRealFirebase: Boolean = true
    override val currentDiagnosticMessage: String? = null

    init {
        // Sync initial state and register listener
        updateCurrentUser()
        auth.addAuthStateListener {
            updateCurrentUser()
        }
    }

    private fun updateCurrentUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            _currentUser.value = AuraUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString(),
                creationDate = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
            )
        } else {
            _currentUser.value = null
        }
    }

    override suspend fun login(email: String, password: String): Result<AuraUser> = suspendCancellableCoroutine { continuation ->
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val authUser = AuraUser(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName,
                            photoUrl = firebaseUser.photoUrl?.toString(),
                            creationDate = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                        )
                        syncUserToFirestore(authUser)
                        continuation.resume(Result.success(authUser))
                    } else {
                        continuation.resume(Result.failure(Exception("Failed to retrieve user after login.")))
                    }
                } else {
                    val ex = task.exception ?: Exception("Authentication failed.")
                    continuation.resume(Result.failure(ex))
                }
            }
    }

    override suspend fun signUp(email: String, password: String, displayName: String?): Result<AuraUser> = suspendCancellableCoroutine { continuation ->
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        if (!displayName.isNullOrBlank()) {
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build()
                            firebaseUser.updateProfile(profileUpdates)
                                .addOnCompleteListener {
                                    val authUser = AuraUser(
                                        uid = firebaseUser.uid,
                                        email = firebaseUser.email ?: "",
                                        displayName = displayName,
                                        photoUrl = null,
                                        creationDate = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                                    )
                                    syncUserToFirestore(authUser)
                                    continuation.resume(Result.success(authUser))
                                }
                        } else {
                            val authUser = AuraUser(
                                uid = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                displayName = null,
                                photoUrl = null,
                                creationDate = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                            )
                            syncUserToFirestore(authUser)
                            continuation.resume(Result.success(authUser))
                        }
                    } else {
                        continuation.resume(Result.failure(Exception("Failed to retrieve user after signup.")))
                    }
                } else {
                    val ex = task.exception ?: Exception("Sign-up failed.")
                    continuation.resume(Result.failure(ex))
                }
            }
    }

    override suspend fun loginWithGoogle(idToken: String, email: String, displayName: String?, photoUrl: String?): Result<AuraUser> = suspendCancellableCoroutine { continuation ->
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val authUser = AuraUser(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName,
                            photoUrl = firebaseUser.photoUrl?.toString(),
                            creationDate = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                        )
                        syncUserToFirestore(authUser)
                        continuation.resume(Result.success(authUser))
                    } else {
                        continuation.resume(Result.failure(Exception("Failed to retrieve user after Google login.")))
                    }
                } else {
                    val ex = task.exception ?: Exception("Google sign-in credential registration failed.")
                    continuation.resume(Result.failure(ex))
                }
            }
    }

    override suspend fun resetPassword(email: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Result.success(Unit))
                } else {
                    val ex = task.exception ?: Exception("Password reset email dispatch failed.")
                    continuation.resume(Result.failure(ex))
                }
            }
    }

    override suspend fun logout(): Result<Unit> {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
        return Result.success(Unit)
    }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?): Result<AuraUser> = suspendCancellableCoroutine { continuation ->
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            continuation.resume(Result.failure(Exception("No user logged in.")))
            return@suspendCancellableCoroutine
        }

        val profileUpdates = UserProfileChangeRequest.Builder().apply {
            if (displayName != null) {
                setDisplayName(displayName)
            }
            if (photoUrl != null) {
                setPhotoUri(android.net.Uri.parse(photoUrl))
            } else {
                setPhotoUri(null)
            }
        }.build()

        firebaseUser.updateProfile(profileUpdates)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val uid = firebaseUser.uid
                    val email = firebaseUser.email ?: ""
                    val finalDisplayName = displayName ?: firebaseUser.displayName
                    val finalPhotoUrl = photoUrl ?: firebaseUser.photoUrl?.toString()
                    val creationDate = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()

                    val updatedUser = AuraUser(
                        uid = uid,
                        email = email,
                        displayName = finalDisplayName,
                        photoUrl = finalPhotoUrl,
                        creationDate = creationDate
                    )

                    _currentUser.value = updatedUser
                    syncUserToFirestore(updatedUser)
                    continuation.resume(Result.success(updatedUser))
                } else {
                    val ex = authTask.exception ?: Exception("Failed to update Firebase Auth profile.")
                    continuation.resume(Result.failure(ex))
                }
            }
    }

    private fun syncUserToFirestore(user: AuraUser) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(user.uid)
            val userData = hashMapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoURL" to user.photoUrl,
                "createdAt" to user.creationDate,
                "updatedAt" to System.currentTimeMillis()
            )
            userDoc.set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    android.util.Log.d("FirebaseAuthService", "Profile synced to Firestore.")
                }
                .addOnFailureListener { e ->
                    android.util.Log.w("FirebaseAuthService", "Failed to sync to Firestore: ", e)
                }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthService", "Firestore not available: ${e.localizedMessage}")
        }
    }
}
