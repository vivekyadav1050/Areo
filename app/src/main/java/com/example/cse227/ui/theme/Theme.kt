package com.example.cse227.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = MedicalSecondaryContainer,
    onSecondary = MedicalOnSecondaryContainer,
    tertiary = MedicalTertiary,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalPrimary,
    onPrimary = MedicalOnPrimary,
    primaryContainer = MedicalPrimaryContainer,
    onPrimaryContainer = MedicalOnPrimaryContainer,
    secondary = MedicalSecondary,
    onSecondary = MedicalOnSecondary,
    secondaryContainer = MedicalSecondaryContainer,
    onSecondaryContainer = MedicalOnSecondaryContainer,
    tertiary = MedicalTertiary,
    onTertiary = MedicalOnTertiary,
    tertiaryContainer = MedicalTertiaryContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight
)

@Composable
fun Cse227Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
