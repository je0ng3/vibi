package com.vibi.shared.data.local.db

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<VibiDatabase>
