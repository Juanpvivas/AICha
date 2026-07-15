package com.juanpvivas.aichatjp.ui.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel : ViewModel() {

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private var nextId = 0L

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _messages.add(
            ChatMessage(
                id = nextId++,
                text = text,
                fromUser = true,
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            )
        )
    }
}
