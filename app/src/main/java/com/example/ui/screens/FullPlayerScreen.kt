package com.example.ui.screens

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.player.AuraPlayerManager
import com.example.viewmodel.AuraMediaViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    viewModel: AuraMediaViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = AuraPlayerManager.currentTrack ?: return
    val isPlaying = AuraPlayerManager.isPlaying
    val progress by AuraPlayerManager.playbackProgress.collectAsState()
    val duration = AuraPlayerManager.trackDuration

    // Format millisecond durations to mm:ss
    val formatTime: (Long) -> String = { ms ->
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("%d:%02d", mins, secs)
    }

    val sliderValue = remember(progress) { progress.toFloat() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (track.mediaType == "AUDIO") "Now Playing Audio" else "Now Streaming Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_player")) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", modifier = Modifier.size(32.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(track.id) }) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                if (track.mediaType == "VIDEO") {
                    // --- VIDEO PLAYER VIEW (Using real native Android VideoView) ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f)
                            .clip(RoundedCornerShape(16.dp))
                            .testTag("video_player_card"),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline
                        ),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val context = LocalContext.current
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        val mediaController = MediaController(ctx)
                                        mediaController.setAnchorView(this)
                                        setMediaController(mediaController)
                                        
                                        // Handle offline paths or remote urls
                                        val source = if (track.isOffline && track.localPath != null && File(track.localPath).exists()) {
                                            track.localPath
                                        } else {
                                            track.url
                                        }
                                        setVideoPath(source)
                                        
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                update = { videoView ->
                                    // Make sure it updates dynamically if the track changes
                                    val source = if (track.isOffline && track.localPath != null && File(track.localPath).exists()) {
                                        track.localPath
                                    } else {
                                        track.url
                                    }
                                    videoView.setVideoPath(source)
                                    videoView.start()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    // --- AUDIO PLAYER ART VIEW ---
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                        ) {
                            AsyncImage(
                                model = track.coverUrl ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400",
                                contentDescription = "Album Cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Title and artist text
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.normalizedArtist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Album: ${track.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                // Seeker and progress (Only shown for Audio - Video uses native media controllers)
                if (track.mediaType == "AUDIO") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Slider(
                            value = sliderValue,
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..(if (duration > 0) duration.toFloat() else 100000f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("audio_seekbar")
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(progress), style = MaterialTheme.typography.bodySmall)
                            Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Playback Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cache Status button
                    IconButton(
                        onClick = {
                            if (track.isOffline) {
                                viewModel.deleteOfflineTrack(track.id)
                            } else {
                                viewModel.downloadTrack(track.id)
                            }
                        },
                        modifier = Modifier.testTag("player_download_btn")
                    ) {
                        Icon(
                            imageVector = if (track.isOffline) Icons.Default.OfflinePin else Icons.Default.Download,
                            contentDescription = "Offline Cache Toggle",
                            tint = if (track.isOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Skip Previous
                    IconButton(onClick = { viewModel.playPrevious() }, modifier = Modifier.testTag("player_prev")) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                    }

                    // Play/Pause Floating Action Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                if (isPlaying) {
                                    viewModel.pauseTrack()
                                } else {
                                    if (AuraPlayerManager.currentTrack == null) {
                                        viewModel.playTrack(track, emptyList())
                                    } else {
                                        viewModel.resumeTrack()
                                    }
                                }
                            }
                            .testTag("player_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Skip Next
                    IconButton(onClick = { viewModel.playNext() }, modifier = Modifier.testTag("player_next")) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                    }

                    // Add to Playlist Button
                    IconButton(
                        onClick = { viewModel.selectedPlaylist = null }, // Action to go back or open details
                        modifier = Modifier.testTag("player_playlist_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Playlist status",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
