package com.vibi.shared.data.local.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(): RoomDatabase.Builder<VibiDatabase> {
    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val dbPath = requireNotNull(documentDirectory?.path) {
        "Could not resolve iOS document directory for Vibi database."
    } + "/" + VibiDatabase.DB_FILE_NAME
    return Room.databaseBuilder<VibiDatabase>(name = dbPath)
}
