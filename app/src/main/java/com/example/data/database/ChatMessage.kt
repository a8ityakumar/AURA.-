package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

data class SourceInfo(
    val title: String,
    val url: String
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourcesJson: String? = null
) {
    fun getSources(): List<SourceInfo> {
        if (sourcesJson.isNullOrBlank()) return emptyList()
        return sourcesJson.split("\n").mapNotNull { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                SourceInfo(title = parts[0], url = parts[1])
            } else {
                null
            }
        }
    }
}
