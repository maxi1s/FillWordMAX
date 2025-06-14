package com.example.fillwordsmax.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Light Theme Colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFFB5C2),
    onPrimary = Color(0xFF2C2C2C),
    primaryContainer = Color(0xFFFF9BA8),
    onPrimaryContainer = Color(0xFF2C2C2C),
    secondary = Color(0xFFB5E6D3),
    onSecondary = Color(0xFF2C2C2C),
    secondaryContainer = Color(0xFF9BD6C3),
    onSecondaryContainer = Color(0xFF2C2C2C),
    tertiary = Color(0xFFE6D3B5),
    onTertiary = Color(0xFF2C2C2C),
    tertiaryContainer = Color(0xFFD3B5E6),
    onTertiaryContainer = Color(0xFF2C2C2C),
    background = Color(0xFFFFF5F5),
    onBackground = Color(0xFF2C2C2C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2C2C2C),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF666666),
    error = Color(0xFFFFB5B5),
    onError = Color(0xFF2C2C2C)
)

// Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB5C2),
    onPrimary = Color(0xFFF5F5F5),
    primaryContainer = Color(0xFFFF9BA8),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFB5E6D3),
    onSecondary = Color(0xFFF5F5F5),
    secondaryContainer = Color(0xFF9BD6C3),
    onSecondaryContainer = Color(0xFFF5F5F5),
    tertiary = Color(0xFFE6D3B5),
    onTertiary = Color(0xFFF5F5F5),
    tertiaryContainer = Color(0xFFD3B5E6),
    onTertiaryContainer = Color(0xFFF5F5F5),
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF2C2C2C),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFCCCCCC),
    error = Color(0xFFFFB5B5),
    onError = Color(0xFFF5F5F5)
)

private val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun FillWordMaxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 