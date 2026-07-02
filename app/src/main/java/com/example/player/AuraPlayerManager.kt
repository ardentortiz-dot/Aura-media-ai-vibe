package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.MediaItemEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

object AuraPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    // Player State
    var currentTrack by mutableStateOf<MediaItemEntity?>(null)
    var isPlaying by mutableStateOf(false)
    var currentPlaybackList by mutableStateOf<List<MediaItemEntity>>(emptyList())
    var currentTrackIndex by mutableStateOf(-1)

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress: StateFlow<Long> = _playbackProgress

    var trackDuration by mutableStateOf(0L)

    init {
        // Automatically start checking progress when playing
    }

    fun play(context: Context, item: MediaItemEntity, list: List<MediaItemEntity> = emptyList()) {
        try {
            // Stop any current video/audio
            stop()

            currentTrack = item
            if (list.isNotEmpty()) {
                currentPlaybackList = list
                currentTrackIndex = list.indexOfFirst { it.id == item.id }
            } else {
                currentPlaybackList = listOf(item)
                currentTrackIndex = 0
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                // If the track is downloaded offline, play from local path, otherwise use remote URL
                val dataSource = if (item.isOffline && item.localPath != null && File(item.localPath).exists()) {
                    item.localPath
                } else {
                    item.url
                }
                
                setDataSource(dataSource)
                prepareAsync()
                
                setOnPreparedListener { mp ->
                    mp.start()
                    AuraPlayerManager.isPlaying = true
                    AuraPlayerManager.trackDuration = mp.duration.toLong()
                    startProgressUpdate()
                }

                setOnCompletionListener {
                    next(context)
                }

                setOnErrorListener { _, _, _ ->
                    AuraPlayerManager.isPlaying = false
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                stopProgressUpdate()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            isPlaying = true
            startProgressUpdate()
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
        stopProgressUpdate()
        _playbackProgress.value = 0
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _playbackProgress.value = positionMs
    }

    fun next(context: Context) {
        if (currentPlaybackList.isEmpty() || currentTrackIndex == -1) return
        var nextIndex = currentTrackIndex + 1
        if (nextIndex >= currentPlaybackList.size) {
            nextIndex = 0 // Wrap around
        }
        play(context, currentPlaybackList[nextIndex], currentPlaybackList)
    }

    fun previous(context: Context) {
        if (currentPlaybackList.isEmpty() || currentTrackIndex == -1) return
        var prevIndex = currentTrackIndex - 1
        if (prevIndex < 0) {
            prevIndex = currentPlaybackList.size - 1 // Wrap to end
        }
        play(context, currentPlaybackList[prevIndex], currentPlaybackList)
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _playbackProgress.value = it.currentPosition.toLong()
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }
}
