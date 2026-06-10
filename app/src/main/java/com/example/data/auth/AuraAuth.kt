package com.example.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object AuraAuth {
    private const val TAG = "AuraAuth"
    
    @Volatile
    private var _instance: AuthService? = null
    
    val instance: AuthService
        get() = _instance ?: throw IllegalStateException("AuraAuth is not initialized. Please call AuraAuth.init(context) first.")

    fun init(context: Context): AuthService {
        return _instance ?: synchronized(this) {
            _instance ?: run {
                val service = resolveAuthService(context.applicationContext)
                _instance = service
                service
            }
        }
    }

    private fun resolveAuthService(context: Context): AuthService {
        // 1. Check if Firebase is already initialized
        val initializedApp = try {
            FirebaseApp.getInstance()
        } catch (e: Exception) {
            null
        }

        if (initializedApp != null) {
            Log.d(TAG, "Firebase already initialized automatically. Returning FirebaseAuthenticationService.")
            return FirebaseAuthenticationService()
        }

        // 2. Try to initialize using google-services plugin resources if they exist
        val googleServicesResId = context.resources.getIdentifier("google_app_id", "string", context.packageName)
        if (googleServicesResId != 0) {
            try {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "Firebase initialized using auto-generated google-services resources.")
                return FirebaseAuthenticationService()
            } catch (e: Exception) {
                Log.e(TAG, "Failed initializing Firebase from auto-generated resources", e)
            }
        }

        // 3. Check for .env fallback manually defined properties
        val apiKey = try { com.example.BuildConfig.FIREBASE_API_KEY } catch (_: Exception) { "" }
        val appId = try { com.example.BuildConfig.FIREBASE_APPLICATION_ID } catch (_: Exception) { "" }
        val projectId = try { com.example.BuildConfig.FIREBASE_PROJECT_ID } catch (_: Exception) { "" }

        val isKeysConfigured = !apiKey.isNullOrBlank() && 
                               apiKey != "MY_FIREBASE_API_KEY" && 
                               !appId.isNullOrBlank() && 
                               appId != "MY_FIREBASE_APPLICATION_ID" &&
                               !projectId.isNullOrBlank() && 
                               projectId != "MY_FIREBASE_PROJECT_ID"

        if (isKeysConfigured) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "Firebase initialized programmatically using custom env configuration.")
                return FirebaseAuthenticationService()
            } catch (e: Exception) {
                Log.e(TAG, "Failed programmatic Firebase init using env keys", e)
            }
        }

        // 4. Fallback to Local Sandbox
        Log.w(TAG, "No valid Firebase configuration found (no google-services.json resources, env keys missing, or runtime error). Activating Local Sandbox Mode.")
        return LocalAuthenticationService(context)
    }
}
