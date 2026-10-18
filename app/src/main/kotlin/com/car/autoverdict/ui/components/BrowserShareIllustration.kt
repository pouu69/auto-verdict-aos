package com.car.autoverdict.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme

/** Mock address bar text; a URL, not copy, so it stays out of strings.xml. */
internal const val MOCK_LISTING_URL = "fem.encar.com/cars/detail/..."

/** Mock tab-switcher count; a glyph on the drawing, not copy. */
private const val MOCK_TAB_COUNT = "1"

/** Chrome's tab switcher is a small rounded square; the theme shapes are too round at 16dp. */
private val TabSwitcherShape = RoundedCornerShape(percent = 25)

/**
 * Share guide step 1: a mobile browser on an Encar listing, laid out like Chrome's toolbar
 * (home, address bar, new tab, tab switcher, overflow menu) with the overflow button
 * highlighted, above a skeleton of the page.
 *
 * Decorative: cleared from semantics, the step text says the same thing.
 */
@Composable
fun BrowserMenuButtonIllustration(modifier: Modifier = Modifier) {
    MockScreen(modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BrowserToolbar(highlightMenu = true)
            PageSkeleton(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
        }
    }
}

/**
 * Share guide step 2: Chrome's opened overflow menu (icon row, then the item list) over the
 * dimmed page, anchored top-right like the real one, with the share item highlighted.
 *
 * Decorative: cleared from semantics, the step text says the same thing.
 */
@Composable
fun BrowserShareMenuIllustration(modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val shapes = AvTheme.shapes
    MockScreen(modifier) {
        // The page under the menu, sized by the menu so it always fills the frame.
        Column(modifier = Modifier.matchParentSize()) {
            BrowserToolbar(highlightMenu = false)
            PageSkeleton(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp, bottom = 8.dp)
                .fillMaxWidth(MENU_WIDTH_FRACTION)
                .shadow(elevation = 4.dp, shape = shapes.tile)
                .background(colors.background),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceSubtle)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MenuIcon(Icons.Rounded.ArrowForward, tint = colors.textDisabled)
                MenuIcon(Icons.Outlined.StarOutline)
                MenuIcon(Icons.Outlined.FileDownload)
                MenuIcon(Icons.Outlined.Info)
                MenuIcon(Icons.Rounded.Refresh)
            }
            BrowserMenuRow(icon = Icons.Outlined.AddBox, label = stringResource(R.string.illus_menu_new_tab))
            BrowserMenuRow(icon = Icons.Outlined.History, label = stringResource(R.string.illus_menu_history))
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border),
            )
            BrowserMenuRow(
                icon = Icons.Rounded.Share,
                label = stringResource(R.string.illus_menu_share),
                highlighted = true,
            )
            BrowserMenuRow(icon = Icons.Outlined.FindInPage, label = stringResource(R.string.illus_menu_find))
        }
    }
}

/** Phone-screen frame shared by the share guide drawings; clears semantics (decorative). */
@Composable
internal fun MockScreen(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = AvTheme.colors
    val shapes = AvTheme.shapes
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {}
            .clip(shapes.tile)
            .border(1.dp, colors.border, shapes.tile)
            .background(colors.background),
        content = content,
    )
}

@Composable
private fun BrowserToolbar(highlightMenu: Boolean) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToolbarIcon(Icons.Rounded.Home)
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(AvTheme.shapes.chip)
                .background(colors.surfaceMuted)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = MOCK_LISTING_URL,
                style = typography.caption1,
                color = colors.textSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ToolbarIcon(Icons.Rounded.Add)
        Box(
            modifier = Modifier
                .size(17.dp)
                .border(1.5.dp, colors.textSecondary, TabSwitcherShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = MOCK_TAB_COUNT, style = typography.caption2, color = colors.textSecondary, maxLines = 1)
        }
        // The overflow button: a filled accent circle when it's the step's target.
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (highlightMenu) colors.accent else colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = if (highlightMenu) colors.onAccent else colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Grey blocks standing in for the listing page: photo, title, then a detail line. */
@Composable
private fun PageSkeleton(modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val shapes = AvTheme.shapes
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shapes.tile)
                .background(colors.surfaceMuted),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(10.dp)
                .clip(shapes.chip)
                .background(colors.surfaceMuted),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(8.dp)
                .clip(shapes.chip)
                .background(colors.surfaceMuted),
        )
    }
}

@Composable
private fun ToolbarIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = AvTheme.colors.textSecondary,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun MenuIcon(icon: ImageVector, tint: Color = AvTheme.colors.textSecondary) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
}

@Composable
private fun BrowserMenuRow(icon: ImageVector, label: String, highlighted: Boolean = false) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val content = if (highlighted) colors.accentText else colors.textSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) colors.accentWeak else colors.background)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = if (highlighted) typography.buttonS else typography.caption1,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Chrome's menu covers roughly the right two-thirds to three-quarters of the screen. */
private const val MENU_WIDTH_FRACTION = 0.78f
