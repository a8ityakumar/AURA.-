package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import com.example.data.repository.ChatRepository
import com.example.data.backend.AuraBackendFunction
import com.example.data.backend.BackendResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ChatUiState {
    object Idle : ChatUiState
    object Loading : ChatUiState
    data class Success(val response: String) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _apiKeyError = MutableStateFlow<String?>(null)
    val apiKeyError: StateFlow<String?> = _apiKeyError.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isDevMode = MutableStateFlow(false)
    val isDevMode: StateFlow<Boolean> = _isDevMode.asStateFlow()

    init {
        // Simple security check for API key
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank() || key == "MY_GEMINI_API_KEY" || key.contains("MY_")) {
            _apiKeyError.value = "API key is missing or not configured. Please use the Secrets panel in Google AI Studio to configure your GEMINI_API_KEY."
            // Default to mock mode to guarantee instant and responsive app-only operations without credentials
            _isDevMode.value = true
        }
    }

    fun toggleDevMode() {
        _isDevMode.value = !_isDevMode.value
    }

    // Expose all chat sessions as a reactive stream
    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactively switch the messages Flow whenever the active session ID changes
    val currentMessages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessages(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectSession(sessionId: String?) {
        _currentSessionId.value = sessionId
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun startNewSession(title: String = "New Conversation") {
        val newId = UUID.randomUUID().toString()
        viewModelScope.launch {
            val session = ChatSession(id = newId, title = title)
            repository.createSession(session)
            _currentSessionId.value = newId
        }
    }

    fun clearAllCurrentMessages() {
        val sessionId = _currentSessionId.value
        if (sessionId != null) {
            viewModelScope.launch {
                repository.deleteSession(sessionId)
                // Re-create session to keep it active as a clean thread
                val session = ChatSession(id = sessionId, title = "New Conversation")
                repository.createSession(session)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // 1. Ensure active chat session exists, create one if not
            var sessionId = _currentSessionId.value
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString()
                val shortTitle = if (text.length > 25) text.take(22) + "..." else text
                val session = ChatSession(id = sessionId, title = shortTitle)
                repository.createSession(session)
                _currentSessionId.value = sessionId
            }

            // 2. Save user message locally
            val userMsg = ChatMessage(
                sessionId = sessionId,
                role = "user",
                content = text
            )
            repository.saveMessage(userMsg)

            // 3. Mark generation as active
            _isGenerating.value = true

            // Fast local mock mode to simulate super-smooth rendering and bypass backend/API overhead
            if (_isDevMode.value) {
                kotlinx.coroutines.delay(200) // Small tactile delay for realistic interaction flow
                
                val lowerMessage = text.lowercase().trim()
                val responseContent = when {
                    lowerMessage.contains("strategy") || lowerMessage.contains("plan") || lowerMessage.contains("analyze") -> """
                        ### AURA STRATEGIC REFINEMENT

                        I have analyzed your request. Here is an optimized minimalist feature plan:
                        
                        1. **Core Canvas isolation**: Remove all background orchestration noise.
                        2. **Direct Execution loop**: Capture input events and render lightweight state immediately.
                        3. **Zero-friction rendering**: Eliminate heavy animation states.
                        
                        *Status: Tactical model fully aligned.*
                    """.trimIndent()
                    lowerMessage.contains("ui") || lowerMessage.contains("theme") || lowerMessage.contains("refine") -> """
                        ### AURA DESIGN DIRECTIVE

                        Your interface is utilizing the **Clean Minimalism** system:
                        - **Canvas**: Pure Black (`#000000`)
                        - **Typography**: Inter (High weight-contrast pairings)
                        - **Visual Accent**: Pure White boundaries and low-opacity circular gradients
                        
                        This delivers high readability without compromising hardware performance.
                    """.trimIndent()
                    else -> """
                        ### AURA INTENT PROCESSED

                        Your intention has been recognized successfully:
                        
                        > "$text"
                        
                        All operations have been verified within the local, lightweight high-performance environment. No network latency was introduced.
                    """.trimIndent()
                }

                val mockMsg = ChatMessage(
                    sessionId = sessionId,
                    role = "model",
                    content = responseContent
                )
                repository.saveMessage(mockMsg)
                
                // Update session title dynamically after first exchange
                val sessionsList = sessions.value
                val activeSession = sessionsList.find { it.id == sessionId }
                if (activeSession != null && (activeSession.title == "New Conversation" || activeSession.title == "New Chat")) {
                    val newTitle = if (text.length > 25) text.take(22) + "..." else text
                    repository.createSession(activeSession.copy(title = newTitle))
                }
                
                _isGenerating.value = false
                return@launch
            }

            // 4. Fetch the entire thread history for contextual response
            val history = repository.getMessagesSync(sessionId)
            val contents = history.map { msg ->
                Content(
                    role = if (msg.role == "user") "user" else "model",
                    parts = listOf(Part(text = msg.content))
                )
            }

            // 5. Build system instruction configuration for the backend call
            val systemInstruction = Content(
                parts = listOf(Part(text = """
                    You are AURA., an advanced AI assistant designed to provide accurate, structured, intelligent, and reliable responses.

                    AURA. is a completely offline, free-version AI assistant. Online search, live connectivity, and direct web access are disabled.

                    AURA. must never pretend to search online, click links, access websites, view social media accounts (like Instagram, Google, YouTube), check server status pages, or query live updates.

                    PRIMARY OBJECTIVES
                    - Accurate
                    - Organized
                    - Easy to read
                    - Helpful
                    - Fact-based
                    - Actionable

                    RESPONSE FORMAT OR WRITING STYLE
                    Every response must follow this structure unless specifically requested otherwise:
                    1. Direct Answer
                    2. Key Explanation / Details
                    3. Additional Context, Recommendations, or Limitation Notice (if applicable)

                    Formatting Rules:
                    - Use Material Design 3 Markdown heading hierarchies (# Main Topic, ## Major Section, ### Subsection). Keep headings clean and visually organized.
                    - Always add a blank line between every section and paragraph to assist with readability.
                    - Maximum paragraph length: 3 lines. If a paragraph exceeds 3 lines, split it.
                    - Use lists and bullet points frequently. Avoid large walls of text.

                    CURRENT AND LATEST INFORMATION CONSTRAINT
                    If the user asks about latest, current, live, today, now, recent, prices, news, social media profiles, server status, exam updates, laws, finance, health, or anything that may change over time, AURA. must not guess and must not invent facts.

                    For any such request regarding current or live information, AURA. must respond exactly with:

                    “I can’t verify current information because online search is not enabled right now. If you share a link, screenshot, or copied text, I can analyze it.”

                    UPLOADED AND USER-PROVIDED CONTENT RULE
                    AURA. can analyze any information, text, document, screenshot, bio, profile, or link content that the user provides directly in the chat or as an attachment.

                    When analyzing user-provided content, AURA. must clearly separate:
                    - Verified from provided information (what is explicitly shown or stated)
                    - What is not confirmed (potential gaps or unverified parts)
                    - Assumptions not to make
                    - Extra information or context needed to make a complete analysis

                    RESTORE NORMAL AI CHAT
                    For stable, general, or creative topics, AURA. responds with rich, accurate, and detailed knowledge. This includes:
                    - General concept explanations, comparisons, and studies.
                    - Writing, proofreading, and rewriting.
                    - Coding assistance, debugging, and software architecture (using stable, known versions).
                    - Summarizing content provided directly by the user.
                    - Brainstorming, math, logic, and planning.
                    - Creative stories, general advice, and historical facts.

                    CHAT DISTINCTION RULES
                    Every AURA. response should begin with:

                    AURA.
                    ────────────────────

                    followed by a blank line and then the content.

                    This makes it immediately obvious that the response came from AURA.
                """.trimIndent()))
            )

            try {
                // Delegate computation entirely to secure simulated AuraBackendFunction function
                val result = AuraBackendFunction.invoke(contents, systemInstruction)

                val (responseText, sourcesJson) = when (result) {
                    is BackendResult.ResultSuccess -> {
                        val serSources = if (result.sources.isNotEmpty()) {
                            result.sources.joinToString("\n") { "${it.title}|${it.url}" }
                        } else {
                            null
                        }
                        Pair(result.answer, serSources)
                    }
                    is BackendResult.Error -> {
                        Pair(result.userMessage, null)
                    }
                }

                // Save assistant message locally with parsed sources
                val assistantMsg = ChatMessage(
                    sessionId = sessionId,
                    role = "model",
                    content = responseText,
                    sourcesJson = sourcesJson
                )
                repository.saveMessage(assistantMsg)

                // Update session title dynamically after first exchange if it was the default
                val sessionsList = sessions.value
                val activeSession = sessionsList.find { it.id == sessionId }
                if (activeSession != null && (activeSession.title == "New Conversation" || activeSession.title == "New Chat")) {
                    val newTitle = if (text.length > 25) text.take(22) + "..." else text
                    repository.createSession(activeSession.copy(title = newTitle))
                }

            } catch (e: Exception) {
                val code = if (e is retrofit2.HttpException) e.code() else null
                val userFriendlyMessage = "I’m having trouble connecting right now. This may be a temporary server or network issue. Please try again in a moment."
                val technicalDetail = if (code != null) "Error: HTTP $code" else "Error: ${e.localizedMessage ?: "Unknown network error"}"
                
                val errorMsg = ChatMessage(
                    sessionId = sessionId,
                    role = "model",
                    content = "$userFriendlyMessage\n\n$technicalDetail",
                    sourcesJson = null
                )
                repository.saveMessage(errorMsg)
            } finally {
                _isGenerating.value = false
            }
        }
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
