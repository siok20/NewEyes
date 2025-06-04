package com.neweyes.chat

data class Message(
    val text: String,
    val isUser: Boolean = false
)