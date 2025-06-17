import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,  // Para soportar múltiples chats
    val timestamp: Long,
    val text: String?,
    val imageUri: String?,
    val isUser: Boolean
)
