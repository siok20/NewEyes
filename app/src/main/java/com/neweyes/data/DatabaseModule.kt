package com.neweyes.data

import android.content.Context
import androidx.room.Room

object DatabaseModule {
    fun provideDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "neweyes_db" // Nombre del archivo .db en almacenamiento interno
        ).build()
    }
}