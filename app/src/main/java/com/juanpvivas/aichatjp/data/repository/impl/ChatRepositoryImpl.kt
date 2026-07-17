package com.juanpvivas.aichatjp.data.repository.impl

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.juanpvivas.aichatjp.core.AppLogger
import com.juanpvivas.aichatjp.data.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val openAI: OpenAI
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

        val request = ChatCompletionRequest(
            model = ModelId("llama-3.3-70b-versatile"),
            messages = conversationHistory
        )

        AppLogger.d("Request: model=${request.model.id}, messages=${request.messages.size}")

        val completion = openAI.chatCompletion(request)

        AppLogger.d("Completion received, choices=${completion.choices.size}")

        val assistantContent = completion.choices.first().message.content ?: ""

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
