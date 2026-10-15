package com.juanpvivas.aichatjp.ui.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.juanpvivas.aichatjp.ui.chat.components.ChatContent
import com.juanpvivas.aichatjp.ui.chat.components.ChatInputBar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedRequestKeepsMessagesVisibleAlongsideError() {
        compose.setContent {
            MaterialTheme {
                ChatContent(ChatUiState.Error("Offline", listOf(
                    ChatMessage(1, "Previous question", true, "12:00"),
                    ChatMessage(2, "Previous answer", false, "12:01")
                )))
            }
        }
        compose.onNodeWithText("Previous question").assertIsDisplayed()
        compose.onNodeWithText("Previous answer").assertIsDisplayed()
        compose.onNodeWithText("Offline").assertIsDisplayed()
    }

    @Test fun pendingRequestDoesNotSendOrDiscardDraft() {
        val enabled = mutableStateOf(false)
        val sent = mutableListOf<String>()
        compose.setContent { MaterialTheme { ChatInputBar(onSend = { sent.add(it) }, enabled = enabled.value) } }
        compose.onNode(hasSetTextAction()).performTextInput("Draft")
        compose.onNode(hasSetTextAction()).performImeAction()
        assertEquals(emptyList<String>(), sent)
        compose.onNode(hasSetTextAction()).assertTextEquals("Draft")
        compose.runOnIdle { enabled.value = true }
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(listOf("Draft"), sent) }
    }
}
