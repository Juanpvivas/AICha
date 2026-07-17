package com.juanpvivas.aichatjp.data.repository.impl

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.juanpvivas.aichatjp.core.AppLogger
import com.juanpvivas.aichatjp.data.remote.GroqApiClient
import com.juanpvivas.aichatjp.data.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val groqApiClient: GroqApiClient
) : ChatRepository {

    private val conversationHistory = mutableListOf<ChatMessage>()

    override suspend fun sendMessage(userMessage: String): String {
        AppLogger.i("ChatRepository.sendMessage called")

        conversationHistory.add(
            ChatMessage(
                role = ChatRole.User,
                content = userMessage
            )
        )

        val assistantContent = groqApiClient.chatCompletion(conversationHistory)

        conversationHistory.add(
            ChatMessage(
                role = ChatRole.Assistant,
                content = assistantContent
            )
        )

        return assistantContent
    }

    override fun clearHistory() {
        conversationHistory.clear()
    }
}
