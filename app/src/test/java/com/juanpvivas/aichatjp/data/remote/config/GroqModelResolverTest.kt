package com.juanpvivas.aichatjp.data.remote.config

import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GroqModelResolverTest {
    private val client = mockk<OpenAI>()
    private val resolver = GroqModelResolver(client)
    private fun models(vararg ids: String) = ids.map { Model(ModelId(it)) }

    @Test fun `selects available chat model while excluding audio guards embeddings and unknown models`() = runTest {
        coEvery { client.models() } returns models(
            "whisper-large-v3", "canopylabs/orpheus-v1-english", "meta-llama/llama-guard-4",
            "openai/gpt-oss-safeguard-20b", "qwen/embedding", "unclassified-model", "qwen/qwen3.8-27b"
        )
        assertEquals("qwen/qwen3.8-27b", resolver.resolveChatModel())
    }

    @Test fun `catalog is cached until refresh and rejected model is excluded from subsequent selections`() = runTest {
        coEvery { client.models() } returnsMany listOf(
            models("qwen/old"), models("qwen/old", "openai/gpt-oss-20b")
        )
        assertEquals("qwen/old", resolver.resolveChatModel())
        assertEquals("qwen/old", resolver.resolveChatModel())
        coVerify(exactly = 1) { client.models() }
        assertEquals("openai/gpt-oss-20b", resolver.resolveChatModel(true, setOf("qwen/old")))
        assertEquals("openai/gpt-oss-20b", resolver.resolveChatModel())
        coVerify(exactly = 2) { client.models() }
    }

    @Test fun `empty catalog fails without inventing fallback and is not cached`() = runTest {
        coEvery { client.models() } returnsMany listOf(emptyList(), models("allam-2-7b"))
        try { resolver.resolveChatModel(); fail("Expected unavailable models") }
        catch (_: IllegalStateException) { }
        assertEquals("allam-2-7b", resolver.resolveChatModel())
    }

    @Test fun `catalog with only audio models cannot be used for chat`() = runTest {
        coEvery { client.models() } returns models("whisper-large-v3", "canopylabs/orpheus-arabic-saudi")
        try { resolver.resolveChatModel(); fail("Expected unavailable models") }
        catch (_: IllegalStateException) { }
    }

    @Test fun `discovery errors are propagated rather than using an unverified fallback`() = runTest {
        coEvery { client.models() } throws IllegalStateException("Offline")
        try { resolver.resolveChatModel(); fail("Expected discovery error") }
        catch (e: IllegalStateException) { assertEquals("Offline", e.message) }
    }

    @Test fun `discovery cancellation propagates`() = runTest {
        coEvery { client.models() } throws CancellationException("Cancelled")
        try { resolver.resolveChatModel(); fail("Expected cancellation") }
        catch (_: CancellationException) { }
    }
}
