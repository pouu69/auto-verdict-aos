package com.car.autoverdict.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme

/**
 * Section title with an optional count and trailing text action. The action is shown
 * only when both [actionLabel] and [onAction] are given.
 *
 * With [onToggle] the whole row becomes a fold button (48dp+, chevron on the right) that
 * stays a heading for screen readers and announces [expanded]; pair it with [AvCollapsibleBody].
 * Without it the header looks and behaves as before.
 */
@Composable
fun AvSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val toggle = if (onToggle != null) {
        val state = stringResource(if (expanded) R.string.section_state_expanded else R.string.section_state_collapsed)
        val clickLabel = stringResource(
            if (expanded) R.string.section_fold_click_label else R.string.section_expand_click_label,
            title,
        )
        Modifier
            .avPressable(onClick = onToggle, onClickLabel = clickLabel)
            .semantics { stateDescription = state }
            .heightIn(min = 48.dp)
    } else {
        Modifier
    }
    // A folded section has no body under it, so the title gets the bottom space instead.
    val bottom by animateDpAsState(if (onToggle != null && !expanded) 16.dp else 4.dp, label = "headerBottom")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(toggle)
            .padding(start = 20.dp, end = if (onToggle != null) 16.dp else 8.dp, top = 20.dp, bottom = bottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (count != null) "$title $count" else title,
            style = typography.buttonM,
            color = colors.textSecondary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .avPressable(onClick = onAction)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = actionLabel, style = typography.caption1, color = colors.textTertiary)
            }
        }
        if (onToggle != null) {
            // Points down when folded, up when open.
            val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation),
            )
        }
    }
}

/** Body under a collapsible [AvSectionHeader]: slides open and shut from the top. */
@Composable
fun AvCollapsibleBody(expanded: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = expanded,
        modifier = modifier,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        content()
    }
}

/** 8dp grey band between sections (no shadows, no hairlines). */
@Composable
fun AvSectionDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(AvTheme.spacing.sectionDivider)
            .background(AvTheme.colors.surfaceMuted),
    )
}
