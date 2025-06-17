package com.neweyes.chat.history

import com.neweyes.data.dao.ChatDao
import com.neweyes.data.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatHistoryDao: ChatDao
) {
    suspend fun insertChat(title: String): Long {
        val entity = ChatEntity(title = title, createdAt = System.currentTimeMillis())
        return chatHistoryDao.insert(entity)
    }

    fun getAllChats(): Flow<List<ChatEntity>> = chatHistoryDao.getAllChats()
}