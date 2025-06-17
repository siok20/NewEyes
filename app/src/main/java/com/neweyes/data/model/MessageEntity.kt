package com.neweyes.data.model

import androidx.room.*

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chatId"])] // Mejora de rendimiento para relaciones
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,  // Foreign Key
    val timestamp: Long,
    val text: String?,
    val imageUri: String?,
    val isUser: Boolean
)

