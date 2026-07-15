package com.juanpvivas.aichatjp.ui.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.juanpvivas.aichatjp.ui.chat.ChatUiState

@Composable
fun ChatContent(
    uiState: ChatUiState,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is ChatUiState.Empty -> ChatEmptyMessage(modifier = modifier)
        is ChatUiState.Success -> ChatMessageList(
            messages = uiState.messages,
            modifier = modifier
        )
    }
}
