package com.juanpvivas.aichatjp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanpvivas.aichatjp.core.AppLogger
import com.juanpvivas.aichatjp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Empty)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var conversationId: Long? = null
    private var operation: Job? = null
    private var generation = 0L
    private var nextId = 0L

    fun sendMessage(text: String) {
        if (text.isBlank() || (_uiState.value as? ChatUiState.Success)?.isLoading == true) return
        val messages = visibleMessages() + ChatMessage(nextId++, text, true, currentTime())
        _uiState.value = ChatUiState.Success(messages, isLoading = true)
        val requestGeneration = generation
        operation = viewModelScope.launch {
            try {
                val id = conversationId ?: withContext(ioDispatcher) {
                    chatRepository.createConversation(text.take(50))
                }.also { conversationId = it }
                val response = withContext(ioDispatcher) { chatRepository.sendMessage(text, id) }
                if (requestGeneration == generation) {
                    _uiState.value = ChatUiState.Success(
                        messages + ChatMessage(nextId++, response, false, currentTime())
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("Error sending message", e)
                if (requestGeneration == generation) {
                    _uiState.value = ChatUiState.Error(e.message ?: "Error desconocido", messages)
                }
            }
        }
    }

    fun clearHistory() {
        cancelOperation()
        conversationId = null
        nextId = 0L
        _uiState.value = ChatUiState.Empty
    }

    fun loadConversation(id: Long) {
        cancelOperation()
        conversationId = id
        _uiState.value = ChatUiState.Success(emptyList(), isLoading = true)
        val requestGeneration = generation
        operation = viewModelScope.launch {
            try {
                val messages = withContext(ioDispatcher) { chatRepository.getMessagesSync(id) }
                if (requestGeneration == generation) {
                    val chatMessages = messages.map { message ->
                        ChatMessage(nextId++, message.content, message.isFromUser, currentTime(message.timestamp))
                    }
                    _uiState.value = if (chatMessages.isEmpty()) ChatUiState.Empty
                    else ChatUiState.Success(chatMessages)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("Error loading conversation", e)
                if (requestGeneration == generation) {
                    _uiState.value = ChatUiState.Error(e.message ?: "Error cargando conversacion")
                }
            }
        }
    }

    fun onConversationDeleted(id: Long) {
        if (conversationId == id) clearHistory()
    }

    private fun cancelOperation() {
        generation++
        operation?.cancel()
        operation = null
    }

    private fun visibleMessages(): List<ChatMessage> = when (val state = _uiState.value) {
        is ChatUiState.Success -> state.messages
        is ChatUiState.Error -> state.messages
        ChatUiState.Empty -> emptyList()
    }

    private fun currentTime(timestamp: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
