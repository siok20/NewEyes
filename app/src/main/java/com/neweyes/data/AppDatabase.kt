package com.neweyes.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.neweyes.data.model.*
import com.neweyes.data.dao.*

@Database(entities = [ChatEntity::class, MessageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
