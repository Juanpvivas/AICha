package com.juanpvivas.aichatjp.data.repository.impl

import com.juanpvivas.aichatjp.data.local.dao.ConversationDao
import com.juanpvivas.aichatjp.data.local.dao.MessageDao
import com.juanpvivas.aichatjp.data.local.entity.ConversationEntity
import com.juanpvivas.aichatjp.data.local.entity.MessageEntity
import com.juanpvivas.aichatjp.data.local.mapper.toDomain
import com.juanpvivas.aichatjp.data.model.ChatMessage
import com.juanpvivas.aichatjp.data.model.Conversation
import com.juanpvivas.aichatjp.data.remote.ChatRemoteDataSource
import com.juanpvivas.aichatjp.data.repository.ChatRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatRemoteDataSource: ChatRemoteDataSource,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) : ChatRepository {

    override suspend fun sendMessage(userMessage: String, conversationId: Long): String {
        requireNotNull(conversationDao.getConversationById(conversationId)) {
            "Conversation no longer exists"
        }
        messageDao.insertMessage(
            MessageEntity(conversationId = conversationId, content = userMessage, isFromUser = true)
        )
        val history = messageDao.getMessagesByConversationSync(conversationId).map { it.toDomain() }
        val response = chatRemoteDataSource.sendMessage(history)
        currentCoroutineContext().ensureActive()
        messageDao.insertMessage(
            MessageEntity(conversationId = conversationId, content = response.content, isFromUser = false)
        )
        conversationDao.getConversationById(conversationId)?.let { conversation ->
            conversationDao.updateConversation(conversation.copy(updatedAt = System.currentTimeMillis()))
        }
        return response.content
    }

    override fun getConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations().map { entities -> entities.map { it.toDomain() } }

    override fun getMessages(conversationId: Long): Flow<List<ChatMessage>> =
        messageDao.getMessagesByConversation(conversationId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getMessagesSync(conversationId: Long): List<ChatMessage> =
        messageDao.getMessagesByConversationSync(conversationId).map { it.toDomain() }

    override suspend fun createConversation(title: String): Long =
        conversationDao.insertConversation(ConversationEntity(title = title))

    override suspend fun deleteConversation(conversationId: Long) {
        conversationDao.deleteConversation(conversationId)
    }
}
