package com.car.autoverdict.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme

/**
 * A simplified, code-drawn mock of the Android share sheet as a browser opens it: drag handle,
 * title, the shared link's preview card, then a row of app targets with the AutoVerdict tile
 * (the launcher icon: white car on an accent rounded square) highlighted. Used in the share
 * guide and onboarding without shipping a screenshot asset (density-adaptive, zero binary weight).
 *
 * Decorative: cleared from semantics, the surrounding step text says the same thing.
 */
@Composable
fun ShareSheetIllustration(modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val shapes = AvTheme.shapes
    MockScreen(modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(shapes.chip)
                    .background(colors.border),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.illus_share_sheet_title),
                style = typography.buttonS,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            // Preview card of the shared link.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.tile)
                    .background(colors.surfaceSubtle)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(shapes.buttonS)
                        .background(colors.surfaceMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(8.dp)
                            .clip(shapes.chip)
                            .background(colors.surfaceMuted),
                    )
                    Text(
                        text = MOCK_LISTING_URL,
                        style = typography.caption1,
                        color = colors.textTertiary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                DimmedAppTile(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = stringResource(R.string.illus_app_messenger),
                    modifier = Modifier.weight(1f),
                )
                AutoVerdictTile(modifier = Modifier.weight(AV_TILE_WEIGHT))
                DimmedAppTile(
                    icon = Icons.Outlined.Sms,
                    label = stringResource(R.string.illus_app_message),
                    modifier = Modifier.weight(1f),
                )
                DimmedAppTile(
                    icon = Icons.Outlined.MailOutline,
                    label = stringResource(R.string.illus_app_mail),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DimmedAppTile(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        // Same outer size as the highlighted tile's ring, so the labels line up.
        Box(modifier = Modifier.size(TILE_OUTER), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(TILE_ICON)
                    .clip(AvTheme.shapes.tile)
                    .background(colors.surfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        TileLabel(text = label, highlighted = false)
    }
}

@Composable
private fun AutoVerdictTile(modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val shapes = AvTheme.shapes
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(TILE_OUTER)
                .border(2.dp, colors.accent, shapes.buttonL)
                .padding(TILE_RING_GAP),
            contentAlignment = Alignment.Center,
        ) {
            // The real launcher icon: the adaptive foreground on the accent background.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shapes.tile)
                    .background(colors.accent),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.onAccent),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        TileLabel(text = stringResource(R.string.app_name), highlighted = true)
    }
}

@Composable
private fun TileLabel(text: String, highlighted: Boolean) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Text(
        text = text,
        style = if (highlighted) typography.caption2 else typography.caption1,
        color = if (highlighted) colors.accentText else colors.textTertiary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
    )
}

private val TILE_ICON = 44.dp
private val TILE_RING_GAP = 5.dp
private val TILE_OUTER = TILE_ICON + TILE_RING_GAP * 2
private const val AV_TILE_WEIGHT = 1.5f
