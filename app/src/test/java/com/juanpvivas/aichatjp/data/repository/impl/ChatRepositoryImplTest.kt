package com.juanpvivas.aichatjp.data.repository.impl

import com.juanpvivas.aichatjp.data.local.dao.ConversationDao
import com.juanpvivas.aichatjp.data.local.dao.MessageDao
import com.juanpvivas.aichatjp.data.local.entity.ConversationEntity
import com.juanpvivas.aichatjp.data.local.entity.MessageEntity
import com.juanpvivas.aichatjp.data.model.ChatMessage
import com.juanpvivas.aichatjp.data.remote.ChatRemoteDataSource
import com.juanpvivas.aichatjp.data.remote.dto.SendMessageResponse
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryImplTest {
    private lateinit var remote: ChatRemoteDataSource
    private lateinit var conversations: ConversationDao
    private lateinit var messages: MessageDao
    private lateinit var repository: ChatRepositoryImpl
    private val stored = mutableListOf<MessageEntity>()

    @Before fun setup() {
        stored.clear()
        remote = mockk()
        conversations = mockk(relaxed = true)
        messages = mockk()
        coEvery { conversations.getConversationById(any()) } answers { ConversationEntity(firstArg(), "Saved") }
        coEvery { messages.insertMessage(any()) } answers {
            stored.add(firstArg<MessageEntity>().copy(id = stored.size.toLong() + 1))
            stored.last().id
        }
        coEvery { messages.getMessagesByConversationSync(any()) } answers {
            val id = firstArg<Long>()
            stored.filter { it.conversationId == id }.toList()
        }
        coEvery { remote.sendMessage(any()) } returns SendMessageResponse("Reply", "available", null)
        repository = ChatRepositoryImpl(remote, conversations, messages)
    }

    @Test fun `saved conversation supplies full ordered history and preserves roles`() = runTest {
        stored.add(MessageEntity(1, 7, "Old question", true, 0))
        stored.add(MessageEntity(2, 7, "Old answer", false, 1))
        val request = slot<List<ChatMessage>>()
        coEvery { remote.sendMessage(capture(request)) } returns SendMessageResponse("Reply", "available", null)
        assertEquals("Reply", repository.sendMessage("Continue", 7))
        assertEquals(listOf("Old question", "Old answer", "Continue"), request.captured.map { it.content })
        assertEquals(listOf(true, false, true), request.captured.map { it.isFromUser })
        assertTrue(stored.all { it.conversationId == 7L })
        assertFalse(stored.last().isFromUser)
        coVerify(exactly = 0) { conversations.insertConversation(any()) }
    }

    @Test fun `failed response retains user message and next send uses persisted history`() = runTest {
        coEvery { remote.sendMessage(any()) } throws IllegalStateException("Offline")
        try { repository.sendMessage("First", 7); fail("Expected network failure") }
        catch (e: IllegalStateException) { assertEquals("Offline", e.message) }
        assertEquals(listOf("First"), stored.map { it.content })
        val request = slot<List<ChatMessage>>()
        coEvery { remote.sendMessage(capture(request)) } returns SendMessageResponse("Reply", "available", null)
        repository.sendMessage("Second", 7)
        assertEquals(listOf("First", "Second"), request.captured.map { it.content })
    }

    @Test fun `concurrent requests remain attached to their explicit conversation ids`() = runTest {
        val first = CompletableDeferred<SendMessageResponse>()
        coEvery { remote.sendMessage(match { it.last().content == "First" }) } coAnswers { first.await() }
        val job = launch { repository.sendMessage("First", 7) }
        runCurrent()
        repository.sendMessage("Second", 8)
        first.complete(SendMessageResponse("First reply", "available", null))
        job.join()
        assertEquals(7L, stored.first { it.content == "First reply" }.conversationId)
        assertEquals(8L, stored.first { it.content == "Reply" }.conversationId)
    }

    @Test fun `cancelled request does not persist an assistant reply`() = runTest {
        val response = CompletableDeferred<SendMessageResponse>()
        coEvery { remote.sendMessage(any()) } coAnswers { withContext(NonCancellable) { response.await() } }
        val job = launch { repository.sendMessage("Pending", 7) }
        runCurrent()
        job.cancel()
        response.complete(SendMessageResponse("Obsolete", "available", null))
        job.join()
        assertEquals(listOf("Pending"), stored.map { it.content })
    }

    @Test fun `deleted conversation cannot send another message`() = runTest {
        coEvery { conversations.getConversationById(7) } returns null
        try { repository.sendMessage("Question", 7); fail("Expected missing conversation") }
        catch (_: IllegalArgumentException) { }
        coVerify(exactly = 0) { messages.insertMessage(any()) }
        coVerify(exactly = 0) { remote.sendMessage(any()) }
    }
}
