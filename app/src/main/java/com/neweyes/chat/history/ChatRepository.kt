package com.neweyes.chat.history

import com.neweyes.data.dao.ChatDao
import com.neweyes.data.dao.MessageDao
import com.neweyes.data.entity.ChatEntity
import com.neweyes.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    suspend fun insertChat(chat: ChatEntity): Long = chatDao.insert(chat)
    fun getAllChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()

    suspend fun insertMessage(message: MessageEntity) = messageDao.insert(message)
    fun getMessagesForChat(chatId: Long) = messageDao.getMessagesForChat(chatId)

    suspend fun deleteChatAndMessages(chatId: Long) {
        messageDao.deleteMessagesForChat(chatId)
        chatDao.deleteChatById(chatId)
    }

}
