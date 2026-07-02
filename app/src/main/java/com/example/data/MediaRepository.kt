package com.example.data

import android.content.Context
import androidx.room.Room
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MediaRepository(private val context: Context, private val db: AppDatabase) {

    private val mediaDao = db.mediaDao()

    val allMediaItems: Flow<List<MediaItemEntity>> = mediaDao.getAllMediaItems()
    val allPlaylists: Flow<List<PlaylistEntity>> = mediaDao.getAllPlaylists()
    val artistCorrectionsFlow: Flow<List<ArtistCorrectionEntity>> = mediaDao.getAllArtistCorrectionsFlow()

    fun getMediaItemsByType(mediaType: String): Flow<List<MediaItemEntity>> {
        return mediaDao.getMediaItemsByType(mediaType)
    }

    fun getMediaItemsForPlaylist(playlistId: Int): Flow<List<MediaItemEntity>> {
        return mediaDao.getMediaItemsForPlaylist(playlistId)
    }

    suspend fun getMediaItemById(id: Int): MediaItemEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaItemById(id)
    }

    suspend fun insertPlaylist(name: String, description: String = ""): Int = withContext(Dispatchers.IO) {
        val playlist = PlaylistEntity(name = name, description = description)
        mediaDao.insertPlaylist(playlist).toInt()
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        mediaDao.deleteCrossRefsForPlaylist(playlist.id)
        mediaDao.deletePlaylist(playlist)
    }

    suspend fun addMediaToPlaylist(playlistId: Int, mediaItemId: Int) = withContext(Dispatchers.IO) {
        mediaDao.insertPlaylistCrossRef(PlaylistMediaCrossRef(playlistId, mediaItemId))
    }

    suspend fun removeMediaFromPlaylist(playlistId: Int, mediaItemId: Int) = withContext(Dispatchers.IO) {
        mediaDao.deletePlaylistCrossRef(playlistId, mediaItemId)
    }

    suspend fun insertMediaItem(title: String, artist: String, album: String, duration: Long, mediaType: String, url: String, coverUrl: String? = null) = withContext(Dispatchers.IO) {
        val normalized = getNormalizedName(artist)
        val item = MediaItemEntity(
            title = title,
            artist = artist,
            normalizedArtist = normalized,
            album = album,
            duration = duration,
            mediaType = mediaType,
            url = url,
            coverUrl = coverUrl
        )
        mediaDao.insertMediaItem(item)
    }

    suspend fun deleteMediaItem(item: MediaItemEntity) = withContext(Dispatchers.IO) {
        mediaDao.deleteMediaItem(item)
    }

    suspend fun toggleFavorite(itemId: Int) = withContext(Dispatchers.IO) {
        val item = mediaDao.getMediaItemById(itemId)
        if (item != null) {
            mediaDao.insertMediaItem(item.copy(isFavorite = !item.isFavorite))
        }
    }

    // Smart local offline normalization heuristics
    private suspend fun getNormalizedName(artistName: String): String {
        val trimmed = artistName.trim()
        val corrections = mediaDao.getAllArtistCorrections()
        
        // 1. Check database-defined corrections first
        val manualCorrection = corrections.firstOrNull { it.originalName.equals(trimmed, ignoreCase = true) }
        if (manualCorrection != null) {
            return manualCorrection.correctedName
        }

        // 2. Perform case-insensitive search for close matches
        // E.g., if "Taylor Swift" already exists, "Taylor swift" should normalize to "Taylor Swift"
        val existingItems = mediaDao.getAllMediaItems().first()
        val matchingArtist = existingItems.firstOrNull { 
            it.artist.trim().equals(trimmed, ignoreCase = true) 
        }
        
        if (matchingArtist != null) {
            // Find the most standard capitalized version (usually the one that has capitals or is longer)
            val existingArtist = matchingArtist.artist.trim()
            if (existingArtist != trimmed) {
                return existingArtist
            }
        }

        return trimmed
    }

    /**
     * Seed initial content if library is empty
     */
    suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        val items = mediaDao.getAllMediaItems().first()
        if (items.isNotEmpty()) return@withContext

        val initialItems = listOf(
            // Music with intentional metadata spelling errors to showcase the correction tool
            MediaItemEntity(
                title = "Blinding Lights",
                artist = "The Weeknd",
                normalizedArtist = "The Weeknd",
                album = "After Hours",
                duration = 200,
                mediaType = "AUDIO",
                url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                coverUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=300&auto=format&fit=crop"
            ),
            MediaItemEntity(
                title = "Save Your Tears",
                artist = "the weeknd", // error case
                normalizedArtist = "The Weeknd",
                album = "After Hours",
                duration = 215,
                mediaType = "AUDIO",
                url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                coverUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=300&auto=format&fit=crop"
            ),
            MediaItemEntity(
                title = "Anti-Hero",
                artist = "Taylor Swift",
                normalizedArtist = "Taylor Swift",
                album = "Midnights",
                duration = 201,
                mediaType = "AUDIO",
                url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&auto=format&fit=crop"
            ),
            MediaItemEntity(
                title = "Cruel Summer",
                artist = "Taylor swift", // error case
                normalizedArtist = "Taylor Swift",
                album = "Lover",
                duration = 178,
                mediaType = "AUDIO",
                url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&auto=format&fit=crop"
            ),
            MediaItemEntity(
                title = "Shake It Off",
                artist = "Taylor Swift ", // error case (trailing space)
                normalizedArtist = "Taylor Swift",
                album = "1989",
                duration = 219,
                mediaType = "AUDIO",
                url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&auto=format&fit=crop"
            ),
            MediaItemEntity(
                title = "Bad Blood",
                artist = "T. Swift", // error case / alias
                normalizedArtist = "Taylor Swift",
                album = "1989",
                duration = 211,
                mediaType = "AUDIO",
                url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&auto=format&fit=crop"
            ),
            MediaItemEntity(
                title = "As It Was",
                artist = "Harry Styles",
                normalizedArtist = "Harry Styles",
                album = "Harry's House",
                duration = 167,
                mediaType = "AUDIO",
                url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300&auto=format&fit=crop"
            ),
            MediaItemEntity(
                title = "Watermelon Sugar",
                artist = "Harry styles", // error case
                normalizedArtist = "Harry Styles",
                album = "Fine Line",
                duration = 174,
                mediaType = "AUDIO",
                url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300&auto=format&fit=crop"
            ),

            // Videos
            MediaItemEntity(
                title = "Forest stream in sunlight",
                artist = "Visuals Studio",
                normalizedArtist = "Visuals Studio",
                album = "Nature",
                duration = 15,
                mediaType = "VIDEO",
                url = "https://assets.mixkit.co/videos/preview/mixkit-forest-stream-in-the-sunlight-529-large.mp4",
                coverUrl = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=300&auto=format&fit=crop"
            ),
            MediaItemEntity(
                title = "Cosmic Stars Symphony",
                artist = "Cosmic Journey",
                normalizedArtist = "Cosmic Journey",
                album = "Space",
                duration = 30,
                mediaType = "VIDEO",
                url = "https://assets.mixkit.co/videos/preview/mixkit-stars-in-space-background-1611-large.mp4",
                coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=300&auto=format&fit=crop"
            ),
            MediaItemEntity(
                title = "Sea Waves Crashing",
                artist = "Nature Hub ", // trailing space
                normalizedArtist = "Nature Hub",
                album = "Ocean",
                duration = 22,
                mediaType = "VIDEO",
                url = "https://assets.mixkit.co/videos/preview/mixkit-sea-waves-crashing-on-rocks-4275-large.mp4",
                coverUrl = "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?w=300&auto=format&fit=crop"
            )
        )

        mediaDao.insertMediaItems(initialItems)

        // Seed initial playlists
        val playlistId = mediaDao.insertPlaylist(
            PlaylistEntity(name = "My Faves", description = "Your favorite tracks for a good mood")
        ).toInt()

        // Link first two items to the playlist
        mediaDao.insertPlaylistCrossRef(PlaylistMediaCrossRef(playlistId, 1))
        mediaDao.insertPlaylistCrossRef(PlaylistMediaCrossRef(playlistId, 3))
    }

    /**
     * Offline caching: Download and save file to local storage for offline playback
     */
    suspend fun downloadForOffline(itemId: Int, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val item = mediaDao.getMediaItemById(itemId) ?: return@withContext false
        if (item.isOffline) return@withContext true

        try {
            val destinationFile = File(context.filesDir, "media_cache_${itemId}.${if (item.mediaType == "AUDIO") "mp3" else "mp4"}")
            val url = URL(item.url)
            val connection = url.openConnection()
            connection.connect()

            val fileLength = connection.contentLength
            val input = url.openStream()
            val output = FileOutputStream(destinationFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress(total.toFloat() / fileLength.toFloat())
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            // Update item as offline with local path
            mediaDao.insertMediaItem(
                item.copy(
                    isOffline = true,
                    localPath = destinationFile.absolutePath
                )
            )
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Remove offline cache
     */
    suspend fun removeOfflineCache(itemId: Int) = withContext(Dispatchers.IO) {
        val item = mediaDao.getMediaItemById(itemId) ?: return@withContext
        if (!item.isOffline) return@withContext

        try {
            item.localPath?.let {
                val file = File(it)
                if (file.exists()) {
                    file.delete()
                }
            }
            mediaDao.insertMediaItem(item.copy(isOffline = false, localPath = null))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Use Gemini AI model (gemini-3.5-flash) to find duplicate artists and suggest metadata cleanup corrections.
     * Direct REST API from our gemini-api skill.
     */
    suspend fun getGeminiArtistCorrections(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Return local algorithm recommendations as a fallback
            return@withContext getLocalSuggestedCorrections()
        }

        // Get unique artist names in current library
        val items = mediaDao.getAllMediaItems().first()
        val uniqueArtists = items.map { it.artist }.distinct()
        if (uniqueArtists.size <= 1) return@withContext emptyList()

        val prompt = """
            You are an expert audio metadata cleaning engine. 
            Analyze this list of unique artist names from a user's library:
            ${uniqueArtists.joinToString(separator = ", ")}
            
            Identify names that are clearly referring to the same artist but have formatting, capitalization, trailing/leading spaces, spelling mistakes, or alias configuration errors (e.g. "Taylor swift", "Taylor Swift", "Taylor Swift ", "T. Swift" or "the weeknd", "The Weeknd" or "Nature Hub ", "Nature Hub").
            
            For each duplicate/error found, provide a mapping of the erroneous "original" artist name to the standard "corrected" canonical spelling of that artist's name.
            
            Return the output STRICTLY as a JSON array of objects, with no markdown formatting or other wrapper text. Each object must have "original" and "corrected" fields.
            Example:
            [
              {"original": "Taylor swift", "corrected": "Taylor Swift"},
              {"original": "T. Swift", "corrected": "Taylor Swift"}
            ]
            If no duplicate variations or errors are found, return an empty array [].
        """.trimIndent()

        try {
            val jsonText = GeminiApiClient.generateContent(apiKey, prompt)
            
            // Parse JSON manually or using Android JSONObject for zero dependency issue
            val corrections = mutableListOf<Pair<String, String>>()
            val jsonArray = JSONArray(jsonText)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val original = obj.getString("original")
                val corrected = obj.getString("corrected")
                corrections.add(original to corrected)
            }
            corrections
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to local suggests
            getLocalSuggestedCorrections()
        }
    }

    /**
     * Local heuristics to suggest corrections when offline or API key is missing
     */
    suspend fun getLocalSuggestedCorrections(): List<Pair<String, String>> {
        val items = mediaDao.getAllMediaItems().first()
        val uniqueArtists = items.map { it.artist }.distinct()
        val suggestions = mutableListOf<Pair<String, String>>()

        for (artist in uniqueArtists) {
            val trimmed = artist.trim()
            
            // Check if there is another artist that has the same spelling case-insensitively but differs
            val match = uniqueArtists.firstOrNull { 
                it != artist && it.trim().equals(trimmed, ignoreCase = true) 
            }

            if (match != null) {
                // Heuristic: Prefer the one with more capitalization, or if one ends with spaces, prefer the trimmed one.
                val artistScore = getArtistScore(artist)
                val matchScore = getArtistScore(match)
                
                if (artistScore < matchScore) {
                    suggestions.add(artist to match)
                }
            } else if (artist != trimmed) {
                // Simple trailing space correction
                suggestions.add(artist to trimmed)
            } else if (artist.contains(".") && artist.lowercase().replace(".", "").trim() == "t swift") {
                // Taylor Swift abbreviation
                val taylorSwift = uniqueArtists.firstOrNull { it.trim().equals("Taylor Swift", ignoreCase = true) }
                if (taylorSwift != null) {
                    suggestions.add(artist to taylorSwift)
                }
            }
        }
        return suggestions.distinctBy { it.first }
    }

    private fun getArtistScore(name: String): Int {
        var score = 0
        if (name.isNotEmpty() && name[0].isUpperCase()) score += 5
        // Count capitals
        score += name.count { it.isUpperCase() }
        // Deduct points for trailing space
        if (name.endsWith(" ") || name.startsWith(" ")) score -= 10
        return score
    }

    /**
     * Apply a list of metadata corrections. 
     * This saves them to the DB as rules, and updates all matching media items.
     */
    suspend fun applyArtistCorrections(corrections: List<Pair<String, String>>) = withContext(Dispatchers.IO) {
        for ((original, corrected) in corrections) {
            // Insert correction entity
            mediaDao.insertArtistCorrection(ArtistCorrectionEntity(original, corrected))
            
            // Update media items
            mediaDao.updateNormalizedArtist(original, corrected)
            
            // Update the artist field itself if they decide to fix metadata permanently
            val items = mediaDao.getAllMediaItems().first()
            for (item in items) {
                if (item.artist == original) {
                    mediaDao.insertMediaItem(item.copy(artist = corrected, normalizedArtist = corrected))
                } else if (item.normalizedArtist == original) {
                    mediaDao.insertMediaItem(item.copy(normalizedArtist = corrected))
                }
            }
        }
    }
}
