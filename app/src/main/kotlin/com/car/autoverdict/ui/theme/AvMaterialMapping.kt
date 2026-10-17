package com.car.autoverdict.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme

/** Fills every M3 slot (spec 4.6) so stock M3 components never show default purple. */
internal fun AvColors.toM3ColorScheme(): ColorScheme = lightColorScheme(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = accentWeak,
    onPrimaryContainer = accentText,
    inversePrimary = AvPalette.Blue200,
    secondary = AvPalette.Grey700,
    onSecondary = AvPalette.White,
    secondaryContainer = surfaceMuted,
    onSecondaryContainer = textPrimary,
    tertiary = AvPalette.Grey700,
    onTertiary = AvPalette.White,
    tertiaryContainer = surfaceMuted,
    onTertiaryContainer = textPrimary,
    background = background,
    onBackground = textPrimary,
    surface = background,
    onSurface = textPrimary,
    surfaceVariant = surfaceMuted,
    onSurfaceVariant = textSecondary,
    surfaceTint = background,
    inverseSurface = toastBg,
    inverseOnSurface = AvPalette.White,
    error = dangerText,
    onError = AvPalette.White,
    errorContainer = dangerBg,
    onErrorContainer = dangerText,
    outline = AvPalette.Grey300,
    outlineVariant = border,
    scrim = scrim,
    surfaceBright = background,
    surfaceDim = surfaceMuted,
    surfaceContainerLowest = background,
    surfaceContainerLow = background,
    surfaceContainer = surfaceSubtle,
    surfaceContainerHigh = surfaceMuted,
    surfaceContainerHighest = AvPalette.Grey200,
)

internal fun AvTypography.toM3Typography(): Typography = Typography(
    displayLarge = title1,
    displayMedium = title1,
    displaySmall = title1,
    headlineLarge = title1,
    headlineMedium = title1,
    headlineSmall = title2,
    titleLarge = title2,
    titleMedium = body1,
    titleSmall = buttonM,
    bodyLarge = body2,
    bodyMedium = body2,
    bodySmall = caption1,
    labelLarge = buttonL,
    labelMedium = caption1,
    labelSmall = caption2,
)
