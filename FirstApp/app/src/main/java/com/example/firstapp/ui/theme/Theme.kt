package com.example.firstapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// Mapăm paleta Velocity pe schema standard Material 3
private val VelocityDarkColorScheme = darkColorScheme(
    primary = PrimaryVoltBlue,
    onPrimary = OnPrimary,
    secondary = SecondaryEnginePurple,
    onSecondary = OnSecondary,
    background = SurfaceDark,
    surface = SurfaceContainer,
    onBackground = OnSurfaceText,
    onSurface = OnSurfaceText,
    error = BrakeRed,
    outline = OutlineSlate,
    surfaceVariant = SurfaceContainerLow,
    onSurfaceVariant = OnSurfaceVariantText
)

@Composable
fun FirstAppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = VelocityDarkColorScheme
    val view = LocalView.current

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}