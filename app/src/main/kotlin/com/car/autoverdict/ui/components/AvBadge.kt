package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import com.car.autoverdict.ui.theme.tone

/** Pill label in a semantic tone (e.g. verdict). */
@Composable
fun AvBadge(text: String, tone: AvTone, modifier: Modifier = Modifier) {
    val toneColors = AvTheme.colors.tone(tone)
    Text(
        text = text,
        style = AvTheme.typography.caption2,
        color = toneColors.text,
        modifier = modifier
            .clip(AvTheme.shapes.chip)
            .background(toneColors.bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Small fact tile (label over value). Non-neutral tones tint the whole tile. */
@Composable
fun AvFactTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tone: AvTone = AvTone.Neutral,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val toneColors = colors.tone(tone)
    val neutral = tone == AvTone.Neutral
    Column(
        modifier = modifier
            .clip(AvTheme.shapes.tile)
            .background(toneColors.bg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(text = label, style = typography.caption1, color = if (neutral) colors.textSecondary else toneColors.text)
        Text(
            text = value,
            style = typography.body1.copy(fontWeight = FontWeight.SemiBold),
            color = if (neutral) colors.textPrimary else toneColors.text,
        )
    }
}
