package com.juanpvivas.aichatjp.data.remote.impl

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.exception.*
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.juanpvivas.aichatjp.core.AppLogger
import com.juanpvivas.aichatjp.data.model.ChatMessage
import com.juanpvivas.aichatjp.data.remote.config.GroqModelResolver
import com.juanpvivas.aichatjp.data.remote.mapper.ChatMessageMapper
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChatRemoteDataSourceImplTest {
    private lateinit var client: OpenAI
    private lateinit var remote: ChatRemoteDataSourceImpl
    private val history = listOf(
        ChatMessage(1, 7, "Question", true, 0),
        ChatMessage(2, 7, "Previous answer", false, 1)
    )
    private fun models(vararg ids: String) = ids.map { Model(ModelId(it)) }
    private fun error(code: String) = InvalidRequestException(404, OpenAIError(OpenAIErrorDetails(code, "Unavailable")))

    @Before fun setup() {
        mockkObject(AppLogger)
        every { AppLogger.i(any(), any()) } just Runs
        every { AppLogger.d(any(), any()) } just Runs
        client = mockk()
        remote = ChatRemoteDataSourceImpl(client, ChatMessageMapper(), GroqModelResolver(client))
        coEvery { client.models() } returns models("qwen/current")
    }
    @After fun cleanup() { unmockkAll() }

    private fun completion(): ChatCompletion = mockk {
        every { model } returns ModelId("qwen/current")
        every { choices } returns listOf(mockk { every { message } returns com.aallam.openai.api.chat.ChatMessage(role = ChatRole.Assistant, content = "Answer") })
        every { usage } returns null
    }

    @Test fun `request uses discovered model and correct historical roles`() = runTest {
        val request = slot<ChatCompletionRequest>()
        coEvery { client.chatCompletion(capture(request)) } returns completion()
        assertEquals("Answer", remote.sendMessage(history).content)
        assertEquals("qwen/current", request.captured.model.id)
        assertEquals(listOf(ChatRole.User, ChatRole.Assistant), request.captured.messages.map { it.role })
    }

    @Test fun `model not found refreshes catalog and retries once with another model`() = runTest {
        coEvery { client.models() } returnsMany listOf(models("qwen/old"), models("qwen/new"))
        val requests = mutableListOf<ChatCompletionRequest>()
        coEvery { client.chatCompletion(capture(requests)) } throws error("model_not_found") andThen completion()
        assertEquals("Answer", remote.sendMessage(history).content)
        assertEquals(listOf("qwen/old", "qwen/new"), requests.map { it.model.id })
        assertEquals(requests[0].messages, requests[1].messages)
        coVerify(exactly = 2) { client.models() }
    }

    @Test fun `second rejection is propagated without retry loop`() = runTest {
        coEvery { client.models() } returnsMany listOf(models("qwen/old"), models("qwen/new"))
        coEvery { client.chatCompletion(any()) } throws error("model_not_found")
        try { remote.sendMessage(history); fail("Expected unavailable model") }
        catch (_: InvalidRequestException) { }
        coVerify(exactly = 2) { client.chatCompletion(any()) }
        coVerify(exactly = 2) { client.models() }
    }

    @Test fun `refresh never retries a rejected model when it is the only one listed`() = runTest {
        coEvery { client.chatCompletion(any()) } throws error("model_not_found")
        try { remote.sendMessage(history); fail("Expected unavailable model") }
        catch (_: IllegalStateException) { }
        coVerify(exactly = 1) { client.chatCompletion(any()) }
    }

    @Test fun `authentication errors do not switch models`() = runTest {
        val failure = AuthenticationException(401, OpenAIError(OpenAIErrorDetails("invalid_api_key", "Invalid key")))
        coEvery { client.chatCompletion(any()) } throws failure
        try { remote.sendMessage(history); fail("Expected auth error") }
        catch (e: AuthenticationException) { assertSame(failure, e) }
        coVerify(exactly = 1) { client.models() }
        coVerify(exactly = 1) { client.chatCompletion(any()) }
    }
}
