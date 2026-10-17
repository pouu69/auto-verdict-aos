package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.ui.theme.AvTheme

/**
 * Section title with an optional count and trailing text action. The action is shown
 * only when both [actionLabel] and [onAction] are given.
 */
@Composable
fun AvSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 4.dp),
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
