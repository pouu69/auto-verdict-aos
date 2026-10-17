package com.car.autoverdict.result.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.RepairPanel
import com.car.autoverdict.report.Repairs
import com.car.autoverdict.report.SystemItem
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone

/**
 * Spec 5.2-6: replaced/repaired panels, then flagged systems, then how many systems are fine.
 * With nothing to list it says so in one line.
 */
@Composable
fun RepairsSection(repairs: Repairs, modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(modifier = modifier.fillMaxWidth()) {
        AvSectionHeader(title = stringResource(R.string.result_section_repairs))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AvTheme.spacing.screenHorizontal)
                .padding(bottom = 20.dp),
        ) {
            repairs.panels.forEach { PanelRow(it) }
            repairs.systems.flagged.forEach { SystemRow(it) }
            if (repairs.panels.isEmpty() && repairs.systems.flagged.isEmpty()) {
                Text(
                    text = stringResource(R.string.result_repairs_none),
                    style = typography.body2,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            if (repairs.systems.okCount > 0) {
                Text(
                    text = stringResource(R.string.result_repairs_ok_count, repairs.systems.okCount),
                    style = typography.caption1,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PanelRow(panel: RepairPanel) {
    RepairRow(label = panel.name) {
        if (panel.rank != null) AvBadge(text = panel.rank, tone = panel.tone.toAvTone())
        AvBadge(text = panel.status, tone = AvTone.Neutral)
    }
}

@Composable
private fun SystemRow(item: SystemItem) {
    RepairRow(label = stringResource(R.string.result_system_name, item.group, item.name)) {
        AvBadge(text = item.status, tone = item.tone.toAvTone())
    }
}

@Composable
private fun RepairRow(label: String, badges: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = AvTheme.typography.body2,
            color = AvTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { badges() }
    }
}
