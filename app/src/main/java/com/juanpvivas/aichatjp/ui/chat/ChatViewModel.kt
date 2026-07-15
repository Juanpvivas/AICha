package com.juanpvivas.aichatjp.ui.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
    val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

class ChatViewModel : ViewModel() {

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _messages.add(ChatMessage(text = text, fromUser = true))
    }
}
