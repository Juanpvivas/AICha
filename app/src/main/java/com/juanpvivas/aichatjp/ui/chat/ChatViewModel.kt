package com.juanpvivas.aichatjp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanpvivas.aichatjp.core.AppLogger
import com.juanpvivas.aichatjp.data.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Empty)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var nextId = 0L

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        AppLogger.i("sendMessage: $text")

        val userMessage = ChatMessage(
            id = nextId++,
            text = text,
            fromUser = true,
            time = currentTime()
        )

        _uiState.update { currentState ->
            val messages = when (currentState) {
                is ChatUiState.Empty -> listOf(userMessage)
                is ChatUiState.Success -> currentState.messages + userMessage
                is ChatUiState.Error -> listOf(userMessage)
            }
            ChatUiState.Success(messages, isLoading = true)
        }

        viewModelScope.launch {
            try {
                val response = chatRepository.sendMessage(text)
                AppLogger.i("Response received: ${response.take(100)}")

                val aiMessage = ChatMessage(
                    id = nextId++,
                    text = response,
                    fromUser = false,
                    time = currentTime()
                )

                _uiState.update { currentState ->
                    val messages = when (currentState) {
                        is ChatUiState.Success -> currentState.messages + aiMessage
                        else -> listOf(aiMessage)
                    }
                    ChatUiState.Success(messages)
                }
            } catch (e: Exception) {
                AppLogger.e("Error sending message", e)
                _uiState.update { currentState ->
                    val messages = when (currentState) {
                        is ChatUiState.Success -> currentState.messages
                        else -> emptyList()
                    }
                    ChatUiState.Error(e.message ?: "Error desconocido")
                }
            }
        }
    }

    private fun currentTime(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
