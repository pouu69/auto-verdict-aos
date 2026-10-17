package com.car.autoverdict.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.components.AvEmptyState
import com.car.autoverdict.ui.components.AvIconButton
import com.car.autoverdict.ui.components.AvIllustrationIcon
import com.car.autoverdict.ui.components.AvListRow
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.components.AvTextField
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import com.car.autoverdict.ui.theme.verdictLabelRes

/**
 * Home tab root (spec 3): link field with paste, the share-guide row and recent views.
 * Stateless apart from the "{n}대 더 보기" fold; [HomeRoute] owns input, error and platform calls.
 *
 * [recent] is null until the first query returns, and then nothing is drawn for the section
 * so the empty state doesn't flash on entry.
 */
@Composable
fun HomeScreen(
    input: String,
    onInputChange: (String) -> Unit,
    error: HomeError?,
    onPaste: () -> Unit,
    onSubmit: () -> Unit,
    recent: List<RecentViewItem>?,
    onOpenRecent: (RecentViewItem) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenShareGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val focusManager = LocalFocusManager.current
    val resources = LocalContext.current.resources
    val ageFormatter = remember(resources) { ResourcesRelativeAgeFormatter(resources) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding(),
    ) {
        AvTopBar(
            onBack = null,
            actions = {
                AvIconButton(
                    icon = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    onClick = onOpenSettings,
                )
            },
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "input") {
                Column(modifier = Modifier.padding(horizontal = AvTheme.spacing.screenHorizontal)) {
                    // No line breaks in the copy: it wraps naturally at large font scales.
                    Text(
                        text = stringResource(R.string.home_headline),
                        style = typography.title1,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .semantics { heading() },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.home_description),
                        style = typography.body2,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(24.dp))
                    AvTextField(
                        value = input,
                        onValueChange = onInputChange,
                        placeholder = stringResource(R.string.home_input_placeholder),
                        leadingIcon = Icons.Rounded.Link,
                        errorText = error?.let { stringResource(it.messageRes) },
                        onClear = { onInputChange("") },
                        actionLabel = stringResource(R.string.home_paste),
                        onAction = onPaste,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    )
                    Spacer(Modifier.height(12.dp))
                    // Enabled for any non-blank text; validation happens on tap (spec 3.2).
                    AvButton(
                        text = stringResource(R.string.home_analyze),
                        onClick = onSubmit,
                        enabled = input.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            item(key = "share") {
                AvListRow(
                    title = stringResource(R.string.home_share_row_title),
                    subtitle = stringResource(R.string.home_share_row_subtitle),
                    leading = { AvIllustrationIcon(icon = Icons.Rounded.IosShare, tone = AvTone.Accent, size = 40.dp) },
                    trailing = {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textTertiary)
                    },
                    onClick = {
                        // Close the keyboard before the sheet slides up over it.
                        focusManager.clearFocus()
                        onOpenShareGuide()
                    },
                )
            }
            if (recent != null) {
                recentSection(
                    items = recent,
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                    ageFormatter = ageFormatter,
                    onOpenRecent = onOpenRecent,
                )
            }
        }
    }
}

private fun LazyListScope.recentSection(
    items: List<RecentViewItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    ageFormatter: RelativeAgeFormatter,
    onOpenRecent: (RecentViewItem) -> Unit,
) {
    item(key = "recent_divider") { AvSectionDivider(Modifier.padding(top = 8.dp)) }
    item(key = "recent_header") {
        AvSectionHeader(
            title = stringResource(R.string.home_recent_title),
            count = items.size.takeIf { it > 0 },
        )
    }
    if (items.isEmpty()) {
        item(key = "recent_empty") {
            AvEmptyState(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.home_recent_empty_title),
                description = stringResource(R.string.home_recent_empty_description),
            )
        }
        return
    }
    val visible = recentVisible(items, expanded)
    items(visible.items, key = { it.carId }) { item ->
        RecentRow(item = item, ageFormatter = ageFormatter, onClick = { onOpenRecent(item) })
    }
    if (items.size > RECENT_COLLAPSED_COUNT) {
        item(key = "recent_more") {
            AvButton(
                text = if (expanded) {
                    stringResource(R.string.home_recent_fold)
                } else {
                    stringResource(R.string.home_recent_show_more, visible.hiddenCount)
                },
                onClick = onToggle,
                style = AvButtonStyle.Secondary,
                size = AvButtonSize.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AvTheme.spacing.screenHorizontal, vertical = 8.dp),
            )
        }
    }
    item(key = "recent_footnote") {
        Text(
            text = stringResource(R.string.home_recent_footnote),
            style = AvTheme.typography.caption1,
            color = AvTheme.colors.textTertiary,
            modifier = Modifier.padding(
                start = AvTheme.spacing.screenHorizontal,
                end = AvTheme.spacing.screenHorizontal,
                top = 8.dp,
                bottom = 24.dp,
            ),
        )
    }
}

/** Reads as one node: "title, 82점 · 3시간 전, 괜찮아요". Legacy rows drop the score and badge. */
@Composable
private fun RecentRow(item: RecentViewItem, ageFormatter: RelativeAgeFormatter, onClick: () -> Unit) {
    val age = ageFormatter.format(item.age)
    AvListRow(
        title = item.title ?: stringResource(R.string.home_recent_untitled, item.carId),
        subtitle = item.score?.let {
            stringResource(R.string.home_recent_meta, stringResource(R.string.result_score, it), age)
        } ?: age,
        trailing = item.verdict?.let { verdict ->
            { AvBadge(text = stringResource(verdictLabelRes(verdict)), tone = AvTone.fromVerdict(verdict)) }
        },
        onClick = onClick,
        onClickLabel = stringResource(R.string.home_recent_click_label),
    )
}
