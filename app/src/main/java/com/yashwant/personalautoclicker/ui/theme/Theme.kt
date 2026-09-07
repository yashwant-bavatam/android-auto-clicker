package com.yashwant.personalautoclicker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AutoBlueLight,
    onPrimary = AutoBackground,

    primaryContainer = AutoBlueDark,
    onPrimaryContainer = AutoWhite,

    secondary = AutoBlue,
    onSecondary = AutoWhite,

    background = AutoBackground,
    onBackground = AutoWhite,

    surface = AutoSurface,
    onSurface = AutoWhite,

    surfaceVariant = AutoSurfaceVariant,
    onSurfaceVariant = AutoTextSecondary,

    error = AutoRed,
    onError = AutoWhite
)

private val LightColorScheme = lightColorScheme(
    primary = AutoBlue,
    onPrimary = AutoWhite,

    primaryContainer = AutoBlueLight,
    onPrimaryContainer = AutoLightText,

    secondary = AutoBlueDark,
    onSecondary = AutoWhite,

    background = AutoLightBackground,
    onBackground = AutoLightText,

    surface = AutoLightSurface,
    onSurface = AutoLightText,

    surfaceVariant = AutoLightSurfaceVariant,
    onSurfaceVariant = AutoLightText,

    error = AutoRed,
    onError = AutoWhite
)

@Composable
fun PersonalAutoClickerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}