package com.car.autoverdict.result.sections

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.result.ResultBanner
import com.car.autoverdict.ui.components.avPressable
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import com.car.autoverdict.ui.theme.tone

/**
 * Spec 5.2-2: one notice above the verdict, with an inline action for update and partial data.
 * While [refreshing], the partial-data retry turns into a progress label that ignores taps.
 */
@Composable
fun ResultBannerCard(
    banner: ResultBanner,
    onApplyUpdate: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    refreshing: Boolean = false,
) {
    val (message, tone, action) = when (banner) {
        ResultBanner.SoldOut -> Triple(R.string.result_banner_sold_out, AvTone.Neutral, null)
        ResultBanner.UpdateAvailable -> Triple(
            R.string.result_banner_update,
            AvTone.Accent,
            BannerAction(R.string.result_banner_update_action, R.string.result_banner_update_click_label, onApplyUpdate),
        )
        ResultBanner.LoginRequired -> Triple(R.string.result_banner_login, AvTone.Accent, null)
        ResultBanner.PartialData -> Triple(
            R.string.result_banner_partial,
            AvTone.Warning,
            BannerAction(R.string.result_retry, R.string.result_banner_retry_click_label, onRetry),
        )
    }
    val toneColors = AvTheme.colors.tone(tone)
    val retrying = refreshing && banner == ResultBanner.PartialData
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AvTheme.spacing.screenHorizontal, vertical = 8.dp)
            .clip(AvTheme.shapes.tile)
            .background(toneColors.bg)
            .padding(start = 16.dp, end = if (action != null) 4.dp else 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(message),
            style = AvTheme.typography.body2,
            color = toneColors.text,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                // Banners can appear after the screen is already read (refresh done, listing sold).
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (retrying) {
            RetryingLabel(color = toneColors.text)
        } else if (action != null) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .avPressable(onClick = action.onClick, onClickLabel = stringResource(action.clickLabel))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(action.label), style = AvTheme.typography.buttonS, color = toneColors.text)
            }
        }
    }
}

/** Stands in for [다시 시도] while the retry runs; same footprint, no click action. */
@Composable
private fun RetryingLabel(color: Color) {
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .padding(horizontal = 12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = color, strokeWidth = 2.dp)
        Spacer(Modifier.width(6.dp))
        Text(text = stringResource(R.string.result_refreshing), style = AvTheme.typography.buttonS, color = color)
    }
}

/** [label] is the short visible text; [clickLabel] tells screen readers what the tap does. */
private data class BannerAction(
    @StringRes val label: Int,
    @StringRes val clickLabel: Int,
    val onClick: () -> Unit,
)
