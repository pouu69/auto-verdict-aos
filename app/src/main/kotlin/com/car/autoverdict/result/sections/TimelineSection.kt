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
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.TimelineEntry
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.tone

/** Timelines this long fold down to the newest [TIMELINE_FOLDED_COUNT] entries (spec 5.2-8). */
internal const val TIMELINE_FOLD_THRESHOLD = 7
internal const val TIMELINE_FOLDED_COUNT = 5

/**
 * Newest first, as shown. The report's timeline is ascending, so a plain reversal also
 * flips same-month entries consistently. Folded long timelines keep only the newest five.
 */
internal fun visibleTimeline(entries: List<TimelineEntry>, expanded: Boolean): List<TimelineEntry> {
    val newestFirst = entries.asReversed()
    return if (expanded || !isFoldable(entries)) newestFirst else newestFirst.take(TIMELINE_FOLDED_COUNT)
}

internal fun isFoldable(entries: List<TimelineEntry>): Boolean = entries.size >= TIMELINE_FOLD_THRESHOLD

/**
 * Spec 5.2-8: history as a vertical timeline, newest first. Callers skip it when empty.
 * Seven or more entries fold to the newest five behind a "더 보기" button; the caller owns
 * [expanded] so it can reset it per report and keep the header in view after folding.
 */
@Composable
fun TimelineSection(
    entries: List<TimelineEntry>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = visibleTimeline(entries, expanded)
    Column(modifier = modifier.fillMaxWidth()) {
        AvSectionHeader(title = stringResource(R.string.result_section_timeline))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AvTheme.spacing.screenHorizontal)
                .padding(top = 8.dp, bottom = 12.dp),
        ) {
            // The rail ends at the first and last rows shown, folded or not.
            shown.forEachIndexed { index, entry ->
                TimelineRow(entry, isFirst = index == 0, isLast = index == shown.lastIndex)
            }
            if (isFoldable(entries)) {
                FoldButton(hiddenCount = entries.size - TIMELINE_FOLDED_COUNT, expanded = expanded, onClick = onToggleExpanded)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Full-width Secondary M button (48dp) under the last row; label and state follow [expanded]. */
@Composable
private fun FoldButton(hiddenCount: Int, expanded: Boolean, onClick: () -> Unit) {
    val text = if (expanded) {
        stringResource(R.string.result_timeline_fold)
    } else {
        stringResource(R.string.result_timeline_show_older, hiddenCount)
    }
    val state = stringResource(if (expanded) R.string.result_timeline_state_expanded else R.string.result_timeline_state_collapsed)
    val clickLabel = stringResource(
        if (expanded) R.string.result_timeline_fold_click_label else R.string.result_timeline_expand_click_label,
    )
    AvButton(
        text = text,
        onClick = onClick,
        style = AvButtonStyle.Secondary,
        size = AvButtonSize.Medium,
        modifier = Modifier
            .fillMaxWidth()
            // Outermost semantics win, so this relabels the button's own click action.
            .semantics {
                stateDescription = state
                onClick(label = clickLabel) { onClick(); true }
            },
    )
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
