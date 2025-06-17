package com.neweyes.data

import com.neweyes.chat.Message
import com.neweyes.data.model.MessageEntity

fun MessageEntity.toMessage(): Message {
    return Message(text = text, imageUri = imageUri, isUser = isUser)
}

fun Message.toEntity(chatId: Long): MessageEntity {
    return MessageEntity(
        chatId = chatId,
        timestamp = System.currentTimeMillis(),
        text = text,
        imageUri = imageUri,
        isUser = isUser
    )
}
