package com.juanpvivas.aichatjp.ui.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

data class ChatMessage(
    val content: String,
    val isFromUser: Boolean
)

class ChatViewModel : ViewModel() {

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _messages.add(ChatMessage(content = text, isFromUser = true))
    }
}
