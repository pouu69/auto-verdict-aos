package com.car.autoverdict.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw Toss-style palette. Only design-system code (theme, components) reads this;
 * screens read the semantic [AvColors] tokens.
 */
object AvPalette {
    val Grey900 = Color(0xFF191F28)
    val Grey800 = Color(0xFF333D4B)
    val Grey700 = Color(0xFF4E5968)
    val Grey600 = Color(0xFF6B7684)
    val Grey500 = Color(0xFF8B95A1)
    val Grey400 = Color(0xFFB0B8C1)
    val Grey300 = Color(0xFFD1D6DB)
    val Grey200 = Color(0xFFE5E8EB)
    val Grey100 = Color(0xFFF2F4F6)
    val Grey50 = Color(0xFFF9FAFB)
    val White = Color(0xFFFFFFFF)

    val Blue500 = Color(0xFF3182F6)
    val Blue700 = Color(0xFF1B64DA)
    val Blue200 = Color(0xFF90C2FF)
    val Blue50 = Color(0xFFE8F3FF)

    val Red500 = Color(0xFFF04452)
    val Red700 = Color(0xFFD22030)
    val Red50 = Color(0xFFFFEEEE)

    val Orange500 = Color(0xFFFE9800)
    val Orange800 = Color(0xFF9A5000)
    val Orange50 = Color(0xFFFFF4E5)

    val Green500 = Color(0xFF03B26C)
    val Green700 = Color(0xFF03804D)
    val Green50 = Color(0xFFE5F8EF)
}

/** Semantic color tokens. See spec 4.2 for usage and the contrast policy. */
@Immutable
data class AvColors(
    val background: Color,
    val surfaceMuted: Color,
    val surfaceSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val border: Color,
    val accent: Color,
    val accentPressed: Color,
    val accentWeak: Color,
    val accentText: Color,
    val onAccent: Color,
    val dangerFill: Color,
    val dangerText: Color,
    val dangerBg: Color,
    val warningFill: Color,
    val warningText: Color,
    val warningBg: Color,
    val successFill: Color,
    val successText: Color,
    val successBg: Color,
    val neutralFill: Color,
    val neutralText: Color,
    val neutralBg: Color,
    val toastBg: Color,
    val toastAction: Color,
    val scrim: Color,
)

val LightAvColors = AvColors(
    background = AvPalette.White,
    surfaceMuted = AvPalette.Grey100,
    surfaceSubtle = AvPalette.Grey50,
    textPrimary = AvPalette.Grey900,
    textSecondary = AvPalette.Grey700,
    textTertiary = AvPalette.Grey600,
    textDisabled = AvPalette.Grey400,
    border = AvPalette.Grey200,
    accent = AvPalette.Blue500,
    accentPressed = AvPalette.Blue700,
    accentWeak = AvPalette.Blue50,
    accentText = AvPalette.Blue700,
    onAccent = AvPalette.White,
    dangerFill = AvPalette.Red500,
    dangerText = AvPalette.Red700,
    dangerBg = AvPalette.Red50,
    warningFill = AvPalette.Orange500,
    warningText = AvPalette.Orange800,
    warningBg = AvPalette.Orange50,
    successFill = AvPalette.Green500,
    successText = AvPalette.Green700,
    successBg = AvPalette.Green50,
    neutralFill = AvPalette.Grey400,
    neutralText = AvPalette.Grey700,
    neutralBg = AvPalette.Grey100,
    toastBg = AvPalette.Grey800,
    toastAction = AvPalette.Blue200,
    scrim = AvPalette.Grey900.copy(alpha = 0.5f),
)
