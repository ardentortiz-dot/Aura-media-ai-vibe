package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThemeAccent
import com.example.viewmodel.AuraMediaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: AuraMediaViewModel,
    modifier: Modifier = Modifier
) {
    val isSyncing = viewModel.isSyncing
    val lastSync = viewModel.lastSyncTime
    val isAutoSync = viewModel.isAutoSyncEnabled
    val currentAccent = viewModel.currentAccent
    val isDarkTheme = viewModel.isDarkTheme

    val syncDateStr = remember(lastSync) {
        if (lastSync == 0L) "Never Synced"
        else {
            val date = Date(lastSync)
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())
            formatter.format(date)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = { Text("Aura Settings & Sync", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Cloud Storage Synchronization Section ---
            item {
                Text(
                    "Cloud Backup & Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Google Cloud Platform Backup",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "Synchronize library, playlists, and deduplication rules",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sync Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Last Sync Action",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    syncDateStr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Button(
                                    onClick = { viewModel.triggerCloudSync() },
                                    modifier = Modifier.testTag("sync_now_button"),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Now")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto Sync toggler
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.isAutoSyncEnabled = !isAutoSync }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Automatic Cloud Synchronization", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Sync background edits automatically across devices", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isAutoSync,
                                onCheckedChange = { viewModel.isAutoSyncEnabled = it },
                                modifier = Modifier.testTag("auto_sync_toggle")
                            )
                        }
                    }
                }
            }

            // --- Customizable Theme & UI Styles Section ---
            item {
                Text(
                    "User Interface Customization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Light / Dark Preference Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dark Mode Comfort", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Saves battery and reduces eye strain in dark environments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick = { viewModel.toggleDarkMode() },
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        shape = CircleShape
                                    )
                                    .testTag("dark_mode_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = "Toggle Theme",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Accent Color Palette Selector
                        Text("Aura Accent Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Select a personalized accent color for your controls and headers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge 1: Cosmic Purple
                            AccentBadge(
                                color = Color(0xFF8A2BE2),
                                label = "Purple",
                                isSelected = currentAccent == ThemeAccent.PURPLE,
                                onClick = { viewModel.updateAccent(ThemeAccent.PURPLE) },
                                modifier = Modifier.testTag("accent_purple")
                            )

                            // Badge 2: Sunset Orange
                            AccentBadge(
                                color = Color(0xFFFF6F00),
                                label = "Orange",
                                isSelected = currentAccent == ThemeAccent.ORANGE,
                                onClick = { viewModel.updateAccent(ThemeAccent.ORANGE) },
                                modifier = Modifier.testTag("accent_orange")
                            )

                            // Badge 3: Emerald Green
                            AccentBadge(
                                color = Color(0xFF00897B),
                                label = "Green",
                                isSelected = currentAccent == ThemeAccent.GREEN,
                                onClick = { viewModel.updateAccent(ThemeAccent.GREEN) },
                                modifier = Modifier.testTag("accent_green")
                            )

                            // Badge 4: Ocean Blue
                            AccentBadge(
                                color = Color(0xFF1E88E5),
                                label = "Blue",
                                isSelected = currentAccent == ThemeAccent.BLUE,
                                onClick = { viewModel.updateAccent(ThemeAccent.BLUE) },
                                modifier = Modifier.testTag("accent_blue")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccentBadge(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
