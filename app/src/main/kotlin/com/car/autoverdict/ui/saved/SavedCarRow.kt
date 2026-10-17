package com.car.autoverdict.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.saved.SavedCarDisplay
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.avPressable
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.tone
import kotlinx.coroutines.delay

/**
 * One saved car (spec 3 4.1–4.2, 4.5–4.6): name · verdict, summary · score, count badges · saved
 * date. Normally a tap opens the result and an end-to-start swipe (or the "삭제" accessibility
 * action) calls [onDelete]; while [selecting], a tap toggles [selected] and deleting is off.
 *
 * The caller adds the 8dp outer margin so the focus ring (5dp outside the tile) is not clipped.
 */
@Composable
fun SavedCarRow(
    display: SavedCarDisplay,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AvTheme.colors
    val shape = AvTheme.shapes.tile
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) currentOnDelete()
            value != SwipeToDismissBoxValue.StartToEnd
        },
    )
    // A successful delete removes the row long before this fires; if it failed, slide back in.
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            delay(DISMISS_RESET_DELAY_MS)
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !selecting,
        backgroundContent = {
            // Only while swiping, so the red never shows at the settled row's rounded corners.
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(colors.dangerBg)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = colors.dangerText,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
    ) {
        RowBody(display = display, selecting = selecting, selected = selected, onClick = onClick, onDelete = onDelete)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowBody(
    display: SavedCarDisplay,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val shape = AvTheme.shapes.tile
    val toneColors = colors.tone(display.verdictTone)

    val name = display.name ?: stringResource(R.string.saved_no_name)
    val verdictLabel = stringResource(display.verdictLabel)
    val score = stringResource(R.string.saved_score, display.score)
    val badgeTexts = display.badges.map { stringResource(it.label, it.count) }
    val savedOn = stringResource(R.string.saved_row_saved_on, display.savedOn)
    // Spec 8: "이름, 판정 {라벨} {점수}점, {요약}, {개수 배지들}, {날짜} 저장".
    val description = listOf(
        name,
        "${stringResource(R.string.compare_row_verdict)} $verdictLabel $score",
        display.summary,
        badgeTexts.joinToString(", "),
        savedOn,
    ).filter { it.isNotEmpty() }.joinToString(", ")
    val openLabel = stringResource(R.string.compare_open_result)
    val deleteLabel = stringResource(R.string.saved_delete)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Before clip so the outside focus ring isn't clipped away.
            .avPressable(
                onClick = onClick,
                role = if (selecting) Role.Checkbox else Role.Button,
                onClickLabel = if (selecting) null else openLabel,
                focusShape = shape,
            )
            .semantics {
                contentDescription = description
                if (selecting) {
                    toggleableState = ToggleableState(selected)
                } else {
                    customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
                }
            }
            .clip(shape)
            .background(colors.background)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            SelectionCircle(selected = selected)
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = typography.body1,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                AvBadge(text = verdictLabel, tone = display.verdictTone)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = display.summary,
                    style = typography.body2,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = score,
                    style = typography.body2.copy(fontWeight = FontWeight.SemiBold),
                    color = toneColors.text,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    display.badges.forEachIndexed { index, badge ->
                        AvBadge(text = badgeTexts[index], tone = badge.tone)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(text = savedOn, style = typography.caption1, color = colors.textTertiary)
            }
        }
    }
}

@Composable
private fun SelectionCircle(selected: Boolean) {
    val colors = AvTheme.colors
    val base = Modifier
        .size(24.dp)
        .clip(CircleShape)
    Box(
        modifier = if (selected) {
            base.background(colors.accent)
        } else {
            base.border(1.5.dp, colors.border, CircleShape)
        },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private const val DISMISS_RESET_DELAY_MS = 1_000L
