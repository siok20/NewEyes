package com.neweyes.chat.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.neweyes.data.entity.ChatEntity
import com.neweyes.data.entity.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatHistoryViewModel(private val repository: ChatRepository) : ViewModel() {

    val chatSummaries: StateFlow<List<Chat>> = repository.getAllChats()
        .map { list -> list.map { Chat(it.id, it.title, it.createdAt) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    //val allChats = chatSummaries.asLiveData()

    suspend fun createNewChat(chat: ChatEntity): Long {
        return withContext(Dispatchers.IO) {
            repository.insertChat(chat)
        }
    }

    fun saveMessage(message: MessageEntity) {
        viewModelScope.launch {
            repository.insertMessage(message)
        }
    }

    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>> {
        return repository.getMessagesForChat(chatId)
    }

    fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            repository.deleteChatAndMessages(chatId)
        }
    }


}
