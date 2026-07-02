package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY addedAt DESC")
    fun getAllMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE mediaType = :mediaType ORDER BY addedAt DESC")
    fun getMediaItemsByType(mediaType: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaItemById(id: Int): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaItemEntity>)

    @Update
    suspend fun updateMediaItem(item: MediaItemEntity)

    @Delete
    suspend fun deleteMediaItem(item: MediaItemEntity)

    @Query("UPDATE media_items SET normalizedArtist = :normalizedArtist WHERE artist = :originalArtist")
    suspend fun updateNormalizedArtist(originalArtist: String, normalizedArtist: String)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Int): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("""
        SELECT * FROM media_items 
        INNER JOIN playlist_media_cross_ref ON media_items.id = playlist_media_cross_ref.mediaItemId 
        WHERE playlist_media_cross_ref.playlistId = :playlistId
    """)
    fun getMediaItemsForPlaylist(playlistId: Int): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistCrossRef(crossRef: PlaylistMediaCrossRef)

    @Query("DELETE FROM playlist_media_cross_ref WHERE playlistId = :playlistId AND mediaItemId = :mediaItemId")
    suspend fun deletePlaylistCrossRef(playlistId: Int, mediaItemId: Int)

    @Query("DELETE FROM playlist_media_cross_ref WHERE playlistId = :playlistId")
    suspend fun deleteCrossRefsForPlaylist(playlistId: Int)

    @Query("SELECT * FROM artist_corrections")
    fun getAllArtistCorrectionsFlow(): Flow<List<ArtistCorrectionEntity>>

    @Query("SELECT * FROM artist_corrections")
    suspend fun getAllArtistCorrections(): List<ArtistCorrectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtistCorrection(correction: ArtistCorrectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtistCorrections(corrections: List<ArtistCorrectionEntity>)

    @Query("DELETE FROM artist_corrections")
    suspend fun clearAllArtistCorrections()
}
