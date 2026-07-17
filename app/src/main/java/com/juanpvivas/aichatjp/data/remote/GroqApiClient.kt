package com.juanpvivas.aichatjp.data.remote

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.juanpvivas.aichatjp.core.AppLogger
import javax.inject.Inject

class GroqApiClient @Inject constructor(
    private val openAI: OpenAI
) {
    suspend fun chatCompletion(messages: List<ChatMessage>): String {
        val request = ChatCompletionRequest(
            model = ModelId("llama-3.3-70b-versatile"),
            messages = messages
        )

        AppLogger.d("GroqApiClient: model=${request.model.id}, messages=${messages.size}")

        val completion = openAI.chatCompletion(request)

        AppLogger.d("GroqApiClient: choices=${completion.choices.size}")

        return completion.choices.first().message.content ?: ""
    }
}
