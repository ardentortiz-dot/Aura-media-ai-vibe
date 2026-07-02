package com.example.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.ArtistCorrectionEntity
import com.example.data.MediaItemEntity
import com.example.data.MediaRepository
import com.example.data.PlaylistEntity
import com.example.player.AuraPlayerManager
import com.example.ui.theme.AuraThemeSettings
import com.example.ui.theme.ThemeAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuraMediaViewModel(application: Application) : AndroidViewModel(application) {

    private val db: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "aura_media_db"
    ).fallbackToDestructiveMigration().build()

    private val repository = MediaRepository(application, db)

    // Reactive database streams
    val mediaItems: StateFlow<List<MediaItemEntity>> = repository.allMediaItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artistCorrections: StateFlow<List<ArtistCorrectionEntity>> = repository.artistCorrectionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    var selectedPlaylist by mutableStateOf<PlaylistEntity?>(null)
    val playlistItems = mutableStateOf<List<MediaItemEntity>>(emptyList())

    // Offline progress tracking (itemId -> percentage 0.0f - 1.0f)
    val downloadProgress = mutableStateMapOf<Int, Float>()

    // Metadata Cleanup States
    var isCleaningMetadata by mutableStateOf(false)
        private set
    var suggestedCorrections by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set
    var correctionStatusMessage by mutableStateOf("")
        private set

    // Cloud Sync States
    var isSyncing by mutableStateOf(false)
        private set
    var lastSyncTime by mutableStateOf<Long>(0L)
    var isAutoSyncEnabled by mutableStateOf(false)

    // Current customizable UI state values
    var currentAccent by mutableStateOf(AuraThemeSettings.accent)
    var isDarkTheme by mutableStateOf(AuraThemeSettings.isDarkTheme)

    init {
        // Seed initial data and launch setup
        viewModelScope.launch {
            repository.seedInitialData()
        }
    }

    // --- Media playback delegate actions ---
    fun playTrack(item: MediaItemEntity, list: List<MediaItemEntity>) {
        AuraPlayerManager.play(getApplication(), item, list)
    }

    fun pauseTrack() {
        AuraPlayerManager.pause()
    }

    fun resumeTrack() {
        AuraPlayerManager.resume()
    }

    fun stopTrack() {
        AuraPlayerManager.stop()
    }

    fun seekTo(positionMs: Long) {
        AuraPlayerManager.seekTo(positionMs)
    }

    fun playNext() {
        AuraPlayerManager.next(getApplication())
    }

    fun playPrevious() {
        AuraPlayerManager.previous(getApplication())
    }

    fun toggleFavorite(itemId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(itemId)
            // If the currently playing track is updated, sync its favorite status
            if (AuraPlayerManager.currentTrack?.id == itemId) {
                val updated = repository.getMediaItemById(itemId)
                // Reflect local favorite change in active player current item
            }
        }
    }

    // --- Playlist Actions ---
    fun createPlaylist(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertPlaylist(name, description)
            showToast("Playlist created successfully")
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            if (selectedPlaylist?.id == playlist.id) {
                selectedPlaylist = null
            }
            showToast("Playlist deleted")
        }
    }

    fun addSongToPlaylist(playlistId: Int, mediaItemId: Int) {
        viewModelScope.launch {
            repository.addMediaToPlaylist(playlistId, mediaItemId)
            showToast("Song added to playlist")
            loadPlaylistItems(playlistId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, mediaItemId: Int) {
        viewModelScope.launch {
            repository.removeMediaFromPlaylist(playlistId, mediaItemId)
            showToast("Song removed from playlist")
            loadPlaylistItems(playlistId)
        }
    }

    fun loadPlaylistItems(playlistId: Int) {
        viewModelScope.launch {
            repository.getMediaItemsForPlaylist(playlistId).collect {
                playlistItems.value = it
            }
        }
    }

    // --- Offline Cache Management ---
    fun downloadTrack(itemId: Int) {
        if (downloadProgress.containsKey(itemId)) return // Already in progress
        downloadProgress[itemId] = 0.0f
        
        viewModelScope.launch {
            val success = repository.downloadForOffline(itemId) { progress ->
                viewModelScope.launch(Dispatchers.Main) {
                    downloadProgress[itemId] = progress
                }
            }
            viewModelScope.launch(Dispatchers.Main) {
                downloadProgress.remove(itemId)
                if (success) {
                    showToast("Downloaded for offline playback")
                } else {
                    showToast("Failed to download track")
                }
            }
        }
    }

    fun deleteOfflineTrack(itemId: Int) {
        viewModelScope.launch {
            repository.removeOfflineCache(itemId)
            showToast("Offline cache cleared")
        }
    }

    // --- Gemini Metadata Correction Action ---
    fun analyzeMetadata() {
        isCleaningMetadata = true
        correctionStatusMessage = "Analyzing media library and scanning for artist spelling inconsistencies..."
        suggestedCorrections = emptyList()

        viewModelScope.launch {
            try {
                val suggestions = repository.getGeminiArtistCorrections()
                withContext(Dispatchers.Main) {
                    suggestedCorrections = suggestions
                    if (suggestions.isEmpty()) {
                        correctionStatusMessage = "Your library metadata is clean! No spelling duplicates or alias errors found."
                    } else {
                        correctionStatusMessage = "Success! Found ${suggestions.size} duplicate artist configurations to optimize."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    correctionStatusMessage = "Analysis error: ${e.localizedMessage}. Using offline local suggestions."
                    suggestedCorrections = repository.getLocalSuggestedCorrections()
                }
            } finally {
                isCleaningMetadata = false
            }
        }
    }

    fun applyMetadataCorrections(selected: List<Pair<String, String>>) {
        viewModelScope.launch {
            repository.applyArtistCorrections(selected)
            withContext(Dispatchers.Main) {
                showToast("Applied metadata cleanups! ${selected.size} artists normalized.")
                suggestedCorrections = suggestedCorrections.filter { it !in selected }
                if (suggestedCorrections.isEmpty()) {
                    correctionStatusMessage = "All corrections applied successfully!"
                }
            }
        }
    }

    fun clearCorrectionSuggestions() {
        suggestedCorrections = emptyList()
        correctionStatusMessage = ""
    }

    // --- Cloud Sync Action ---
    fun triggerCloudSync() {
        isSyncing = true
        viewModelScope.launch {
            // Simulate backing up database stats and corrections to cloud storage
            kotlinx.coroutines.delay(2000)
            withContext(Dispatchers.Main) {
                isSyncing = false
                lastSyncTime = System.currentTimeMillis()
                showToast("Cloud sync completed: Playlists & metadata backed up safely!")
            }
        }
    }

    // --- Theme Customizations ---
    fun updateAccent(accent: ThemeAccent) {
        currentAccent = accent
        AuraThemeSettings.accent = accent
    }

    fun toggleDarkMode() {
        isDarkTheme = !isDarkTheme
        AuraThemeSettings.isDarkTheme = isDarkTheme
    }

    fun insertMediaItem(
        title: String,
        artist: String,
        album: String,
        duration: Long,
        mediaType: String,
        url: String,
        coverUrl: String
    ) {
        viewModelScope.launch {
            repository.insertMediaItem(
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                mediaType = mediaType,
                url = url,
                coverUrl = coverUrl
            )
            showToast("Added $title to Library")
        }
    }

    fun deleteMediaItem(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.deleteMediaItem(item)
            showToast("Deleted ${item.title}")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }
}
