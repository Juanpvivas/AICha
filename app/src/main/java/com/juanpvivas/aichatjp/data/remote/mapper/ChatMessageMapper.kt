package com.juanpvivas.aichatjp.data.remote.mapper

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import javax.inject.Inject

class ChatMessageMapper @Inject constructor() {

    fun toChatMessage(message: com.juanpvivas.aichatjp.data.model.ChatMessage): ChatMessage {
        return ChatMessage(
            role = if (message.isFromUser) ChatRole.User else ChatRole.Assistant,
            content = message.content
        )
    }
}
