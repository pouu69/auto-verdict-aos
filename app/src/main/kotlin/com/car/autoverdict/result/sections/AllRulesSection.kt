package com.car.autoverdict.result.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.RuleItem
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.avPressable
import com.car.autoverdict.ui.theme.AvTheme

/**
 * Spec 5.2-9: every evaluated rule, grouped under a category label wherever the category
 * changes. The report already orders rules by category, so no re-sorting here. Rows show the
 * rule's short summary (its message in older reports); rows with a detail sheet open it
 * through [onOpenDetail] (spec 4.1).
 */
@Composable
fun AllRulesSection(
    rules: List<RuleItem>,
    modifier: Modifier = Modifier,
    onOpenDetail: ((RuleItem) -> Unit)? = null,
    expanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null,
) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    ResultSection(
        title = stringResource(R.string.result_section_rules),
        count = rules.size,
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
            rules.forEachIndexed { index, rule ->
                if (index == 0 || rules[index - 1].category != rule.category) {
                    Text(
                        text = rule.category,
                        style = typography.caption1,
                        color = colors.textTertiary,
                        modifier = Modifier
                            .padding(top = if (index == 0) 8.dp else 16.dp, bottom = 4.dp)
                            .semantics { heading() },
                    )
                }
                RuleRow(rule, onClick = onOpenDetail?.takeIf { rule.detail != null }?.let { open -> { open(rule) } })
            }
        }
    }
}

@Composable
private fun RuleRow(rule: RuleItem, onClick: (() -> Unit)?) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val interaction = if (onClick != null) {
        Modifier.avPressable(onClick = onClick, onClickLabel = detailClickLabel())
    } else {
        Modifier.semantics(mergeDescendants = true) {}
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(interaction)
            .heightIn(min = 48.dp)
            .padding(vertical = 8.dp),
    ) {
        // Fixed badge column so titles line up across "통과" / "미확인" widths.
        Box(modifier = Modifier.widthIn(min = 48.dp).padding(top = 2.dp)) {
            AvBadge(text = rule.label, tone = rule.tone.toAvTone())
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.title,
                style = typography.body2.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            // The short summary fits one line on most phones; long ones wrap rather than ellipsize.
            Text(text = rule.summary ?: rule.message, style = typography.body2, color = colors.textSecondary)
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            DetailChevron(modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}
