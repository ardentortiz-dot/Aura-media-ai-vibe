package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaItemEntity::class,
        PlaylistEntity::class,
        PlaylistMediaCrossRef::class,
        ArtistCorrectionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
