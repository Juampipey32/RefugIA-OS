package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CrtColorScheme = darkColorScheme(
    primary = Amber,
    secondary = DarkAmber,
    tertiary = DimAmber,
    background = BlackBg,
    surface = BlackBg,
    onPrimary = BlackBg,
    onSecondary = BlackBg,
    onTertiary = Amber,
    onBackground = Amber,
    onSurface = Amber,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CrtColorScheme,
        typography = Typography,
        content = content
    )
}
