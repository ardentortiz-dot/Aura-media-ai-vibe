package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeAccent {
    PURPLE, ORANGE, GREEN, BLUE
}

object AuraThemeSettings {
    var accent by mutableStateOf(ThemeAccent.PURPLE)
    var isDarkTheme by mutableStateOf(true) // default to dark mode for a premium vibe
}
