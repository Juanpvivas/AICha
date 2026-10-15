package com.juanpvivas.aichatjp.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.juanpvivas.aichatjp.ui.chat.ChatUiState

@Composable
fun ChatContent(uiState: ChatUiState, modifier: Modifier = Modifier) {
    when (uiState) {
        ChatUiState.Empty -> ChatEmptyMessage(modifier = modifier)
        is ChatUiState.Success -> Box(modifier.fillMaxSize()) {
            ChatMessageList(messages = uiState.messages)
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
        is ChatUiState.Error -> Column(modifier.fillMaxSize()) {
            ChatMessageList(messages = uiState.messages, modifier = Modifier.weight(1f))
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}
