package com.juanpvivas.aichatjp.data.repository

interface ChatRepository {
    suspend fun sendMessage(userMessage: String): String
    fun clearHistory()
}
