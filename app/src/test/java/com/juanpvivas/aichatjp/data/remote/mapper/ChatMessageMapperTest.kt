package com.juanpvivas.aichatjp.data.remote.mapper

import com.aallam.openai.api.chat.ChatRole
import com.juanpvivas.aichatjp.data.model.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageMapperTest {
    @Test fun `user and assistant roles survive mapping to API messages`() {
        val mapper = ChatMessageMapper()
        val user = mapper.toChatMessage(ChatMessage(1, 7, "Question", true, 0))
        val assistant = mapper.toChatMessage(ChatMessage(2, 7, "Answer", false, 1))
        assertEquals(ChatRole.User, user.role)
        assertEquals(ChatRole.Assistant, assistant.role)
        assertEquals("Answer", assistant.content)
    }
}
