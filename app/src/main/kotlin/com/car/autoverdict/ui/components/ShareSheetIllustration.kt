package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme

/** App-icon monogram on the mock tile; a logo, not copy. */
private const val MONOGRAM = "AV"

/**
 * A simplified, code-drawn mock of the Android share sheet that highlights the
 * AutoVerdict tile. Used in the share guide and onboarding to teach the
 * "Encar page, Share, AutoVerdict" flow without shipping a screenshot asset
 * (theme- and density-adaptive, zero binary weight).
 *
 * Decorative: cleared from semantics, the surrounding step text says the same thing.
 */
@Composable
fun ShareSheetIllustration(modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {}
            .clip(AvTheme.shapes.tile)
            .background(colors.surfaceSubtle)
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.illus_share_sheet_caption),
            style = AvTheme.typography.caption1,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            DimmedAppTile(icon = Icons.Outlined.ChatBubbleOutline, label = stringResource(R.string.illus_app_messenger))
            AutoVerdictTile()
            DimmedAppTile(icon = Icons.Outlined.Sms, label = stringResource(R.string.illus_app_message))
        }
    }
}

@Composable
private fun DimmedAppTile(icon: ImageVector, label: String) {
    val colors = AvTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(AvTheme.shapes.tile)
                .background(colors.surfaceMuted),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(text = label, style = AvTheme.typography.caption1, color = colors.textTertiary)
    }
}

@Composable
private fun AutoVerdictTile() {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(AvTheme.shapes.tile)
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = MONOGRAM, style = typography.title3, color = colors.onAccent)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = typography.caption2,
            color = colors.accentText,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.illus_tap_here),
            style = typography.caption2,
            color = colors.accentText,
            textAlign = TextAlign.Center,
        )
    }
}
