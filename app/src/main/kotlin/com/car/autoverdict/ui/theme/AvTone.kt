package com.car.autoverdict.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.car.autoverdict.R

/** Semantic tone shared by badges, tiles and verdict displays. */
enum class AvTone {
    Accent, Danger, Warning, Success, Neutral;

    companion object {
        /** Maps the rule engine's verdict string (NEVER/CAUTION/OK/UNKNOWN). */
        fun fromVerdict(verdict: String?): AvTone = when (verdict) {
            "NEVER" -> Danger
            "CAUTION" -> Warning
            "OK" -> Success
            else -> Neutral
        }
    }
}

@Immutable
data class ToneColors(val fill: Color, val text: Color, val bg: Color)

fun AvColors.tone(tone: AvTone): ToneColors = when (tone) {
    AvTone.Accent -> ToneColors(accent, accentText, accentWeak)
    AvTone.Danger -> ToneColors(dangerFill, dangerText, dangerBg)
    AvTone.Warning -> ToneColors(warningFill, warningText, warningBg)
    AvTone.Success -> ToneColors(successFill, successText, successBg)
    AvTone.Neutral -> ToneColors(neutralFill, neutralText, neutralBg)
}

@StringRes
fun verdictLabelRes(verdict: String?): Int = when (AvTone.fromVerdict(verdict)) {
    AvTone.Danger -> R.string.verdict_never
    AvTone.Warning -> R.string.verdict_caution
    AvTone.Success -> R.string.verdict_ok
    AvTone.Accent, AvTone.Neutral -> R.string.verdict_unknown
}
