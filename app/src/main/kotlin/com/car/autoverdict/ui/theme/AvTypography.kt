package com.car.autoverdict.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.car.autoverdict.R

val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

/** Tossface emoji font (bundled unmodified; credit shown on the licenses screen). */
val Tossface = FontFamily(Font(R.font.tossface))

/** Type scale from spec 4.3. Never go below [caption2] (11sp). */
@Immutable
data class AvTypography(
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val buttonL: TextStyle,
    val buttonM: TextStyle,
    val buttonS: TextStyle,
    val caption1: TextStyle,
    val caption2: TextStyle,
)

private fun avStyle(size: Int, weight: FontWeight, lineHeight: Float, tracking: Float) = TextStyle(
    fontFamily = Pretendard,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = tracking.sp,
)

val DefaultAvTypography = AvTypography(
    title1 = avStyle(26, FontWeight.Bold, 1.35f, -0.6f),
    title2 = avStyle(22, FontWeight.Bold, 1.35f, -0.5f),
    title3 = avStyle(19, FontWeight.SemiBold, 1.4f, -0.4f),
    body1 = avStyle(17, FontWeight.Medium, 1.5f, -0.3f),
    body2 = avStyle(15, FontWeight.Normal, 1.5f, -0.2f),
    buttonL = avStyle(17, FontWeight.SemiBold, 1.3f, -0.3f),
    buttonM = avStyle(15, FontWeight.SemiBold, 1.3f, -0.2f),
    buttonS = avStyle(13, FontWeight.SemiBold, 1.3f, 0f),
    caption1 = avStyle(13, FontWeight.Normal, 1.45f, 0f),
    caption2 = avStyle(11, FontWeight.Medium, 1.3f, 0f),
)
