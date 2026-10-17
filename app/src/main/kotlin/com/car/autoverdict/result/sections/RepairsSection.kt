package com.car.autoverdict.result.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.RepairPanel
import com.car.autoverdict.report.Repairs
import com.car.autoverdict.report.SystemItem
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.avPressable
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone

/**
 * Spec 5.2-6: replaced/repaired panels, then flagged systems, then how many other systems are
 * fine. With nothing to list it says so — together with the fine-system count — in one line.
 * When the report carries the whole inspection, a last row opens it via [onOpenInspection].
 */
@Composable
fun RepairsSection(
    repairs: Repairs,
    modifier: Modifier = Modifier,
    onOpenInspection: (() -> Unit)? = null,
    expanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    ResultSection(
        title = stringResource(R.string.result_section_repairs),
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AvTheme.spacing.screenHorizontal)
                .padding(bottom = 20.dp),
        ) {
            repairs.panels.forEach { PanelRow(it) }
            repairs.systems.flagged.forEach { SystemRow(it) }
            val okCount = repairs.systems.okCount
            if (repairs.panels.isEmpty() && repairs.systems.flagged.isEmpty()) {
                Text(
                    text = if (okCount > 0) {
                        stringResource(R.string.result_repairs_none_systems_ok, okCount)
                    } else {
                        stringResource(R.string.result_repairs_none)
                    },
                    style = typography.body2,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else if (okCount > 0) {
                Text(
                    text = stringResource(R.string.result_repairs_ok_count, okCount),
                    style = typography.caption1,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (repairs.sheet != null && onOpenInspection != null) {
                FullInspectionRow(onClick = onOpenInspection)
            }
        }
    }
}

@Composable
private fun FullInspectionRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .avPressable(onClick = onClick, onClickLabel = detailClickLabel())
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.result_repairs_full_inspection),
            style = AvTheme.typography.body2.copy(fontWeight = FontWeight.SemiBold),
            color = AvTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        DetailChevron()
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
