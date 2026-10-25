package com.car.autoverdict.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvDialog
import com.car.autoverdict.ui.components.AvListRow
import com.car.autoverdict.ui.components.AvSectionDivider
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.components.AvTopBar
import com.car.autoverdict.ui.theme.AvTheme

/**
 * What the settings screen shows. A null count means the DB has not answered yet: the row
 * shows no value and can't be tapped, instead of flashing "없음" first.
 */
@Immutable
data class SettingsUiState(
    val recentCount: Int?,
    val savedCount: Int?,
    val versionName: String,
)

/** Which data a pending confirm dialog would clear. */
enum class ClearTarget { Recent, Saved }

/**
 * Stateless settings (spec 3). Only the open confirm dialog is kept here; [onClearRecent] and
 * [onClearSaved] run once the user confirms it. [onGallery] is null outside debug builds.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onClearRecent: () -> Unit,
    onClearSaved: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onLicenses: () -> Unit,
    onReplayGuide: () -> Unit,
    onContact: () -> Unit,
    onGallery: (() -> Unit)?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    // Saveable so an open dialog survives rotation (spec 3.2).
    var pendingClear by rememberSaveable { mutableStateOf<ClearTarget?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding(),
    ) {
        AvTopBar(onBack = onBack, title = stringResource(R.string.settings_title))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            item { AvSectionHeader(title = stringResource(R.string.settings_section_data)) }
            item {
                CountRow(
                    title = stringResource(R.string.settings_clear_recent),
                    count = state.recentCount,
                    onClick = { pendingClear = ClearTarget.Recent },
                )
            }
            item {
                CountRow(
                    title = stringResource(R.string.settings_clear_saved),
                    count = state.savedCount,
                    onClick = { pendingClear = ClearTarget.Saved },
                )
            }
            item { AvSectionDivider() }

            item { AvSectionHeader(title = stringResource(R.string.settings_section_app)) }
            item { NavRow(title = stringResource(R.string.settings_replay_guide), onClick = onReplayGuide) }
            item { NavRow(title = stringResource(R.string.settings_privacy), onClick = onPrivacyPolicy) }
            item { NavRow(title = stringResource(R.string.settings_licenses), onClick = onLicenses) }
            item {
                NavRow(
                    title = stringResource(R.string.settings_contact),
                    subtitle = stringResource(R.string.settings_contact_email),
                    onClick = onContact,
                )
            }
            item {
                AvListRow(
                    title = stringResource(R.string.settings_version),
                    trailing = { ValueText(state.versionName) },
                )
            }

            if (onGallery != null) {
                item { AvSectionDivider() }
                item { AvSectionHeader(title = stringResource(R.string.settings_section_dev)) }
                item { NavRow(title = stringResource(R.string.settings_gallery_title), onClick = onGallery) }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_disclaimer),
                    style = typography.caption1,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(
                        horizontal = AvTheme.spacing.screenHorizontal,
                        vertical = AvTheme.spacing.xxl,
                    ),
                )
            }
        }
    }

    when (pendingClear) {
        ClearTarget.Recent -> AvDialog(
            title = stringResource(R.string.settings_clear_recent_title),
            message = stringResource(R.string.settings_clear_recent_message, state.recentCount ?: 0),
            confirmLabel = stringResource(R.string.settings_clear_recent_confirm),
            onConfirm = {
                pendingClear = null
                onClearRecent()
            },
            dismissLabel = stringResource(R.string.settings_cancel),
            onDismiss = { pendingClear = null },
            destructive = true,
        )
        ClearTarget.Saved -> AvDialog(
            title = stringResource(R.string.settings_clear_saved_title, state.savedCount ?: 0),
            message = stringResource(R.string.settings_clear_saved_message),
            confirmLabel = stringResource(R.string.settings_clear_saved_confirm),
            onConfirm = {
                pendingClear = null
                onClearSaved()
            },
            dismissLabel = stringResource(R.string.settings_cancel),
            onDismiss = { pendingClear = null },
            destructive = true,
        )
        null -> Unit
    }
}

/** "{n}대 >" row that opens a clear dialog; with nothing to clear it reads "없음" and can't be tapped. */
@Composable
private fun CountRow(title: String, count: Int?, onClick: () -> Unit) {
    val clickable = count != null && count > 0
    AvListRow(
        title = title,
        onClick = if (clickable) onClick else null,
        trailing = when {
            count == null -> null
            count > 0 -> ({ ValueWithChevron(stringResource(R.string.settings_count_cars, count)) })
            else -> ({ ValueText(stringResource(R.string.settings_count_none)) })
        },
    )
}

@Composable
private fun NavRow(title: String, onClick: () -> Unit, subtitle: String? = null) {
    AvListRow(title = title, subtitle = subtitle, onClick = onClick, trailing = { Chevron() })
}

@Composable
private fun ValueWithChevron(value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AvTheme.spacing.xs),
    ) {
        ValueText(value)
        Chevron()
    }
}

@Composable
private fun ValueText(value: String) {
    Text(text = value, style = AvTheme.typography.body2, color = AvTheme.colors.textTertiary)
}

/** Decorative: the row itself is the button. */
@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = AvTheme.colors.textTertiary,
        modifier = Modifier.size(20.dp),
    )
}
