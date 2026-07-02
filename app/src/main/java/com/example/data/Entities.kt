package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val normalizedArtist: String,
    val album: String,
    val duration: Long, // in seconds
    val mediaType: String, // "AUDIO" or "VIDEO"
    val url: String, // source path or mock source
    val localPath: String? = null, // downloaded path if offline
    val isOffline: Boolean = false,
    val isFavorite: Boolean = false,
    val coverUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_media_cross_ref", primaryKeys = ["playlistId", "mediaItemId"])
data class PlaylistMediaCrossRef(
    val playlistId: Int,
    val mediaItemId: Int
)

@Entity(tableName = "artist_corrections")
data class ArtistCorrectionEntity(
    @PrimaryKey val originalName: String,
    val correctedName: String
)
