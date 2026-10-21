package com.aunt13psychot1c.chaoshub.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HubNight = Color(0xFF140C1F)
val HubPanel = Color(0xFF221636)
val HubPink = Color(0xFFFF4FA3)
val HubViolet = Color(0xFF9B5CFF)
val HubGlitter = Color(0xFFFFD86B)

@Composable
fun ChaosHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = HubPink,
            onPrimary = Color.White,
            secondary = HubViolet,
            tertiary = HubGlitter,
            background = HubNight,
            surface = HubPanel,
            surfaceContainerLow = HubPanel,
            surfaceContainer = HubPanel,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}
