package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null, // "user" or "model"
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GoogleSearch(
    val placeholder: String? = null // Represent {}
)

@JsonClass(generateAdapter = true)
data class Tool(
    @Json(name = "google_search") val googleSearch: GoogleSearch? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    @Json(name = "system_instruction") val systemInstruction: Content? = null,
    val tools: List<Tool>? = null
)

@JsonClass(generateAdapter = true)
data class WebSource(
    val uri: String? = null,
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class GroundingChunk(
    val web: WebSource? = null
)

@JsonClass(generateAdapter = true)
data class GroundingMetadata(
    @Json(name = "web_search_queries") val webSearchQueries: List<String>? = null,
    @Json(name = "grounding_chunks") val groundingChunks: List<GroundingChunk>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    @Json(name = "finish_reason") val finishReason: String? = null,
    @Json(name = "grounding_metadata") val groundingMetadata: GroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)
