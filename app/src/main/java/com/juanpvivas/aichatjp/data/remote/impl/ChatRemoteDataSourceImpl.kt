package com.juanpvivas.aichatjp.data.remote.impl

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.exception.OpenAIAPIException
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.juanpvivas.aichatjp.data.model.ChatMessage
import com.juanpvivas.aichatjp.data.remote.config.GroqModelResolver
import com.juanpvivas.aichatjp.core.AppLogger
import com.juanpvivas.aichatjp.data.remote.ChatRemoteDataSource
import com.juanpvivas.aichatjp.data.remote.dto.SendMessageResponse
import com.juanpvivas.aichatjp.data.remote.dto.UsageDto
import com.juanpvivas.aichatjp.data.remote.mapper.ChatMessageMapper
import javax.inject.Inject

class ChatRemoteDataSourceImpl @Inject constructor(
    private val openAI: OpenAI,
    private val chatMessageMapper: ChatMessageMapper,
    private val modelResolver: GroqModelResolver
) : ChatRemoteDataSource {

    override suspend fun sendMessage(messages: List<ChatMessage>): SendMessageResponse {
        AppLogger.i("ChatRemoteDataSource.sendMessage called")

        val chatMessages = messages.map { chatMessageMapper.toChatMessage(it) }

        var modelId = modelResolver.resolveChatModel()
        val completion = try {
            openAI.chatCompletion(ChatCompletionRequest(model = ModelId(modelId), messages = chatMessages))
        } catch (e: OpenAIAPIException) {
            if (e.error.detail?.code != "model_not_found") throw e
            modelId = modelResolver.resolveChatModel(refresh = true, excluded = setOf(modelId))
            openAI.chatCompletion(ChatCompletionRequest(model = ModelId(modelId), messages = chatMessages))
        }
        val choice = completion.choices.first()

        AppLogger.d("Completion received, model=${completion.model.id}")

        return SendMessageResponse(
            content = choice.message.content ?: "",
            model = completion.model.id,
            usage = completion.usage?.let {
                UsageDto(
                    promptTokens = it.promptTokens ?: 0,
                    completionTokens = it.completionTokens ?: 0,
                    totalTokens = it.totalTokens ?: 0
                )
            }
        )
    }
}
