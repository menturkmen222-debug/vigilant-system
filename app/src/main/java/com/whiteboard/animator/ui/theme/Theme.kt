package com.whiteboard.animator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================================
// COLOR DEFINITIONS
// ============================================================================

// Primary brand colors - Professional blue
private val PrimaryLight = Color(0xFF1565C0)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFD1E4FF)
private val OnPrimaryContainerLight = Color(0xFF001D36)

// Secondary - Orange accent
private val SecondaryLight = Color(0xFFFF6F00)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFFFDBC8)
private val OnSecondaryContainerLight = Color(0xFF2B1700)

// Tertiary - Teal for highlights
private val TertiaryLight = Color(0xFF00897B)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFF9CF5E8)
private val OnTertiaryContainerLight = Color(0xFF00201D)

// Neutrals
private val BackgroundLight = Color(0xFFFDFCFF)
private val OnBackgroundLight = Color(0xFF1A1C1E)
private val SurfaceLight = Color(0xFFFDFCFF)
private val OnSurfaceLight = Color(0xFF1A1C1E)
private val SurfaceVariantLight = Color(0xFFE0E2EC)
private val OnSurfaceVariantLight = Color(0xFF44474E)
private val OutlineLight = Color(0xFF74777F)

// Error
private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)

// Dark theme colors
private val PrimaryDark = Color(0xFFA0CAFF)
private val OnPrimaryDark = Color(0xFF003258)
private val PrimaryContainerDark = Color(0xFF00497D)
private val OnPrimaryContainerDark = Color(0xFFD1E4FF)

private val SecondaryDark = Color(0xFFFFB68A)
private val OnSecondaryDark = Color(0xFF4A2800)
private val SecondaryContainerDark = Color(0xFF6A3C00)
private val OnSecondaryContainerDark = Color(0xFFFFDBC8)

private val TertiaryDark = Color(0xFF80D8CC)
private val OnTertiaryDark = Color(0xFF003732)
private val TertiaryContainerDark = Color(0xFF005048)
private val OnTertiaryContainerDark = Color(0xFF9CF5E8)

private val BackgroundDark = Color(0xFF1A1C1E)
private val OnBackgroundDark = Color(0xFFE2E2E6)
private val SurfaceDark = Color(0xFF1A1C1E)
private val OnSurfaceDark = Color(0xFFE2E2E6)
private val SurfaceVariantDark = Color(0xFF44474E)
private val OnSurfaceVariantDark = Color(0xFFC4C6D0)
private val OutlineDark = Color(0xFF8E9099)

private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)

// ============================================================================
// COLOR SCHEMES
// ============================================================================

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

// ============================================================================
// THEME COMPOSABLE
// ============================================================================

/**
 * Main theme for Whiteboard Animator Pro.
 * 
 * Supports:
 * - Light/Dark mode
 * - Dynamic colors (Android 12+)
 * - Edge-to-edge display
 */
@Composable
fun WhiteboardAnimatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Set transparent status bar and navigation bar for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            // Set light/dark icons based on theme
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

// ============================================================================
// CUSTOM COLORS
// ============================================================================

/**
 * Extended colors for specific app features.
 */
object WhiteboardColors {
    // Mode indicator colors
    val OfflineMode = Color(0xFF43A047)
    val OnlineMode = Color(0xFF1E88E5)
    
    // Whiteboard backgrounds
    val Whiteboard = Color(0xFFFEFEFE)
    val Chalkboard = Color(0xFF2E4A3E)
    val Paper = Color(0xFFFFF8E1)
    
    // Drawing colors
    val DrawBlack = Color(0xFF212121)
    val DrawBlue = Color(0xFF1976D2)
    val DrawRed = Color(0xFFD32F2F)
    val DrawGreen = Color(0xFF388E3C)
    val DrawOrange = Color(0xFFF57C00)
    val DrawPurple = Color(0xFF7B1FA2)
    
    // Success/Info states
    val Success = Color(0xFF4CAF50)
    val Info = Color(0xFF2196F3)
    val Warning = Color(0xFFFF9800)
}
