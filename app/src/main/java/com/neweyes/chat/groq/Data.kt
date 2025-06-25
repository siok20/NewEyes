package com.neweyes.chat.groq

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>
)

data class ChatResponse(
    val id: String,
    val objectType: String?, // dependiendo del API
    val choices: List<Choice>?
)

data class Choice(
    val index: Int,
    val message: ChatMessage?,
    val finish_reason: String?
)