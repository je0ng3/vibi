package com.vibi.shared.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

lateinit var applicationContext: Context
    internal set

actual fun getDatabaseBuilder(): RoomDatabase.Builder<VibiDatabase> {
    check(::applicationContext.isInitialized) {
        "Vibi DB: call VibiDatabaseInitializer.init(context) before building the database."
    }
    val dbFile = applicationContext.getDatabasePath(VibiDatabase.DB_FILE_NAME)
    return Room.databaseBuilder<VibiDatabase>(
        context = applicationContext,
        name = dbFile.absolutePath
    )
}

object VibiDatabaseInitializer {
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
