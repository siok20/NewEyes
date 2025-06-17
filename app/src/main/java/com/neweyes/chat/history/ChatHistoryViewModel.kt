package com.neweyes.chat.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatHistoryViewModel(private val repository: ChatRepository) : ViewModel() {

    val chatSummaries: StateFlow<List<Chat>> = repository.getAllChats()
        .map { list -> list.map { Chat(it.id, it.title, it.createdAt) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createNewChat(title: String): Long {
        // Deberías mover esto a una función suspend o usar coroutine
        var id = -1L
        viewModelScope.launch {
            id = repository.insertChat(title)
        }
        return id
    }
}
