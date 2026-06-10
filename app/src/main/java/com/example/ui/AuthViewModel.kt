package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuraAuth
import com.example.data.auth.AuraUser
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val user: AuraUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

sealed interface ProfileUiState {
    object Idle : ProfileUiState
    object Loading : ProfileUiState
    object Success : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authService = AuraAuth.init(application)

    val currentUser: StateFlow<AuraUser?> = authService.currentUser
    
    val isRealFirebase: Boolean = authService.isRealFirebase
    val currentDiagnosticMessage: String? = authService.currentDiagnosticMessage

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    fun clearProfileUiState() {
        _profileUiState.value = ProfileUiState.Idle
    }

    fun setCustomError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email and password.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authService.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Login failed. Please check credentials.")
                }
            )
        }
    }

    fun signUp(email: String, password: String, displayName: String?) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter email and password.")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authService.signUp(email, password, displayName)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Sign-up failed.")
                }
            )
        }
    }

    fun loginWithGoogle(idToken: String, email: String, displayName: String?, photoUrl: String?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authService.loginWithGoogle(idToken, email, displayName, photoUrl)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Success(user)
                },
                onFailure = { error ->
                    Log.e("AuthViewModel", "Google Firebase authentication mapping failed: ", error)
                    _uiState.value = AuthUiState.Error("Google Sign-In failed. Please use email login for now.")
                }
            )
        }
    }

    fun resetPassword(email: String, onSuccess: () -> Unit) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email address.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authService.resetPassword(email)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState.Idle
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Password reset failed.")
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authService.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun saveProfile(
        displayName: String,
        imageUri: Uri?,
        removeAvatar: Boolean,
        context: Context,
        onSuccess: () -> Unit
    ) {
        val user = currentUser.value ?: run {
            _profileUiState.value = ProfileUiState.Error("User not logged in.")
            return
        }

        viewModelScope.launch {
            _profileUiState.value = ProfileUiState.Loading
            try {
                var finalPhotoUrl: String? = user.photoUrl

                if (removeAvatar) {
                    finalPhotoUrl = null
                } else if (imageUri != null) {
                    val compressedFile = processAndCompressImage(context, imageUri)
                    if (compressedFile == null) {
                        _profileUiState.value = ProfileUiState.Error("Failed to process or compress selected image.")
                        return@launch
                    }

                    if (isRealFirebase) {
                        val storageRef = FirebaseStorage.getInstance().reference
                            .child("users")
                            .child(user.uid)
                            .child("profile")
                            .child("profile_picture.jpg")

                        val uploadUri = Uri.fromFile(compressedFile)
                        val uploadTask = storageRef.putFile(uploadUri)

                        val downloadUrl = suspendCancellableCoroutine<String> { continuation ->
                            uploadTask.addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { url ->
                                    continuation.resume(url.toString())
                                }.addOnFailureListener { e ->
                                    continuation.resumeWithException(e)
                                }
                            }.addOnFailureListener { e ->
                                continuation.resumeWithException(e)
                            }
                        }
                        finalPhotoUrl = downloadUrl
                    } else {
                        val localFile = File(context.filesDir, "profile_${user.uid}.jpg")
                        compressedFile.copyTo(localFile, overwrite = true)
                        finalPhotoUrl = Uri.fromFile(localFile).toString()
                    }
                }

                val updateResult = authService.updateProfile(displayName.trim(), finalPhotoUrl)
                updateResult.fold(
                    onSuccess = {
                        _profileUiState.value = ProfileUiState.Success
                        onSuccess()
                    },
                    onFailure = { error ->
                        _profileUiState.value = ProfileUiState.Error(error.localizedMessage ?: "Failed to save profile.")
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profile save exception: ", e)
                _profileUiState.value = ProfileUiState.Error(e.localizedMessage ?: "Save failed due to an unexpected error.")
            }
        }
    }

    private fun processAndCompressImage(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()

            val width = originalBitmap.width
            val height = originalBitmap.height
            val size = kotlin.math.min(width, height)
            val x = (width - size) / 2
            val y = (height - size) / 2
            val squareBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)

            val targetSize = 400
            val scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)

            val tempFile = File(context.cacheDir, "profile_pic_temp.jpg")
            if (tempFile.exists()) tempFile.delete()

            val out = FileOutputStream(tempFile)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.flush()
            out.close()

            if (originalBitmap != squareBitmap) originalBitmap.recycle()
            if (squareBitmap != scaledBitmap) squareBitmap.recycle()
            scaledBitmap.recycle()

            tempFile
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Process image error: ", e)
            null
        }
    }
}
