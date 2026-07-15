package com.juanpvivas.aichatjp.ui.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.juanpvivas.aichatjp.ui.chat.ChatMessage

@Composable
fun ChatContent(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) {
        EmptyChatMessage(modifier = modifier)
    } else {
        ChatMessageList(messages = messages, modifier = modifier)
    }
}
