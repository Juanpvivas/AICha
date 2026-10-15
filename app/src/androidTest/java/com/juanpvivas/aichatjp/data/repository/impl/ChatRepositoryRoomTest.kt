package com.juanpvivas.aichatjp.data.repository.impl

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.juanpvivas.aichatjp.data.local.database.AppDatabase
import com.juanpvivas.aichatjp.data.local.entity.ConversationEntity
import com.juanpvivas.aichatjp.data.local.entity.MessageEntity
import com.juanpvivas.aichatjp.data.model.ChatMessage
import com.juanpvivas.aichatjp.data.remote.ChatRemoteDataSource
import com.juanpvivas.aichatjp.data.remote.dto.SendMessageResponse
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChatRepositoryRoomTest {
    private lateinit var db: AppDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java
        ).build()
    }
    @After fun cleanup() { db.close() }

    @Test fun reopenedConversationUsesPersistedContextAndKeepsRepliesInSameConversation() = runBlocking {
        val id = db.conversationDao().insertConversation(ConversationEntity(title = "Saved"))
        db.messageDao().insertMessage(MessageEntity(conversationId = id, content = "Question", isFromUser = true, timestamp = 1))
        db.messageDao().insertMessage(MessageEntity(conversationId = id, content = "Answer", isFromUser = false, timestamp = 1))
        var context = emptyList<ChatMessage>()
        val remote = object : ChatRemoteDataSource {
            override suspend fun sendMessage(messages: List<ChatMessage>): SendMessageResponse {
                context = messages
                return SendMessageResponse("Continued answer", "available", null)
            }
        }
        val repository = ChatRepositoryImpl(remote, db.conversationDao(), db.messageDao())
        repository.sendMessage("Continue", id)
        assertEquals(listOf("Question", "Answer", "Continue"), context.map { it.content })
        assertEquals(listOf(true, false, true), context.map { it.isFromUser })
        val saved = repository.getMessagesSync(id)
        assertEquals(4, saved.size)
        assertTrue(saved.all { it.conversationId == id })
        assertEquals("Continued answer", saved.last().content)
    }

    @Test fun equalTimestampsAreOrderedByMessageId() = runBlocking {
        val id = db.conversationDao().insertConversation(ConversationEntity(title = "Saved"))
        db.messageDao().insertMessage(MessageEntity(20, id, "Later", false, 1))
        db.messageDao().insertMessage(MessageEntity(10, id, "Earlier", true, 1))
        assertEquals(listOf(10L, 20L), db.messageDao().getMessagesByConversationSync(id).map { it.id })
    }
}
