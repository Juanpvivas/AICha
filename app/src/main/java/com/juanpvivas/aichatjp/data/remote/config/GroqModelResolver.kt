package com.juanpvivas.aichatjp.data.remote.config

import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqModelResolver @Inject constructor(private val openAI: OpenAI) {
    private val mutex = Mutex()
    private var cachedModels: List<String>? = null

    suspend fun resolveChatModel(refresh: Boolean = false, excluded: Set<String> = emptySet()): String =
        mutex.withLock {
            if (refresh || cachedModels == null) {
                val models = openAI.models().map { it.id.id }.filter(::isChatModel)
                    .sortedWith(compareBy<String> { preference(it) }.thenBy { it })
                // Do not retain an empty result: a subsequent send should try discovery again.
                cachedModels = models.filterNot { it in excluded }.takeIf { it.isNotEmpty() }
            }
            cachedModels?.firstOrNull { it !in excluded }
                ?: throw IllegalStateException("No compatible chat model is available for this account")
        }

    private fun preference(id: String): Int = when {
        id.startsWith("qwen/") -> 0
        id.startsWith("openai/gpt-oss-") -> 1
        else -> 2
    }

    private fun isChatModel(id: String): Boolean {
        val name = id.lowercase()
        if (listOf("guard", "whisper", "tts", "orpheus", "dall-e", "embed").any { it in name }) return false
        return listOf("qwen/", "openai/gpt-oss-", "llama", "meta-llama/llama-", "gemma", "mixtral", "mistral", "allam-")
            .any { name.startsWith(it) }
    }
}
