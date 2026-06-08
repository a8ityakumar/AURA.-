package com.example.data.repository

import com.example.data.database.ChatDao
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessages(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun getMessagesSync(sessionId: String): List<ChatMessage> {
        return chatDao.getMessagesForSessionSync(sessionId)
    }

    suspend fun createSession(session: ChatSession) {
        chatDao.insertSession(session)
    }

    suspend fun saveMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }
}
