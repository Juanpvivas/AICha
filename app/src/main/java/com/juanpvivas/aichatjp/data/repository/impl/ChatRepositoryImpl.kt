package com.juanpvivas.aichatjp.data.repository.impl

import com.juanpvivas.aichatjp.core.AppLogger
import com.juanpvivas.aichatjp.data.remote.ChatRemoteDataSource
import com.juanpvivas.aichatjp.data.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatRemoteDataSource: ChatRemoteDataSource
) : ChatRepository {

    private val conversationHistory = mutableListOf<String>()

    override suspend fun sendMessage(userMessage: String): String {
        AppLogger.i("ChatRepository.sendMessage called")

        conversationHistory.add(userMessage)

        val response = chatRemoteDataSource.sendMessage(conversationHistory)

        conversationHistory.add(response.content)

        return response.content
    }

    override fun clearHistory() {
        conversationHistory.clear()
    }
}
