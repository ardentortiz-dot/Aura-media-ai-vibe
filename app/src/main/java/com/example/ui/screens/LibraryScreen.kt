package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.data.MediaItemEntity
import com.example.player.AuraPlayerManager
import com.example.viewmodel.AuraMediaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: AuraMediaViewModel,
    onNavigateToPlaylists: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.mediaItems.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Music, 2: Videos, 3: Offline

    var showAddDialog by remember { mutableStateOf(false) }
    var showTrackMenu by remember { mutableStateOf<MediaItemEntity?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf<MediaItemEntity?>(null) }

    // Filter items
    val filteredItems = remember(items, searchQuery, selectedTab) {
        items.filter { item ->
            // Search criteria
            val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                    item.artist.contains(searchQuery, ignoreCase = true) ||
                    item.album.contains(searchQuery, ignoreCase = true)

            // Tab criteria
            val matchesTab = when (selectedTab) {
                0 -> true
                1 -> item.mediaType == "AUDIO"
                2 -> item.mediaType == "VIDEO"
                3 -> item.isOffline
                else -> true
            }

            matchesSearch && matchesTab
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = { Text("Aura Library", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_media_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Track")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_bar"),
                placeholder = { Text("Search songs, videos, artists...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            // Tab Rows (All, Music, Videos, Offline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Music Only", "Videos Only", "Offline Play").forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        label = { Text(label) },
                        modifier = Modifier.testTag("filter_chip_$index"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // List of items
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "No tracks",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No media files found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Add items or download tracks to build your library",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val isPlayingThis = AuraPlayerManager.currentTrack?.id == item.id && AuraPlayerManager.isPlaying
                        val progress = viewModel.downloadProgress[item.id]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .combinedClickable(
                                    onClick = { viewModel.playTrack(item, filteredItems) },
                                    onLongClick = { showTrackMenu = item }
                                )
                                .testTag("media_item_${item.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPlayingThis) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isPlayingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover thumbnail
                                Box(modifier = Modifier.size(56.dp)) {
                                    AsyncImage(
                                        model = item.coverUrl ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=150",
                                        contentDescription = "Cover preview",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Visual cue for media type
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(2.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.mediaType == "AUDIO") Icons.Default.MusicNote else Icons.Default.Videocam,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Track Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPlayingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.normalizedArtist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.album} • ${item.duration / 60}:${String.format("%02d", item.duration % 60)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                // Interactive Icons
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Download State icon
                                    when {
                                        item.isOffline -> {
                                            Icon(
                                                imageVector = Icons.Default.OfflinePin,
                                                contentDescription = "Offline Cache Ready",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        progress != null -> {
                                            CircularProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                        else -> {
                                            IconButton(
                                                onClick = { viewModel.downloadTrack(item.id) },
                                                modifier = Modifier.testTag("download_button_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Download offline",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }

                                    // Favorite heart Icon
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(item.id) },
                                        modifier = Modifier.testTag("favorite_button_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (item.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }

                                    // More options
                                    IconButton(
                                        onClick = { showTrackMenu = item },
                                        modifier = Modifier.testTag("more_button_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "More"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Add Track Dialog ---
    if (showAddDialog) {
        var addTitle by remember { mutableStateOf("") }
        var addArtist by remember { mutableStateOf("") }
        var addAlbum by remember { mutableStateOf("") }
        var addMediaType by remember { mutableStateOf("AUDIO") }
        var addUrl by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Media Track", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = addTitle,
                        onValueChange = { addTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_title_field")
                    )

                    OutlinedTextField(
                        value = addArtist,
                        onValueChange = { addArtist = it },
                        label = { Text("Artist") },
                        singleLine = true,
                        placeholder = { Text("e.g. Taylor Swift") },
                        modifier = Modifier.fillMaxWidth().testTag("add_artist_field")
                    )

                    OutlinedTextField(
                        value = addAlbum,
                        onValueChange = { addAlbum = it },
                        label = { Text("Album Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_album_field")
                    )

                    OutlinedTextField(
                        value = addUrl,
                        onValueChange = { addUrl = it },
                        label = { Text("Media URL or Stream Path") },
                        singleLine = true,
                        placeholder = { Text("https://example.com/song.mp3") },
                        modifier = Modifier.fillMaxWidth().testTag("add_url_field")
                    )

                    // Selection Segment for Media Type
                    Column {
                        Text("Media Format", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { addMediaType = "AUDIO" }
                            ) {
                                RadioButton(selected = addMediaType == "AUDIO", onClick = { addMediaType = "AUDIO" })
                                Text("Audio (MP3)")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { addMediaType = "VIDEO" }
                            ) {
                                RadioButton(selected = addMediaType == "VIDEO", onClick = { addMediaType = "VIDEO" })
                                Text("Video (MP4)")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (addTitle.isNotBlank() && addArtist.isNotBlank() && addUrl.isNotBlank()) {
                                    val cover = if (addMediaType == "AUDIO")
                                        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"
                                    else
                                        "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=300"

                                    viewModel.viewModelScope.launch {
                                        viewModel.insertMediaItem(
                                            title = addTitle,
                                            artist = addArtist,
                                            album = addAlbum.ifBlank { "Single" },
                                            duration = if (addMediaType == "AUDIO") 180 else 25,
                                            mediaType = addMediaType,
                                            url = addUrl,
                                            coverUrl = cover
                                        )
                                    }
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier.testTag("submit_media_button")
                        ) {
                            Text("Save Track")
                        }
                    }
                }
            }
        }
    }

    // --- Long Press Track Context Menu ---
    showTrackMenu?.let { item ->
        AlertDialog(
            onDismissRequest = { showTrackMenu = null },
            title = { Text(item.title) },
            text = { Text("Choose an action for this media track.") },
            confirmButton = {
                TextButton(onClick = {
                    showAddToPlaylistDialog = item
                    showTrackMenu = null
                }) {
                    Text("Add to Playlist")
                }
            },
            dismissButton = {
                Row {
                    if (item.isOffline) {
                        TextButton(onClick = {
                            viewModel.deleteOfflineTrack(item.id)
                            showTrackMenu = null
                        }) {
                            Text("Clear Cache", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = {
                        viewModel.deleteMediaItem(item)
                        showTrackMenu = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    // --- Add to Playlist Selection Dialog ---
    showAddToPlaylistDialog?.let { mediaItem ->
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = null },
            title = { Text("Add to Playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists created yet. Create a playlist from the Playlists tab.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                    ) {
                        items(playlists) { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, mediaItem.id)
                                        showAddToPlaylistDialog = null
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    playlist.name,
                                    modifier = Modifier.padding(12.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
