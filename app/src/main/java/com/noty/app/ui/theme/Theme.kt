package com.noty.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.noty.app.utils.ThemeManager

/*
 * Every role is listed, deliberately. The scheme this replaced set four — primary, secondary,
 * surface and background — and left the eleven others the UI actually reads falling through to
 * Material's baseline purple, so an indigo primary sat on purple containers for anyone without
 * dynamic colour. If a screen starts using a new role, it is already here.
 */
private val LightColors = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    inversePrimary = inversePrimaryLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    surfaceTint = surfaceTintLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    surfaceBright = surfaceBrightLight,
    surfaceDim = surfaceDimLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainerLowest = surfaceContainerLowestLight
)

private val DarkColors = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    inversePrimary = inversePrimaryDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    surfaceTint = surfaceTintDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    surfaceBright = surfaceBrightDark,
    surfaceDim = surfaceDimDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainerLowest = surfaceContainerLowestDark
)

/**
 * Noty is built out of Material 3 Expressive components, so it uses the Expressive theme
 * rather than plain MaterialTheme. The difference at runtime is [MotionScheme]: this wrapper
 * defaults motionScheme to expressive, which swaps Material's standard easing for the
 * spatial spring physics the Expressive spec describes, across every component that animates.
 *
 * Nothing else is passed. Shapes and typography default, and the colour scheme is the same
 * one plain MaterialTheme was given — this changes how things move, not how they look.
 *
 * Still marked @ExperimentalMaterial3ExpressiveApi in Material3 1.4.0. The opt-in is here and
 * nowhere else, so the day it stabilises or changes shape, this is the only call site.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotyTheme(
    themeMode: ThemeManager.ThemeMode = ThemeManager.ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    // The status and navigation bar icons are the one piece of chrome Compose does not draw,
    // so they have to be told which way to go. Previously this came free from
    // AppCompatDelegate.setDefaultNightMode() changing the resource configuration; now that
    // the theme is a plain parameter, the window would keep the *system's* light/dark icons
    // and a user on "Dark" under a light system would get dark icons on a dark bar.
    val view = LocalView.current
    // as? rather than as: there is no window to configure in a @Preview, and none either
    // if this theme is ever hosted by a ComposeView outside an Activity.
    val window = (view.context as? Activity)?.window
    if (window != null && !view.isInEditMode) {
        SideEffect {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        content = content
    )
}
