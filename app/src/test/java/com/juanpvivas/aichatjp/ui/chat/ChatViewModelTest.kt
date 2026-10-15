package com.juanpvivas.aichatjp.ui.chat

import android.util.Log
import com.juanpvivas.aichatjp.data.model.ChatMessage as SavedMessage
import com.juanpvivas.aichatjp.data.repository.ChatRepository
import com.juanpvivas.aichatjp.testutil.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var repository: ChatRepository
    private lateinit var viewModel: ChatViewModel

    @Before fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        repository = mockk()
        coEvery { repository.createConversation(any()) } returns 42L
        coEvery { repository.sendMessage(any(), any()) } returns "Answer"
        coEvery { repository.getMessagesSync(7) } returns listOf(
            SavedMessage(1, 7, "Old question", true, 0),
            SavedMessage(2, 7, "Old answer", false, 1)
        )
        viewModel = ChatViewModel(repository, main.dispatcher)
    }
    @After fun cleanup() { unmockkAll() }

    @Test fun `blank input leaves the empty state unchanged`() = runTest {
        viewModel.sendMessage("  ")
        advanceUntilIdle()
        assertEquals(ChatUiState.Empty, viewModel.uiState.value)
        coVerify(exactly = 0) { repository.createConversation(any()) }
    }

    @Test fun `send emits loading then answer and creates only one conversation`() = runTest {
        viewModel.sendMessage("Hello")
        assertTrue((viewModel.uiState.value as ChatUiState.Success).isLoading)
        advanceUntilIdle()
        assertEquals(listOf("Hello", "Answer"), messages())
        viewModel.sendMessage("Again")
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.createConversation("Hello") }
        coVerify { repository.sendMessage("Again", 42) }
        assertEquals(listOf("Hello", "Answer", "Again", "Answer"), messages())
    }

    @Test fun `reopened conversation is continued with its saved id`() = runTest {
        viewModel.loadConversation(7)
        advanceUntilIdle()
        viewModel.sendMessage("Continue")
        advanceUntilIdle()
        coVerify { repository.sendMessage("Continue", 7) }
        coVerify(exactly = 0) { repository.createConversation(any()) }
        assertEquals(listOf("Old question", "Old answer", "Continue", "Answer"), messages())
    }

    @Test fun `error retains previous messages and next send uses same conversation`() = runTest {
        viewModel.loadConversation(7)
        advanceUntilIdle()
        coEvery { repository.sendMessage("Fail", 7) } throws IllegalStateException("Offline")
        viewModel.sendMessage("Fail")
        advanceUntilIdle()
        val error = viewModel.uiState.value as ChatUiState.Error
        assertEquals("Offline", error.message)
        assertEquals(listOf("Old question", "Old answer", "Fail"), error.messages.map { it.text })
        viewModel.sendMessage("Retry")
        advanceUntilIdle()
        assertEquals(listOf("Old question", "Old answer", "Fail", "Retry", "Answer"), messages())
        coVerify { repository.sendMessage("Retry", 7) }
    }

    @Test fun `first request failure keeps created conversation for next send`() = runTest {
        coEvery { repository.sendMessage("Fail", 42) } throws IllegalStateException("Offline")
        viewModel.sendMessage("Fail")
        advanceUntilIdle()
        viewModel.sendMessage("Retry")
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.createConversation(any()) }
        coVerify { repository.sendMessage("Retry", 42) }
        assertEquals(listOf("Fail", "Retry", "Answer"), messages())
    }

    @Test fun `repeated send is ignored while response is pending`() = runTest {
        val response = CompletableDeferred<String>()
        coEvery { repository.sendMessage(any(), any()) } coAnswers { response.await() }
        viewModel.sendMessage("First")
        runCurrent()
        viewModel.sendMessage("Second")
        runCurrent()
        coVerify(exactly = 1) { repository.sendMessage(any(), any()) }
        response.complete("Done")
        advanceUntilIdle()
        assertEquals(listOf("First", "Done"), messages())
    }

    @Test fun `new conversation ignores a late response even from non cancellable work`() = runTest {
        val response = CompletableDeferred<String>()
        coEvery { repository.sendMessage(any(), any()) } coAnswers {
            withContext(NonCancellable) { response.await() }
        }
        viewModel.sendMessage("First")
        runCurrent()
        viewModel.clearHistory()
        response.complete("Obsolete")
        advanceUntilIdle()
        assertEquals(ChatUiState.Empty, viewModel.uiState.value)
        coEvery { repository.sendMessage(any(), any()) } returns "New answer"
        viewModel.sendMessage("New")
        advanceUntilIdle()
        assertEquals(listOf("New", "New answer"), messages())
        coVerify(exactly = 2) { repository.createConversation(any()) }
    }

    @Test fun `selecting another conversation cancels pending response`() = runTest {
        val response = CompletableDeferred<String>()
        coEvery { repository.sendMessage(any(), any()) } coAnswers { response.await() }
        viewModel.sendMessage("Pending")
        runCurrent()
        viewModel.loadConversation(7)
        advanceUntilIdle()
        response.complete("Obsolete")
        advanceUntilIdle()
        assertEquals(listOf("Old question", "Old answer"), messages())
    }

    @Test fun `late conversation load cannot replace more recent selection`() = runTest {
        val load = CompletableDeferred<List<SavedMessage>>()
        coEvery { repository.getMessagesSync(8) } coAnswers { withContext(NonCancellable) { load.await() } }
        viewModel.loadConversation(8)
        runCurrent()
        viewModel.loadConversation(7)
        runCurrent()
        load.complete(listOf(SavedMessage(3, 8, "Obsolete", true, 0)))
        advanceUntilIdle()
        assertEquals(listOf("Old question", "Old answer"), messages())
    }

    @Test fun `deleting active conversation cancels request and clears screen`() = runTest {
        val response = CompletableDeferred<String>()
        coEvery { repository.sendMessage(any(), any()) } coAnswers { response.await() }
        viewModel.loadConversation(7)
        advanceUntilIdle()
        viewModel.sendMessage("Pending")
        runCurrent()
        viewModel.onConversationDeleted(7)
        response.complete("Obsolete")
        advanceUntilIdle()
        assertEquals(ChatUiState.Empty, viewModel.uiState.value)
    }

    @Test fun `deleting another conversation leaves the current chat visible`() = runTest {
        viewModel.loadConversation(7)
        advanceUntilIdle()
        viewModel.onConversationDeleted(8)
        assertEquals(listOf("Old question", "Old answer"), messages())
    }

    @Test fun `load failure ends loading and reports error`() = runTest {
        coEvery { repository.getMessagesSync(7) } throws IllegalStateException("Read failed")
        viewModel.loadConversation(7)
        advanceUntilIdle()
        assertEquals("Read failed", (viewModel.uiState.value as ChatUiState.Error).message)
    }

    private fun messages() = (viewModel.uiState.value as ChatUiState.Success).messages.map { it.text }
}
