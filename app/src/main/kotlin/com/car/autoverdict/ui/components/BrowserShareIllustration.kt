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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme

/** Mock address bar text; a URL, not copy, so it stays out of strings.xml. */
private const val MOCK_URL = "fem.encar.com/cars/detail/..."

/**
 * Code-drawn mock of a mobile browser's address bar + overflow menu, highlighting
 * the share item. Teaches users where the browser share entry point is — the step
 * most people get stuck on — before they reach the system share sheet shown by
 * [ShareSheetIllustration]. Browser-agnostic, with a note on where Chrome and
 * Samsung Internet keep the menu.
 *
 * Decorative: cleared from semantics, the guide's step text says the same thing.
 */
@Composable
fun BrowserShareIllustration(modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val shapes = AvTheme.shapes
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {}
            .clip(shapes.tile)
            .background(colors.surfaceSubtle)
            .padding(12.dp),
    ) {
        // Address bar with the overflow menu button highlighted.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.chip)
                .background(colors.background)
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = MOCK_URL,
                style = typography.caption1,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(shapes.chip)
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.illus_menu_caption),
            style = typography.caption2,
            color = colors.accentText,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
        )
        Spacer(Modifier.height(6.dp))

        // Overflow menu with the share row highlighted.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.tile)
                .background(colors.background),
        ) {
            BrowserMenuRow(icon = Icons.Outlined.StarOutline, label = stringResource(R.string.illus_menu_favorite))
            BrowserMenuRow(
                icon = Icons.Outlined.IosShare,
                label = stringResource(R.string.illus_menu_share),
                highlighted = true,
            )
            BrowserMenuRow(icon = Icons.Outlined.Print, label = stringResource(R.string.illus_menu_print))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.illus_browser_note),
            style = typography.caption1,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BrowserMenuRow(icon: ImageVector, label: String, highlighted: Boolean = false) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val content = if (highlighted) colors.accentText else colors.textTertiary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) colors.accentWeak else colors.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            style = typography.caption1,
            color = content,
            modifier = Modifier.weight(1f),
        )
        if (highlighted) {
            Text(text = stringResource(R.string.illus_tap_here), style = typography.caption2, color = content)
        }
    }
}
