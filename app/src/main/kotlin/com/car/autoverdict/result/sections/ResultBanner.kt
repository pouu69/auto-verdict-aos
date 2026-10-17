package com.car.autoverdict.result.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.result.ResultBanner
import com.car.autoverdict.ui.components.avPressable
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import com.car.autoverdict.ui.theme.tone

/** Spec 5.2-2: one notice above the verdict, with an inline action for update and partial data. */
@Composable
fun ResultBannerCard(
    banner: ResultBanner,
    onApplyUpdate: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (message, tone, action) = when (banner) {
        ResultBanner.SoldOut -> Triple(stringResource(R.string.result_banner_sold_out), AvTone.Neutral, null)
        ResultBanner.UpdateAvailable ->
            Triple(stringResource(R.string.result_banner_update), AvTone.Accent, stringResource(R.string.result_banner_update_action) to onApplyUpdate)
        ResultBanner.LoginRequired -> Triple(stringResource(R.string.result_banner_login), AvTone.Accent, null)
        ResultBanner.PartialData ->
            Triple(stringResource(R.string.result_banner_partial), AvTone.Warning, stringResource(R.string.result_retry) to onRetry)
    }
    val toneColors = AvTheme.colors.tone(tone)
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
            text = message,
            style = AvTheme.typography.body2,
            color = toneColors.text,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        )
        if (action != null) {
            val (label, onClick) = action
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .avPressable(onClick = onClick)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = label, style = AvTheme.typography.buttonS, color = toneColors.text)
            }
        }
    }
}
