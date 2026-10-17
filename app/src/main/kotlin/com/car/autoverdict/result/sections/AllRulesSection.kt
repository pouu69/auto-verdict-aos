package com.car.autoverdict.result.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.RuleItem
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.theme.AvTheme

/**
 * Spec 5.2-9: every evaluated rule, grouped under a category label wherever the category
 * changes. The report already orders rules by category, so no re-sorting here.
 */
@Composable
fun AllRulesSection(rules: List<RuleItem>, modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(modifier = modifier.fillMaxWidth()) {
        AvSectionHeader(title = stringResource(R.string.result_section_rules), count = rules.size)
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
                RuleRow(rule)
            }
        }
    }
}

@Composable
private fun RuleRow(rule: RuleItem) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
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
            Text(text = rule.message, style = typography.body2, color = colors.textSecondary)
        }
    }
}
