package com.neweyes.chat

data class Message(
    val text: String? = null,
    val imageUri: String? = null,
    val isUser: Boolean,
    val userName: String? = null
)
