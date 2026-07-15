package com.juanpvivas.aichatjp.ui.chat

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Empty)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var nextId = 0L

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val message = ChatMessage(
            id = nextId++,
            text = text,
            fromUser = true,
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        )

        _uiState.update { currentState ->
            val messages = when (currentState) {
                is ChatUiState.Empty -> listOf(message)
                is ChatUiState.Success -> currentState.messages + message
            }
            ChatUiState.Success(messages)
        }
    }
}
