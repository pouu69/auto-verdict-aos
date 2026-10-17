package com.car.autoverdict.result.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.ReportCounts
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone

/** Spec 5.2-4: one chip per rule outcome; zero counts are left out. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountChips(counts: ReportCounts, modifier: Modifier = Modifier) {
    val chips = listOf(
        Triple(R.string.result_count_danger, counts.danger, AvTone.Danger),
        Triple(R.string.result_count_hidden, counts.hidden, AvTone.Danger),
        Triple(R.string.result_count_warning, counts.warning, AvTone.Warning),
        Triple(R.string.result_count_pass, counts.pass, AvTone.Success),
        Triple(R.string.result_count_unknown, counts.unknown, AvTone.Neutral),
        Triple(R.string.result_count_excluded, counts.excluded, AvTone.Neutral),
    ).filter { it.second > 0 }
    if (chips.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = AvTheme.spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chips.forEach { (label, count, tone) -> AvBadge(text = stringResource(label, count), tone = tone) }
    }
}
