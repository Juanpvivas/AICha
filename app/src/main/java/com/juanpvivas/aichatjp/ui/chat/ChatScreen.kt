package com.juanpvivas.aichatjp.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juanpvivas.aichatjp.ui.chat.components.ChatContent
import com.juanpvivas.aichatjp.ui.chat.components.ChatInputBar
import com.juanpvivas.aichatjp.ui.chat.components.ChatTitle
import com.juanpvivas.aichatjp.ui.theme.AiChatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()
) {
    fun sendMessage(text: String) {
        viewModel.sendMessage(text)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { ChatTitle() },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Surface(color = MaterialTheme.colorScheme.surface) {
                    ChatInputBar(
                        onSend = ::sendMessage,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                    )
                }
            }
        }
    ) { innerPadding ->
        ChatContent(
            messages = viewModel.messages,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    AiChatTheme {
        ChatScreen()
    }
}
