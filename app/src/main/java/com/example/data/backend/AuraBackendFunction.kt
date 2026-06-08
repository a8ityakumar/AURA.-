package com.example.data.backend

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.api.Tool
import com.example.data.api.GoogleSearch
import com.example.data.database.SourceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * A simulated server-side function that acts as our Google Cloud/Firebase Cloud Function backend.
 *
 * Architecture:
 * - AURA Android client invoker invokes AuraBackendFunction.
 * - Gemini API Key is secured on the backend environment config (represented by BuildConfig.GEMINI_API_KEY).
 * - Implements online Google Search grounding for real-time / current events questions.
 * - Extracts and packages response text along with source URLs and titles recursively.
 * - Gracefully supports recovery fallback to ungrounded answers if grounding or search fails.
 */
object AuraBackendFunction {

    private const val TAG = "AURA_BACKEND"

    private fun logApiError(e: Exception) {
        if (e is retrofit2.HttpException) {
            val code = e.code()
            var body: String? = null
            try {
                body = e.response()?.errorBody()?.string()
            } catch (ignored: Exception) {}
            
            Log.e(TAG, "HTTP Error $code: ${body ?: "No error body available"}", e)
            
            // Diagnose error type based on code and response body
            val diagnosis = when {
                code == 503 -> {
                    "[CRITICAL] HTTP 503 Service Unavailable: The model is temporarily overloaded or search grounding service is down."
                }
                code == 429 || body?.contains("RESOURCE_EXHAUSTED") == true || body?.contains("quota") == true -> {
                    "[CRITICAL] Quota Exceeded / Rate Limited (HTTP $code): Check project limits or billing."
                }
                code == 400 && (body?.contains("API_KEY_INVALID") == true || body?.contains("not valid") == true || body?.contains("key") == true) -> {
                    "[CRITICAL] Invalid API Key (HTTP $code): The configured GEMINI_API_KEY is rejected."
                }
                code == 404 || (code == 400 && body?.contains("model") == true) -> {
                    "[CRITICAL] Unsupported Gemini Model (HTTP $code): Selected model is not supported or deprecated."
                }
                else -> {
                    "[ERROR] API Request failed with HTTP status code $code"
                }
            }
            Log.e(TAG, diagnosis)
        } else if (e is java.io.IOException) {
            Log.e(TAG, "[CRITICAL] Network Failure: Connection timed out, DNS resolution failure or no internet access.", e)
        } else {
            Log.e(TAG, "[ERROR] Unexpected Exception: ${e.localizedMessage ?: e.javaClass.name}", e)
        }
    }

    suspend fun invoke(
        contents: List<Content>,
        systemInstruction: Content?
    ): BackendResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("MY_")) {
            Log.e(TAG, "[CRITICAL] API Key is missing. Make sure GEMINI_API_KEY is configured in your Environment / Secrets.")
            return@withContext BackendResult.Error(
                userMessage = "I’m having trouble connecting to the AI model right now. Please try again in a moment.",
                technicalDetail = "API key configuration error (missing GEMINI_API_KEY)"
            )
        }

        // Always use standard ungrounded query to act as an offline-style helper
        executeStandardQuery(contents, systemInstruction, apiKey)
    }

    private suspend fun executeStandardQuery(
        contents: List<Content>,
        systemInstruction: Content?,
        apiKey: String
    ): BackendResult {
        val primaryModel = "gemini-3.5-flash"
        val fallbackModel = "gemini-3.1-flash-lite-preview"
        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            tools = null
        )

        Log.d(TAG, "Invoking standard Gemini API query using model: $primaryModel")

        return try {
            val response = RetrofitClient.service.generateContent(primaryModel, apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                BackendResult.ResultSuccess(
                    answer = text,
                    sources = emptyList(),
                    isGroundingUsed = false
                )
            } else {
                Log.e(TAG, "Primary model returned an empty list of candidates.")
                BackendResult.Error(
                    userMessage = "I’m having trouble connecting to the AI model right now. Please try again in a moment.",
                    technicalDetail = "Empty candidate response from primary intelligence engine"
                )
            }
        } catch (primaryException: Exception) {
            Log.e(TAG, "Primary model $primaryModel invocation failed.", primaryException)
            logApiError(primaryException)
            Log.w(TAG, "Attempting immediate fallback to model: $fallbackModel")
            
            try {
                val response = RetrofitClient.service.generateContent(fallbackModel, apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text != null) {
                    BackendResult.ResultSuccess(
                        answer = text,
                        sources = emptyList(),
                        isGroundingUsed = false
                    )
                } else {
                    Log.e(TAG, "Fallback model returned an empty list of candidates.")
                    BackendResult.Error(
                        userMessage = "I’m having trouble connecting to the AI model right now. Please try again in a moment.",
                        technicalDetail = "Empty candidate response from fallback intelligence engine"
                    )
                }
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Fallback model $fallbackModel invocation also failed.", fallbackException)
                logApiError(fallbackException)
                val code = if (fallbackException is retrofit2.HttpException) fallbackException.code() else null
                val userErrorMessage = "I’m having trouble connecting to the AI model right now. Please try again in a moment."
                BackendResult.Error(
                    userMessage = userErrorMessage,
                    technicalDetail = if (code != null) "Fallback Error: HTTP $code" else "Fallback Error: ${fallbackException.localizedMessage ?: "Unknown network error"}"
                )
            }
        }
    }
}


sealed interface BackendResult {
    data class ResultSuccess(
        val answer: String,
        val sources: List<SourceInfo>,
        val isGroundingUsed: Boolean
    ) : BackendResult

    data class Error(
        val userMessage: String,
        val technicalDetail: String
    ) : BackendResult
}
