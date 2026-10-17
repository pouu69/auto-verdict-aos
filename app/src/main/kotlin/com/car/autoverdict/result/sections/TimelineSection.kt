package com.car.autoverdict.result.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.TimelineEntry
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.tone

/** Spec 5.2-8: history as a vertical timeline, oldest first. Callers skip it when empty. */
@Composable
fun TimelineSection(entries: List<TimelineEntry>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        AvSectionHeader(title = stringResource(R.string.result_section_timeline))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AvTheme.spacing.screenHorizontal)
                .padding(top = 8.dp, bottom = 12.dp),
        ) {
            entries.forEachIndexed { index, entry ->
                TimelineRow(entry, isFirst = index == 0, isLast = index == entries.lastIndex)
            }
        }
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry, isFirst: Boolean, isLast: Boolean) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    // IntrinsicSize.Min lets the rail stretch to the text column's height.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier
                .width(12.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RailSegment(visible = !isFirst, color = colors.border, modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.tone(entry.tone.toAvTone()).fill),
            )
            RailSegment(visible = !isLast, color = colors.border, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = 16.dp)) {
            Text(text = entry.date, style = typography.caption1, color = colors.textTertiary)
            Text(text = entry.label, style = typography.body2, color = colors.textPrimary)
            if (entry.detail != null) {
                Text(text = entry.detail, style = typography.caption1, color = colors.textSecondary)
            }
        }
    }
}

/** Keeps its slot either way so the dot stays aligned; only draws when [visible]. */
@Composable
private fun RailSegment(visible: Boolean, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .width(2.dp)
            .then(if (visible) Modifier.background(color) else Modifier),
    )
}
