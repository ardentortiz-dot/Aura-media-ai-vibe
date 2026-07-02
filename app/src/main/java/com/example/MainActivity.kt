package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.player.AuraPlayerManager
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.FullPlayerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MetadataScreen
import com.example.ui.screens.PlaylistScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuraMediaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AuraMediaViewModel = viewModel()
            
            MyApplicationTheme(
                darkTheme = viewModel.isDarkTheme,
                accent = viewModel.currentAccent
            ) {
                var currentTab by remember { mutableStateOf("library") }
                var isPlayerExpanded by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.fillMaxWidth().testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == "library",
                                onClick = { currentTab = "library" },
                                icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                                label = { Text("Library") },
                                modifier = Modifier.testTag("nav_library")
                            )
                            NavigationBarItem(
                                selected = currentTab == "playlists",
                                onClick = { currentTab = "playlists" },
                                icon = { Icon(Icons.Default.QueueMusic, contentDescription = "Playlists") },
                                label = { Text("Playlists") },
                                modifier = Modifier.testTag("nav_playlists")
                            )
                            NavigationBarItem(
                                selected = currentTab == "metadata",
                                onClick = { currentTab = "metadata" },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Cleanup") },
                                label = { Text("AI Clean") },
                                modifier = Modifier.testTag("nav_metadata")
                            )
                            NavigationBarItem(
                                selected = currentTab == "sync",
                                onClick = { currentTab = "sync" },
                                icon = { Icon(Icons.Default.CloudQueue, contentDescription = "Settings & Sync") },
                                label = { Text("Settings") },
                                modifier = Modifier.testTag("nav_sync")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Display Screen Content based on selection
                        when (currentTab) {
                            "library" -> LibraryScreen(
                                viewModel = viewModel,
                                onNavigateToPlaylists = { currentTab = "playlists" },
                                modifier = Modifier.fillMaxSize()
                            )
                            "playlists" -> PlaylistScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            "metadata" -> MetadataScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            "sync" -> CloudSyncScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Floating Mini Player (Slide-in when track is loaded)
                        val trackLoaded = AuraPlayerManager.currentTrack != null
                        AnimatedVisibility(
                            visible = trackLoaded,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it }),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            MiniPlayer(
                                viewModel = viewModel,
                                onExpand = { isPlayerExpanded = true }
                            )
                        }
                    }
                }

                // Expand Full Player Overlap Screen
                if (isPlayerExpanded) {
                    FullPlayerScreen(
                        viewModel = viewModel,
                        onDismiss = { isPlayerExpanded = false }
                    )
                }
            }
        }
    }
}
