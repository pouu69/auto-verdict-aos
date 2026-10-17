package com.car.autoverdict.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAvColors = staticCompositionLocalOf { LightAvColors }
private val LocalAvTypography = staticCompositionLocalOf { DefaultAvTypography }
private val LocalAvShapes = staticCompositionLocalOf { AvShapes() }
private val LocalAvSpacing = staticCompositionLocalOf { AvSpacing() }

/** Token accessors: `AvTheme.colors.textPrimary`, `AvTheme.typography.body1`, … */
object AvTheme {
    val colors: AvColors
        @Composable @ReadOnlyComposable get() = LocalAvColors.current
    val typography: AvTypography
        @Composable @ReadOnlyComposable get() = LocalAvTypography.current
    val shapes: AvShapes
        @Composable @ReadOnlyComposable get() = LocalAvShapes.current
    val spacing: AvSpacing
        @Composable @ReadOnlyComposable get() = LocalAvSpacing.current
}

/** App theme. Light only (spec 3). Also maps tokens onto M3 so stock components match. */
@Composable
fun AvTheme(content: @Composable () -> Unit) {
    val colors = LightAvColors
    val typography = DefaultAvTypography
    CompositionLocalProvider(
        LocalAvColors provides colors,
        LocalAvTypography provides typography,
        LocalAvShapes provides AvShapes(),
        LocalAvSpacing provides AvSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colors.toM3ColorScheme(),
            typography = typography.toM3Typography(),
            content = content,
        )
    }
}
